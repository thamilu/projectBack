package com.eshop.app;

import org.junit.jupiter.api.Test;
import java.io.*;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;

public class PostalCodeSeederTest {

    static class RobustJsonReader extends Reader {
        private final Reader delegate;
        private final int[] buf = new int[3];
        private int bufLen = 0;
        private int bufPos = 0;

        public RobustJsonReader(Reader delegate) {
            this.delegate = delegate;
        }

        @Override
        public int read(char[] cbuf, int off, int len) throws IOException {
            int charsRead = 0;
            while (charsRead < len) {
                int next = getNextChar();
                if (next == -1) {
                    return charsRead == 0 ? -1 : charsRead;
                }
                cbuf[off + charsRead] = (char) next;
                charsRead++;
            }
            return charsRead;
        }

        private int getNextChar() throws IOException {
            if (bufPos < bufLen) {
                return buf[bufPos++];
            }
            bufLen = 0;
            bufPos = 0;

            int c = delegate.read();
            if (c == '\\') {
                int c2 = delegate.read();
                if (c2 == '\\') {
                    int c3 = delegate.read();
                    if (c3 == '"') {
                        // We found \\". This is invalid JSON.
                        // Let's replace it with a single escaped quote: \"
                        buf[bufLen++] = '"';
                        return '\\';
                    } else {
                        buf[bufLen++] = '\\';
                        if (c3 != -1) {
                            buf[bufLen++] = c3;
                        }
                        return '\\';
                    }
                } else {
                    if (c2 != -1) {
                        buf[bufLen++] = c2;
                    }
                    return '\\';
                }
            }
            return c;
        }

        @Override
        public void close() throws IOException {
            delegate.close();
        }
    }

    @Test
    void testFindAllSyntaxErrors() {
        File file = new File("src/main/resources/seed/postal_codes.json");
        System.out.println("Checking file: " + file.getAbsolutePath());
        if (!file.exists()) {
            System.out.println("File does not exist!");
            return;
        }

        try (InputStream is = new FileInputStream(file);
             Reader reader = new RobustJsonReader(new InputStreamReader(is, "UTF-8"))) {
            JsonFactory factory = new JsonFactory();
            ObjectMapper mapper = new ObjectMapper();

            try (JsonParser parser = factory.createParser(reader)) {
                if (parser.nextToken() != JsonToken.START_ARRAY) {
                    System.out.println("Not a start array");
                    return;
                }

                long count = 0;
                Set<String> unrecognizedStates = new TreeSet<>();
                while (true) {
                    try {
                        JsonToken token = parser.nextToken();
                        if (token == null || token == JsonToken.END_ARRAY) {
                            break;
                        }
                        if (token == JsonToken.START_OBJECT) {
                            Map<String, Object> row = mapper.readValue(parser, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
                            count++;
                            String stateName = (String) row.get("stateName");
                            if (stateName != null) {
                                String code = com.eshop.app.location.shared.util.IndianStateMapper.getStateCode(stateName);
                                if ("XX".equals(code)) {
                                    unrecognizedStates.add(stateName);
                                }
                            }
                            if (count % 100000 == 0) {
                                System.out.println("Parsed: " + count + " objects successfully.");
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("JSON parse error after " + count + " successfully parsed objects.");
                        System.err.println("Error details: " + e.getMessage());
                        break;
                    }
                }
                System.out.println("Parsed successfully: " + count + " objects.");
                System.out.println("Unrecognized states (count = " + unrecognizedStates.size() + "): " + unrecognizedStates);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
