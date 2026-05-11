
import java.io.*;
import java.util.zip.GZIPOutputStream;

/**
 * Utility to compress large JSON files into GZIP for Git-friendly storage.
 */
public class GzipCompressor {
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Usage: java GzipCompressor <source_file_path>");
            return;
        }

        File src = new File(args[0]);
        File gzOut = new File(args[0] + ".gz");

        if (!src.exists()) {
            System.err.println("Error: Source file not found: " + src.getAbsolutePath());
            return;
        }

        System.out.println("Compressing: " + src.getName() + " (" + src.length() / (1024 * 1024) + " MB)");
        try (InputStream is = new FileInputStream(src);
             OutputStream os = new GZIPOutputStream(new FileOutputStream(gzOut))) {
            byte[] buffer = new byte[131072]; // 128KB buffer
            int len;
            while ((len = is.read(buffer)) > 0) {
                os.write(buffer, 0, len);
            }
        }
        System.out.println("Success! Created: " + gzOut.getName() + " (" + gzOut.length() / (1024 * 1024) + " MB)");
    }
}
