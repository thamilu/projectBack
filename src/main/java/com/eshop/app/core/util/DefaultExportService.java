package com.eshop.app.core.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class DefaultExportService implements ExportService {

    @Override
    public byte[] exportToExcel(String sheetName, String[] headers, List<Map<String, Object>> data) {
        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            
            Sheet sheet = wb.createSheet(sheetName);
            Row headerRow = sheet.createRow(0);
            
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }
            
            int rowIdx = 1;
            for (Map<String, Object> record : data) {
                Row row = sheet.createRow(rowIdx++);
                for (int i = 0; i < headers.length; i++) {
                    Object val = record.get(headers[i]);
                    row.createCell(i).setCellValue(val != null ? val.toString() : "");
                }
            }
            
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            log.error("Excel generation failed: {}", e.getMessage(), e);
            throw new RuntimeException("Excel generation failed", e);
        }
    }

    @Override
    public byte[] exportToCsv(String[] headers, List<Map<String, Object>> data) {
        StringBuilder sb = new StringBuilder();
        
        // Header
        for (int i = 0; i < headers.length; i++) {
            sb.append(headers[i]);
            if (i < headers.length - 1) sb.append(",");
        }
        sb.append("\n");
        
        // Data
        for (Map<String, Object> record : data) {
            for (int i = 0; i < headers.length; i++) {
                Object val = record.get(headers[i]);
                String strVal = val != null ? val.toString() : "";
                // Simple escaping for CSV
                if (strVal.contains(",") || strVal.contains("\"")) {
                    strVal = "\"" + strVal.replace("\"", "\"\"") + "\"";
                }
                sb.append(strVal);
                if (i < headers.length - 1) sb.append(",");
            }
            sb.append("\n");
        }
        
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
}
