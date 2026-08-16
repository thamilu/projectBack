package com.eshop.app.user.application.service;

import com.eshop.app.user.domain.exception.KeycloakErrorType;
import com.eshop.app.user.domain.exception.KeycloakOperation;
import com.eshop.app.user.domain.exception.KeycloakOperationException;
import com.eshop.app.user.infrastructure.keycloak.KeycloakAdminAdapter;
import com.github.benmanes.caffeine.cache.Cache;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.MDC;

@org.springframework.stereotype.Component
@Slf4j
public class KeycloakAtomicOperations {

    private static final String NO_ROLE = "none";
    private static final String MDC_OPERATION_ID = "keycloak.operationId";
    private static final String MDC_OPERATION = "keycloak.operation";
    private static final String MDC_USER_ID = "keycloak.userId";

    private final MaskingUtil maskingUtil;
    private final MeterRegistry meterRegistry;
    private final KeycloakAdminAdapter keycloakAdapter;
    private final KeycloakServiceValidator validator;
    private final KeycloakExceptionTranslator exceptionTranslator;
    private final Cache<String, RoleRepresentation> keycloakRoleCache;

    private final AtomicLong operationCounter = new AtomicLong(0);

    public KeycloakAtomicOperations(
            MaskingUtil maskingUtil,
            MeterRegistry meterRegistry,
            KeycloakAdminAdapter keycloakAdapter,
            KeycloakServiceValidator validator,
            KeycloakExceptionTranslator exceptionTranslator,
            Cache<String, RoleRepresentation> keycloakRoleCache) {
        this.maskingUtil = Objects.requireNonNull(maskingUtil, "maskingUtil");
        this.meterRegistry = Objects.requireNonNull(meterRegistry, "meterRegistry");
        this.keycloakAdapter = Objects.requireNonNull(keycloakAdapter, "keycloakAdapter");
        this.validator = Objects.requireNonNull(validator, "validator");
        this.exceptionTranslator =
                Objects.requireNonNull(exceptionTranslator, "exceptionTranslator");
        this.keycloakRoleCache = Objects.requireNonNull(keycloakRoleCache, "keycloakRoleCache");
    }

    @CircuitBreaker(name = "keycloak", fallbackMethod = "assignRoleCircuitFallback")
    @Retry(name = "keycloak", fallbackMethod = "assignRoleRetryFallback")
    public void assignRole(String userId, String roleName) {
        validator.validateUserId(userId);
        validator.validateRoleName(roleName);
        executeVoidOperation(
                KeycloakOperation.ASSIGN_ROLE,
                roleName,
                userId,
                () -> doAssignRole(userId, roleName));
    }

    @CircuitBreaker(name = "keycloak", fallbackMethod = "assignRoleByEmailCircuitFallback")
    @Retry(name = "keycloak", fallbackMethod = "assignRoleByEmailRetryFallback")
    public void assignRoleByEmail(String email, String roleName) {
        validator.validateEmail(email);
        validator.validateRoleName(roleName);
        log.debug(
                "Resolving user by email [{}] for role [{}]",
                maskingUtil.maskEmail(email),
                roleName);
        executeVoidOperation(
                KeycloakOperation.ASSIGN_ROLE_BY_EMAIL,
                roleName,
                email,
                () -> {
                    UserRepresentation user = findUserByEmail(email);
                    return doAssignRole(user.getId(), roleName);
                });
    }

    @CircuitBreaker(name = "keycloak", fallbackMethod = "setUserEnabledCircuitFallback")
    @Retry(name = "keycloak", fallbackMethod = "setUserEnabledRetryFallback")
    public void setUserEnabled(String userId, boolean enabled) {
        validator.validateUserId(userId);
        executeVoidOperation(
                KeycloakOperation.SET_USER_ENABLED,
                NO_ROLE,
                userId,
                () -> doSetUserEnabled(userId, enabled));
    }

