

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * [HARDEN] Enterprise utility to convert formatted CSV postal data to JSON.
 * Follows the streaming principle to handle large files and ensures proper JSON escaping.
 */
public class CsvToJsonConverter {

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java CsvToJsonConverter <input_csv> <output_json>");
            return;
        }

        String csvFile = args[0];
        String jsonFile = args[1];

        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(csvFile), StandardCharsets.UTF_8));
             PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(jsonFile), StandardCharsets.UTF_8))) {

            String line;
            boolean firstLine = true;
            pw.println("[");

            while ((line = br.readLine()) != null) {
                if (firstLine) {
                    firstLine = false;
                    continue; // Skip header
                }

                if (line.trim().isEmpty()) continue;

                String[] columns = parseCsvLine(line);
                if (columns.length < 7) continue;

                // Expected CSV Mapping:
                // ID,pinCode,postOfficeName,districtName,localityName,stateName,countryName,phoneCode
                // 0  1       2              3            4            5         6           7

                String jsonLine = String.format(
                    "  {\"isoCode\": \"IN\", \"countryName\": \"%s\", \"phoneCode\": \"%s\", \"stateName\": \"%s\", \"stateCode\": \"\", \"districtName\": \"%s\", \"talukName\": \"%s\", \"pinCode\": \"%s\", \"localityName\": \"%s\", \"postOfficeName\": \"%s\"}",
                    escapeJson(columns[6]),
                    columns.length > 7 && !columns[7].isEmpty() ? escapeJson(columns[7]) : "+91",
                    escapeJson(columns[5]),
                    escapeJson(columns[3]),
                    columns.length > 8 ? escapeJson(columns[8]) : "",
                    escapeJson(columns[1]),
                    escapeJson(columns[4]),
                    escapeJson(columns[2])
                );

                pw.print(jsonLine);
                if (br.ready()) {
                    pw.println(",");
                } else {
                    pw.println();
                }
            }
            pw.println("]");
            System.out.println("Conversion completed: " + jsonFile);

        } catch (IOException e) {
            System.err.println("Error during conversion: " + e.getMessage());
            e.printStackTrace();
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
                        curVal.append('\"'); // escaped quote
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    curVal.append(ch);
                }
            } else {
                if (ch == '\"') {
                    inQuotes = true;
                } else if (ch == ',') {
                    columns.add(curVal.toString().trim());
                    curVal.setLength(0);
                } else {
                    curVal.append(ch);
                }
            }
        }
        columns.add(curVal.toString().trim());
        return columns.toArray(new String[0]);
    }

    private static String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\b", "\\b")
                  .replace("\f", "\\f")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}
