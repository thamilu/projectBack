import java.io.*;
import java.util.zip.GZIPInputStream;
import com.fasterxml.jackson.core.*;

public class RecordCounter {
    public static void main(String[] args) throws Exception {
        countRecords("g:/Project/eshop_back/src/main/resources/seed/postal_codes.json", false);
        countRecords("g:/Project/eshop_back/src/main/resources/seed/postal_codes.json.gz", true);
    }

    private static void countRecords(String path, boolean isGzip) throws Exception {
        File file = new File(path);
        if (!file.exists()) {
            System.out.println(path + " does not exist.");
            return;
        }
        
        System.out.println("Counting " + path + "...");
        InputStream is = new FileInputStream(file);
        if (isGzip) is = new GZIPInputStream(is);
        
        JsonFactory factory = new JsonFactory();
        int count = 0;
        try (JsonParser parser = factory.createParser(is)) {
            if (parser.nextToken() != JsonToken.START_ARRAY) {
                System.out.println("Not an array!");
                return;
            }
            while (parser.nextToken() != JsonToken.END_ARRAY) {
                if (parser.getCurrentToken() == JsonToken.START_OBJECT) {
                    count++;
                    parser.skipChildren();
                }
            }
        }
        System.out.println("Total records: " + count);
    }
}
