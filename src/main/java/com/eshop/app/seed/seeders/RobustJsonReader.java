package com.eshop.app.seed.seeders;

import java.io.IOException;
import java.io.Reader;

/**
 * Custom Filter Reader that dynamically fixes invalid double backslashes before double quotes
 * in the JSON stream (e.g., \\" to \") without modifying the original database seed file.
 */
public class RobustJsonReader extends Reader {
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
                    // Replace \\" with \"
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
