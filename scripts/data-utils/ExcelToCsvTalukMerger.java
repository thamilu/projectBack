
import org.apache.poi.ss.usermodel.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * [HARDEN] Data Utility to merge Taluk names from an All-India Pincode Excel 
 * into a standardized Pincode CSV.
 */
public class ExcelToCsvTalukMerger {

    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Usage: java ExcelToCsvTalukMerger <input_excel> <input_csv> <output_csv>");
            return;
        }

        String excelPath = args[0];
        String csvPath = args[1];
        String outputPath = args[2];

        try {
            System.out.println("Loading Excel data from: " + excelPath);
            Map<String, String> talukMap = loadExcelData(excelPath);
            System.out.println("Excel loaded. Found " + talukMap.size() + " unique office-pincode pairs.");

            System.out.println("Processing CSV: " + csvPath);
            updateCsvData(csvPath, outputPath, talukMap);
            System.out.println("Merge completed: " + outputPath);

        } catch (Exception e) {
            System.err.println("Fatal error during merge: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static Map<String, String> loadExcelData(String path) throws Exception {
        Map<String, String> map = new HashMap<>();
        try (FileInputStream fis = new FileInputStream(path);
             Workbook workbook = WorkbookFactory.create(fis)) {
            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String officeName = getCellValue(row.getCell(0));
                String pincode = getCellValue(row.getCell(1));
                String taluk = getCellValue(row.getCell(7));

                if (officeName != null && pincode != null) {
                    String key = pincode.trim() + "|" + officeName.toLowerCase().trim();
                    map.put(key, taluk);
                }
            }
        }
        return map;
    }

    private static void updateCsvData(String input, String output, Map<String, String> talukMap) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(input), StandardCharsets.UTF_8));
             PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(output), StandardCharsets.UTF_8))) {

            String line;
            boolean firstLine = true;
            int updatedCount = 0;
            int totalCount = 0;

            while ((line = br.readLine()) != null) {
                if (firstLine) {
                    pw.println(line);
                    firstLine = false;
                    continue;
                }

                String[] cols = parseCsvLine(line);
                totalCount++;

                if (cols.length >= 3) {
                    String pinCode = cols[1];
                    String poName = cols[2];
                    String existingTaluk = cols.length > 8 ? cols[8] : "";

                    if (existingTaluk.isEmpty() || existingTaluk.equals("\"\"")) {
                        String key = pinCode.trim() + "|" + poName.toLowerCase().trim();
                        String newTaluk = talukMap.get(key);
                        if (newTaluk != null && !newTaluk.isEmpty()) {
                            String[] newCols = new String[9];
                            System.arraycopy(cols, 0, newCols, 0, Math.min(cols.length, 9));
                            newCols[8] = newTaluk;
                            cols = newCols;
                            updatedCount++;
                        }
                    }
                }
                pw.println(formatCsvLine(cols));
            }
            System.out.println("Stats -> Total rows: " + totalCount + ", Updated: " + updatedCount);
        }
    }

    private static String getCellValue(Cell cell) {
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case STRING: return cell.getStringCellValue();
            case NUMERIC: 
                if (DateUtil.isCellDateFormatted(cell)) return cell.getDateCellValue().toString();
                return String.valueOf((long)cell.getNumericCellValue());
            case BOOLEAN: return String.valueOf(cell.getBooleanCellValue());
            default: return null;
        }
    }

    private static String[] parseCsvLine(String line) {
        List<String> columns = new ArrayList<>();
        StringBuilder curVal = new StringBuilder();
        boolean inQuotes = false;
        char[] chars = line.toCharArray();

        for (int i = 0; i < chars.length; i++) {
            char ch = chars[i];
            if (inQuotes) {
                if (ch == '\"') {
                    if (i + 1 < chars.length && chars[i + 1] == '\"') {
                        curVal.append('\"'); i++;
                    } else inQuotes = false;
                } else curVal.append(ch);
            } else {
                if (ch == '\"') inQuotes = true;
                else if (ch == ',') {
                    columns.add(curVal.toString().trim());
                    curVal.setLength(0);
                } else curVal.append(ch);
            }
        }
        columns.add(curVal.toString().trim());
        return columns.toArray(new String[0]);
    }

    private static String formatCsvLine(String[] cols) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cols.length; i++) {
            String col = cols[i] == null ? "" : cols[i];
            if (col.contains(",") || col.contains("\"")) {
                sb.append("\"").append(col.replace("\"", "\"\"")).append("\"");
            } else {
                sb.append(col);
            }
            if (i < cols.length - 1) sb.append(",");
        }
        return sb.toString();
    }
}
