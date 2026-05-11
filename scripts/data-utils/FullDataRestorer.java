
import java.io.*;
import java.util.zip.GZIPInputStream;

/**
 * Utility to restore a full file from its GZIP version.
 */
public class FullDataRestorer {
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Usage: java FullDataRestorer <source_gz_path>");
            return;
        }

        File src = new File(args[0]);
        String destPath = args[0].endsWith(".gz") ? args[0].substring(0, args[0].length() - 3) : args[0] + ".restored";
        File dest = new File(destPath);

        if (!src.exists()) {
            System.err.println("Error: Source GZ not found: " + src.getAbsolutePath());
            return;
        }

        System.out.println("Restoring from: " + src.getName());
        try (InputStream is = new GZIPInputStream(new FileInputStream(src));
             OutputStream os = new FileOutputStream(dest)) {
            byte[] buffer = new byte[131072];
            int len;
            while ((len = is.read(buffer)) > 0) {
                os.write(buffer, 0, len);
            }
        }
        System.out.println("Success! Restored: " + dest.getName() + " (" + dest.length() / (1024 * 1024) + " MB)");
    }
}