    @CircuitBreaker(name = "keycloak", fallbackMethod = "isUserEnabledCircuitFallback")
    @Retry(name = "keycloak", fallbackMethod = "isUserEnabledRetryFallback")
    public boolean isUserEnabled(String userId) {
        validator.validateUserId(userId);
        return executeQueryOperation(
                KeycloakOperation.IS_USER_ENABLED,
                NO_ROLE,
                userId,
                () -> {
                    UserRepresentation user = keycloakAdapter.getUserById(userId);
                    if (user == null) {
                        log.warn("User [{}] not found in Keycloak during status check", maskingUtil.maskUserId(userId));
                        return false;
                    }
                    boolean enabled = Boolean.TRUE.equals(user.isEnabled());
                    log.debug("User [{}] enabled=[{}]", maskingUtil.maskUserId(userId), enabled);
                    return enabled;
                });
    }

    @CircuitBreaker(name = "keycloak", fallbackMethod = "revokeRoleCircuitFallback")
    @Retry(name = "keycloak", fallbackMethod = "revokeRoleRetryFallback")
    public void revokeRole(String userId, String roleName) {
        validator.validateUserId(userId);
        validator.validateRoleName(roleName);
        executeVoidOperation(
                KeycloakOperation.REVOKE_ROLE,
                roleName,
                userId,
                () -> doRevokeRole(userId, roleName));
    }

    private boolean doRoleOperation(
            String userId,
            String roleName,
            boolean shouldHaveRole,
            KeycloakOperation operation,
            BiConsumer<String, List<RoleRepresentation>> action) {
        List<RoleRepresentation> currentRoles = keycloakAdapter.getUserRealmRoles(userId);
        boolean hasRole =
                currentRoles.stream().anyMatch(r -> roleName.equalsIgnoreCase(r.getName()));

        if (hasRole == shouldHaveRole) {
            log.info(
                    "Role [{}] already in desired state for user [{}] — skip",
                    roleName,
                    maskingUtil.maskUserId(userId));
            recordMetric(operation.getOperationName(), "skipped", roleName);
            return false;
        }

        RoleRepresentation role = getCachedRoleRepresentation(roleName);
        action.accept(userId, Collections.singletonList(role));
        log.info(
                "Role [{}] operation [{}] completed for user [{}]",
                roleName,
                operation.getOperationName(),
                maskingUtil.maskUserId(userId));
        return true;
    }

    private boolean doAssignRole(String userId, String roleName) {
        return doRoleOperation(
                userId,
                roleName,
                true,
                KeycloakOperation.ASSIGN_ROLE,
                keycloakAdapter::addRealmRolesToUser);
    }

    private boolean doRevokeRole(String userId, String roleName) {
        return doRoleOperation(
                userId,
                roleName,
                false,
                KeycloakOperation.REVOKE_ROLE,
                keycloakAdapter::removeRealmRolesFromUser);
    }

    private boolean doSetUserEnabled(String userId, boolean enabled) {
        UserRepresentation user = keycloakAdapter.getUserById(userId);
        if (user == null) {
            log.warn("User [{}] not found in Keycloak when attempting to update enabled status", maskingUtil.maskUserId(userId));
            throw new IllegalArgumentException("User not found in Keycloak: " + maskingUtil.maskUserId(userId));
        }
        if (Boolean.TRUE.equals(user.isEnabled()) == enabled) {
            log.info(
                    "User [{}] already has enabled=[{}] — idempotent skip",
                    maskingUtil.maskUserId(userId),
                    enabled);
            recordMetric(KeycloakOperation.SET_USER_ENABLED.getOperationName(), "skipped");
            return false;
        }
        user.setEnabled(enabled);
        keycloakAdapter.updateUser(userId, user);
        log.info("User [{}] enabled=[{}] updated", maskingUtil.maskUserId(userId), enabled);
        return true;
    }

    private RoleRepresentation getCachedRoleRepresentation(String roleName) {
        return keycloakRoleCache.get(
                roleName,
                key -> {
                    log.debug("Role cache miss — fetching [{}] from Keycloak", key);
                    return keycloakAdapter.getRealmRole(key);
                });
    }

