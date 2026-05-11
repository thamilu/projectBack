
import java.io.*;
import java.util.zip.GZIPInputStream;

/**
 * Utility to split the massive postal_codes.json into Git-friendly chunks.
 * Standardizes parts to ~175,000 records (~40MB) each.
 */
public class PostalCodeSplitter {
    public static void main(String[] args) throws Exception {
        File src = new File("src/main/resources/seed/postal_codes.json.gz");
        if (!src.exists()) {
            System.err.println("Error: Source GZIP not found at " + src.getAbsolutePath());
            return;
        }

        int RECORDS_PER_FILE = 175000;
        int fileCount = 1;
        int currentRecords = 0;
        
        System.out.println("Reading from: " + src.getName());

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(src))))) {
            PrintWriter writer = null;
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.startsWith("{")) {
                    if (currentRecords % RECORDS_PER_FILE == 0) {
                        if (writer != null) {
                            writer.println("\n]");
                            writer.close();
                        }
                        File dest = new File("src/main/resources/seed/postal_codes_part" + fileCount++ + ".json");
                        System.out.println("Writing to: " + dest.getName());
                        writer = new PrintWriter(new FileWriter(dest));
                        writer.println("[");
                        currentRecords = 0;
                    }
                    
                    if (currentRecords > 0) {
                        writer.println(",");
                    }
                    
                    String jsonLine = trimmed.endsWith(",") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
                    writer.print("  " + jsonLine);
                    currentRecords++;
                }
            }
            if (writer != null) {
                writer.println("\n]");
                writer.close();
            }
        }
        System.out.println("Success! Dataset split into " + (fileCount - 1) + " parts.");
    }
}
