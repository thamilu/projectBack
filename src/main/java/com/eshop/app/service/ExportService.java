package com.eshop.app.service;

import java.util.List;
import java.util.Map;

/**
 * Reusable reporting engine for the enterprise.
 * Provides generic methods to convert lists of objects or maps into professional exports.
 */
public interface ExportService {
    /**
     * Generates an Excel file from a list of data maps.
     */
    byte[] exportToExcel(String sheetName, String[] headers, List<Map<String, Object>> data);
    
    /**
     * Generates a CSV file from a list of data maps.
     */
    byte[] exportToCsv(String[] headers, List<Map<String, Object>> data);
}