    public Cache<String, RoleRepresentation> getRoleCache() {
        return keycloakRoleCache;
    }

    private UserRepresentation findUserByEmail(String email) {
        Optional<UserRepresentation> byEmail = searchByEmailExact(email);
        if (byEmail.isPresent()) {
            return byEmail.get();
        }

        Optional<UserRepresentation> byUsername = searchByUsernameExact(email);
        if (byUsername.isPresent()) {
            log.debug("User found by username instead of email: {}", maskingUtil.maskEmail(email));
            return byUsername.get();
        }

        throw new KeycloakOperationException(
                KeycloakErrorType.USER_NOT_FOUND,
                "User not found: " + maskingUtil.maskEmail(email),
                null);
    }

    private Optional<UserRepresentation> searchByEmailExact(String email) {
        List<UserRepresentation> results = keycloakAdapter.searchUsersByEmail(email, true);

        if (results.isEmpty()) {
            return Optional.empty();
        }

        List<UserRepresentation> exactMatches =
                results.stream().filter(u -> email.equalsIgnoreCase(u.getEmail())).toList();

        if (exactMatches.isEmpty()) {
            log.warn(
                    "Keycloak email search returned {} results but no exact match for: {}",
                    results.size(),
                    maskingUtil.maskEmail(email));
            return Optional.empty();
        }

        if (exactMatches.size() > 1) {
            throw new KeycloakOperationException(
                    KeycloakErrorType.INVALID_REQUEST,
                    String.format(
                            "Multiple users found with email: %s (data integrity issue)",
                            maskingUtil.maskEmail(email)),
                    null);
        }

        return Optional.of(exactMatches.get(0));
    }

    private Optional<UserRepresentation> searchByUsernameExact(String username) {
        List<UserRepresentation> results = keycloakAdapter.searchUsersByUsername(username, true);
        return results.stream().filter(u -> username.equalsIgnoreCase(u.getUsername())).findFirst();
    }

    private String generateOperationId() {
        return "KC-" + operationCounter.incrementAndGet();
    }

    private <R> R executeOperation(
            KeycloakOperation operation,
            String role,
            String userId,
            Callable<R> task,
            boolean recordSuccessMetric) {

        String operationId = generateOperationId();

        try (MDC.MDCCloseable ignored1 = MDC.putCloseable(MDC_OPERATION_ID, operationId);
                MDC.MDCCloseable ignored2 =
                        MDC.putCloseable(MDC_OPERATION, operation.getOperationName());
                MDC.MDCCloseable ignored3 =
                        MDC.putCloseable(MDC_USER_ID, maskingUtil.maskUserId(userId))) {

            Timer.Sample sample = Timer.start(meterRegistry);
            String timerStatus = "success";

            try {
                R result = task.call();
                if (recordSuccessMetric
                        || (result instanceof Boolean && Boolean.TRUE.equals(result))) {
                    recordMetric(operation.getOperationName(), "success", role);
                }
                return result;
            } catch (KeycloakOperationException e) {
                timerStatus = "failure";
                recordMetric(operation.getOperationName(), "failure", role);
                throw e;
            } catch (RuntimeException e) {
                timerStatus = "failure";
                recordMetric(operation.getOperationName(), "failure", role);
                throw exceptionTranslator.translate(
                        e, operation.getOperationName(), "role=" + role);
            } catch (Exception e) {
                timerStatus = "failure";
                recordMetric(operation.getOperationName(), "failure", role);
                throw new KeycloakOperationException(
                        KeycloakErrorType.SERVER_ERROR,
                        "Unexpected error in " + operation.getOperationName(),
                        e);
            } finally {
                sample.stop(
                        Timer.builder("keycloak.operation.duration")
                                .tag("operation", operation.getOperationName())
                                .tag("role", role != null ? role : NO_ROLE)
                                .tag("status", timerStatus)
                                .register(meterRegistry));
            }
        }
    }

