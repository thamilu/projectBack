package com.eshop.app.user.application.service.impl;

import com.eshop.app.core.exception.base.BusinessException;
import com.eshop.app.core.port.StoragePort;
import com.eshop.app.inventory.application.service.UserAuditService;
import com.eshop.app.notification.application.service.EmailService;
import com.eshop.app.user.api.request.ExportRequest;
import com.eshop.app.user.api.response.ExportResult;
import com.eshop.app.user.domain.entity.User;
import com.eshop.app.user.domain.repository.UserRepository;
import com.eshop.app.user.shared.domain.enums.ExportFormat;
import com.eshop.app.user.shared.domain.enums.UserAction;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Asynchronous service responsible for exporting user data to CSV or Excel format,
 * uploading the result to storage, and notifying the requester via email.
 *
 * <p><b>Transaction note:</b> This method holds an open JDBC connection for the entire export
 * duration (streaming cursor). Ensure the connection pool (HikariCP) is sized to accommodate
 * concurrent exports without starving normal request traffic. See {@link UserRepository#streamAll()}
 * and {@link UserRepository#streamByRole} — those queries must use {@code JOIN FETCH} for
 * {@code UserProfile} to prevent N+1 queries.
 *
 * <p><b>Architecture note:</b> {@code UserAuditService} is imported from the
 * {@code com.eshop.app.inventory} bounded context. This is a cross-module dependency that should
 * be relocated to the {@code user} or {@code shared} module. Tracked as architecture debt — do not
 * introduce further cross-module imports.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncUserExportService {

    // -------------------------------------------------------------------------
    // Constants
    // -------------------------------------------------------------------------

    private static final int EXPORT_URL_VALIDITY_HOURS = 24;
    private static final Duration PRESIGNED_URL_DURATION = Duration.ofHours(EXPORT_URL_VALIDITY_HOURS);
    private static final String EXPORT_STORAGE_PREFIX = "exports/users/";
    private static final String CONTENT_TYPE_EXCEL =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String CONTENT_TYPE_CSV = "text/csv";
    private static final String EXCEL_SHEET_NAME = "Users";
    private static final String[] EXPORT_HEADERS = {
        "Id", "KeycloakId", "Email", "FirstName", "LastName", "Role"
    };
    private static final String CSV_HEADER_LINE =
            "Id,KeycloakId,Email,FirstName,LastName,Role";
    private static final int PROGRESS_LOG_INTERVAL = 1_000;
    private static final String MDC_EXPORT_ID_KEY = "exportId";

    // Formula-injection prefix characters (CSV injection / Excel injection)
    private static final char[] CSV_FORMULA_INJECTION_CHARS = {'=', '+', '-', '@', '\t', '\r'};

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private final UserRepository userRepository;
    private final StoragePort storagePort;
    private final EmailService emailService;
    private final UserAuditService auditService;

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Asynchronously exports users matching the given {@code request} criteria, uploads the result
     * to remote storage, and notifies {@code requestedBy} via email.
     *
     * @param request    export configuration (format, optional role filter); must not be null
     * @param requestedBy email address of the requesting user; must not be blank
     * @return a {@link CompletableFuture} completed with the {@link ExportResult} on success,
     *         or completed exceptionally with a {@link BusinessException} on failure
     */
    @Async("eshopVirtualThreadExecutor")
    @Transactional(readOnly = true)
    public CompletableFuture<ExportResult> exportUsersAsync(
            ExportRequest request, String requestedBy) {

        // Validate inputs at the async entry point before any resource allocation
        validateExportInputs(request, requestedBy);

        String exportId = UUID.randomUUID().toString();

        // MDC: capture existing context and restore after completion (virtual-thread-safe pattern)
        Map<String, String> previousMdcContext = MDC.getCopyOfContextMap();
        MDC.put(MDC_EXPORT_ID_KEY, exportId);

        try {
            log.info(
                    "Starting async export: exportId=[{}] format=[{}] role=[{}] requestedBy=[{}]",
                    exportId,
                    request.getFormat(),
                    request.getRole(),
                    maskEmail(requestedBy));

            Path tempFile = Files.createTempFile(
                    "user-export-" + exportId, "." + request.getFormat().getExtension());

            try {
                long recordCount = streamUsersToFile(tempFile, request);

                String storageKey = buildStorageKey(exportId, request.getFormat());
                String contentType = resolveContentType(request.getFormat());

                uploadToStorage(tempFile, storageKey, contentType);

                String downloadUrl = storagePort.generatePresignedUrl(
                        storageKey, PRESIGNED_URL_DURATION);

                ExportResult result = ExportResult.builder()
                        .exportId(exportId)
                        .downloadUrl(downloadUrl)
                        .recordCount(recordCount)
                        .expiresAt(LocalDateTime.now().plusHours(EXPORT_URL_VALIDITY_HOURS))
                        .build();

                sendSuccessNotification(requestedBy, exportId, recordCount);

                // Audit: targetUserId is null for bulk/export actions
                auditService.logUserAction(null, null, UserAction.EXPORT);

                log.info(
                        "Export completed: exportId=[{}] recordCount=[{}]",
                        exportId,
                        recordCount);

                return CompletableFuture.completedFuture(result);

            } finally {
                Files.deleteIfExists(tempFile);
            }

        } catch (BusinessException e) {
            // Already a clean application exception — re-throw without wrapping
            log.error("Export failed: exportId=[{}]", exportId, e);
            sendFailureNotification(requestedBy, exportId);
            throw e;

        } catch (Exception e) {
            log.error("Export failed: exportId=[{}]", exportId, e);
            sendFailureNotification(requestedBy, exportId);
            throw new BusinessException(
                    "Export processing failed. Please contact support with export ID: " + exportId,
                    "EXPORT_FAILED",
                    HttpStatus.INTERNAL_SERVER_ERROR);

        } finally {
            restoreMdcContext(previousMdcContext);
        }
    }

    // -------------------------------------------------------------------------
    // Private — Orchestration Helpers
    // -------------------------------------------------------------------------

    private void validateExportInputs(ExportRequest request, String requestedBy) {
        if (request == null) {
            throw new IllegalArgumentException("ExportRequest must not be null");
        }
        if (request.getFormat() == null) {
            throw new IllegalArgumentException("ExportRequest.format must not be null");
        }
        if (!StringUtils.hasText(requestedBy)) {
            throw new IllegalArgumentException("requestedBy must not be blank");
        }
    }

    private String buildStorageKey(String exportId, ExportFormat format) {
        return EXPORT_STORAGE_PREFIX + exportId + "." + format.getExtension();
    }

    private String resolveContentType(ExportFormat format) {
        return format == ExportFormat.EXCEL ? CONTENT_TYPE_EXCEL : CONTENT_TYPE_CSV;
    }

    private void uploadToStorage(Path file, String key, String contentType) throws IOException {
        try (InputStream is = Files.newInputStream(file)) {
            storagePort.upload(key, is, contentType, Files.size(file));
        }
    }

    /**
     * Sends a success notification email to the requester.
     * If the email send fails, the exception is logged but NOT re-thrown — the export
     * itself was successful and the result must not be failed due to a notification error.
     */
    private void sendSuccessNotification(String requestedBy, String exportId, long recordCount) {
        try {
            String subject = "Your user export is ready";
            String body = String.format(
                    "Your user export request (ID: %s) completed successfully.%n%n"
                            + "Records exported: %d%n"
                            + "Please log in to the portal to download your export.%n%n"
                            + "The download link will expire in %d hours.",
                    exportId, recordCount, EXPORT_URL_VALIDITY_HOURS);
            emailService.sendEmail(requestedBy, subject, body);
        } catch (Exception emailEx) {
            log.warn(
                    "Export succeeded but notification email could not be sent: "
                            + "exportId=[{}] reason=[{}]",
                    exportId,
                    emailEx.getMessage());
        }
    }

    /**
     * Sends a failure notification email to the requester.
     * If the email send fails, the exception is logged and suppressed — we must not
     * lose the original export failure cause.
     */
    private void sendFailureNotification(String requestedBy, String exportId) {
        try {
            String subject = "User export could not be completed";
            String body = String.format(
                    "Your user export request (ID: %s) could not be completed.%n%n"
                            + "Please contact support quoting your export ID.",
                    exportId);
            emailService.sendEmail(requestedBy, subject, body);
        } catch (Exception emailEx) {
            log.warn(
                    "Export failure notification could not be sent: exportId=[{}] reason=[{}]",
                    exportId,
                    emailEx.getMessage());
        }
    }

    private void restoreMdcContext(Map<String, String> previousContext) {
        if (previousContext == null) {
            MDC.clear();
        } else {
            MDC.setContextMap(previousContext);
        }
    }

    // -------------------------------------------------------------------------
    // Private — File Writing
    // -------------------------------------------------------------------------

    private long streamUsersToFile(Path tempFile, ExportRequest request) throws IOException {
        return request.getFormat() == ExportFormat.EXCEL
                ? writeExcel(tempFile, request)
                : writeCsv(tempFile, request);
    }

    private long writeExcel(Path tempFile, ExportRequest request) throws IOException {
        final AtomicLong count = new AtomicLong(0);

        try (Workbook workbook = new XSSFWorkbook();
                FileOutputStream fos = new FileOutputStream(tempFile.toFile())) {

            Sheet sheet = workbook.createSheet(EXCEL_SHEET_NAME);
            writeExcelHeaderRow(sheet);

            try (Stream<User> userStream = getUserStream(request)) {
                AtomicInteger rowNum = new AtomicInteger(1);
                userStream.forEach(user -> {
                    String[] fields = mapUserToFields(user);
                    Row row = sheet.createRow(rowNum.getAndIncrement());
                    for (int col = 0; col < fields.length; col++) {
                        row.createCell(col).setCellValue(fields[col]);
                    }
                    logProgress(count.incrementAndGet());
                });
            }

            workbook.write(fos);
        }

        return count.get();
    }

    private void writeExcelHeaderRow(Sheet sheet) {
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < EXPORT_HEADERS.length; i++) {
            headerRow.createCell(i).setCellValue(EXPORT_HEADERS[i]);
        }
    }

    private long writeCsv(Path tempFile, ExportRequest request) throws IOException {
        final AtomicLong count = new AtomicLong(0);

        try (PrintWriter pw = new PrintWriter(new FileWriter(tempFile.toFile()))) {
            pw.println(CSV_HEADER_LINE);

            try (Stream<User> userStream = getUserStream(request)) {
                userStream.forEach(user -> {
                    String[] fields = mapUserToFields(user);
                    String line = String.join(",",
                            escapeCsv(fields[0]),
                            escapeCsv(fields[1]),
                            escapeCsv(fields[2]),
                            escapeCsv(fields[3]),
                            escapeCsv(fields[4]),
                            escapeCsv(fields[5]));
                    pw.println(line);
                    logProgress(count.incrementAndGet());
                });
            }

            // PrintWriter suppresses IOExceptions internally — must explicitly check for errors
            if (pw.checkError()) {
                throw new IOException(
                        "CSV write failed: PrintWriter encountered an error during streaming. "
                                + "The output file may be incomplete.");
            }
        }

        return count.get();
    }

    // -------------------------------------------------------------------------
    // Private — Data Mapping
    // -------------------------------------------------------------------------

    /**
     * Maps a {@link User} entity to an ordered array of String field values matching
     * {@link #EXPORT_HEADERS}. This single method is the source of truth for field mapping
     * across all export formats.
     *
     * @param user the user entity to map; must not be null
     * @return String array: [id, keycloakId, email, firstName, lastName, role]
     */
    private String[] mapUserToFields(User user) {
        String firstName = "";
        String lastName = "";
        if (user.getUserProfile() != null) {
            firstName = nullToEmpty(user.getUserProfile().getFirstName());
            lastName = nullToEmpty(user.getUserProfile().getLastName());
        }

        return new String[] {
            user.getId() != null ? user.getId().toString() : "",
            nullToEmpty(user.getKeycloakId()),
            nullToEmpty(user.getEmail()),
            firstName,
            lastName,
            user.getRole() != null ? user.getRole().name() : ""
        };
    }

    private Stream<User> getUserStream(ExportRequest request) {
        return request.getRole() != null
                ? userRepository.streamByRole(request.getRole())
                : userRepository.streamAll();
    }

    // -------------------------------------------------------------------------
    // Private — Utilities
    // -------------------------------------------------------------------------

    /**
     * Escapes a field value for safe inclusion in a CSV file.
     *
     * <p>Handles:
     * <ul>
     *   <li>RFC 4180 quoting for values containing {@code ,}, {@code "}, {@code \n}, {@code \r}</li>
     *   <li>CSV/Excel formula injection: values starting with {@code =}, {@code +}, {@code -},
     *       {@code @}, {@code \t}, {@code \r} are prefixed with a single quote ({@code '}) to
     *       prevent formula execution when the file is opened in spreadsheet software</li>
     * </ul>
     *
     * @param value the raw field value; null is treated as empty string
     * @return the escaped, safe CSV value
     */
    private String escapeCsv(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }

        // Sanitize formula injection: prefix with single quote to prevent execution
        String sanitized = sanitizeFormulaInjection(value);

        // RFC 4180 quoting
        if (sanitized.contains(",")
                || sanitized.contains("\"")
                || sanitized.contains("\n")
                || sanitized.contains("\r")) {
            return "\"" + sanitized.replace("\"", "\"\"") + "\"";
        }

        return sanitized;
    }

    /**
     * Defends against CSV/Excel formula injection by prefixing dangerous leading characters
     * with a single quote, which causes spreadsheet applications to treat the cell as text.
     */
    private String sanitizeFormulaInjection(String value) {
        if (value.isEmpty()) {
            return value;
        }
        char firstChar = value.charAt(0);
        for (char dangerous : CSV_FORMULA_INJECTION_CHARS) {
            if (firstChar == dangerous) {
                return "'" + value;
            }
        }
        return value;
    }

    private void logProgress(long currentCount) {
        if (currentCount % PROGRESS_LOG_INTERVAL == 0) {
            log.debug("Export progress: [{}] records written", currentCount);
        }
    }

    private static String nullToEmpty(String value) {
        return value != null ? value : "";
    }

    /**
     * Masks an email address for safe structured logging.
     * Retains only the domain portion to avoid writing PII into log aggregators.
     * Example: {@code user@example.com} → {@code ***@example.com}
     */
    private static String maskEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return "[blank]";
        }
        int atIndex = email.indexOf('@');
        return atIndex > 0 ? "***" + email.substring(atIndex) : "***";
    }
}