    private void executeVoidOperation(
            KeycloakOperation operation, String role, String userId, Callable<Boolean> task) {
        executeOperation(operation, role, userId, task, false);
    }

    private <R> R executeQueryOperation(
            KeycloakOperation operation, String role, String userId, Callable<R> task) {
        return executeOperation(operation, role, userId, task, true);
    }

    private void recordMetric(String operation, String status) {
        recordMetric(operation, status, NO_ROLE);
    }

    private void recordMetric(String operation, String status, String role) {
        meterRegistry
                .counter(
                        "keycloak.operation",
                        "operation",
                        operation,
                        "status",
                        status,
                        "role",
                        role != null ? role : NO_ROLE)
                .increment();
    }

    private void handleRetryExhaustion(
            KeycloakOperation operation, String userId, String roleName, Exception e) {
        log.warn(
                "Retry exhausted — op=[{}] userId=[{}] role=[{}]: {}",
                operation.getOperationName(),
                userId != null ? maskingUtil.maskUserId(userId) : "N/A",
                roleName != null ? roleName : "none",
                e.getMessage());
        recordMetric(operation.getOperationName(), "retry_exhausted", roleName);
        throw new KeycloakOperationException(
                KeycloakErrorType.SERVER_ERROR,
                String.format(
                        "Keycloak unavailable after retries — operation: %s, role: %s",
                        operation.getOperationName(), roleName),
                e);
    }

    private void handleCircuitOpen(
            KeycloakOperation operation, String roleName, CallNotPermittedException e) {
        log.error("Circuit open — op=[{}] role=[{}]", operation.getOperationName(), roleName);
        recordMetric(operation.getOperationName(), "circuit_open", roleName);
        throw new KeycloakOperationException(
                KeycloakErrorType.NETWORK_ERROR,
                String.format(
                        "Keycloak circuit open — operation: %s, role: %s",
                        operation.getOperationName(), roleName),
                e);
    }

    void assignRoleRetryFallback(String userId, String roleName, Exception e) {
        handleRetryExhaustion(KeycloakOperation.ASSIGN_ROLE, userId, roleName, e);
    }

    void assignRoleCircuitFallback(String userId, String roleName, CallNotPermittedException e) {
        handleCircuitOpen(KeycloakOperation.ASSIGN_ROLE, roleName, e);
    }

    void assignRoleByEmailRetryFallback(String email, String roleName, Exception e) {
        handleRetryExhaustion(KeycloakOperation.ASSIGN_ROLE_BY_EMAIL, email, roleName, e);
    }

    void assignRoleByEmailCircuitFallback(
            String email, String roleName, CallNotPermittedException e) {
        handleCircuitOpen(KeycloakOperation.ASSIGN_ROLE_BY_EMAIL, roleName, e);
    }

    void setUserEnabledRetryFallback(String userId, boolean enabled, Exception e) {
        handleRetryExhaustion(KeycloakOperation.SET_USER_ENABLED, userId, null, e);
    }

    void setUserEnabledCircuitFallback(
            String userId, boolean enabled, CallNotPermittedException e) {
        handleCircuitOpen(KeycloakOperation.SET_USER_ENABLED, null, e);
    }

    boolean isUserEnabledRetryFallback(String userId, Exception e) {
        handleRetryExhaustion(KeycloakOperation.IS_USER_ENABLED, userId, null, e);
        return false;
    }

    boolean isUserEnabledCircuitFallback(String userId, CallNotPermittedException e) {
        handleCircuitOpen(KeycloakOperation.IS_USER_ENABLED, null, e);
        return false;
    }

    void revokeRoleRetryFallback(String userId, String roleName, Exception e) {
        handleRetryExhaustion(KeycloakOperation.REVOKE_ROLE, userId, roleName, e);
    }

    void revokeRoleCircuitFallback(String userId, String roleName, CallNotPermittedException e) {
        handleCircuitOpen(KeycloakOperation.REVOKE_ROLE, roleName, e);
    }
}
