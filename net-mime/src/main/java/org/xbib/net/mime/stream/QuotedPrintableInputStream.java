package org.xbib.net.mime.stream;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;

public final class QuotedPrintableInputStream extends FilterInputStream {

	private int spaces = 0;

    public QuotedPrintableInputStream(InputStream in) {
        super(new PushbackInputStream(in, 2));
    }

    /**
     * Read the next decoded byte from this input stream. The byte
     * is returned as an <code>int</code> in the range <code>0</code>
     * to <code>255</code>. If no byte is available because the end of
     * the stream has been reached, the value <code>-1</code> is returned.
     * This method blocks until input data is available, the end of the
     * stream is detected, or an exception is thrown.
     *
     * @return the next byte of data, or <code>-1</code> if the end of the
     * stream is reached.
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public int read() throws IOException {
        if (spaces > 0) {
            spaces--;
            return ' ';
        }
        int c = in.read();
        if (c == ' ') {
            while ((c = in.read()) == ' ') {
                spaces++;
            }
            if (c == '\r' || c == '\n' || c == -1) {
                spaces = 0;
            } else {
                ((PushbackInputStream) in).unread(c);
                c = ' ';
            }
            return c;
        } else if (c == '=') {
            int a = in.read();
            if (a == '\n') {
                return read();
            } else if (a == '\r') {
                int b = in.read();
                if (b != '\n') {
                    ((PushbackInputStream) in).unread(b);
                }
                return read();
            } else if (a == -1) {
                return -1;
            } else {
                return Hex.fromHex(a, in.read());
            }
        }
        return c;
    }

    @Override
    public int read(byte[] buf, int off, int len) throws IOException {
        int i, c;
        for (i = 0; i < len; i++) {
            if ((c = read()) == -1) {
                if (i == 0) {
                    i = -1;
                }
                break;
            }
            buf[off + i] = (byte) c;
        }
        return i;
    }

    @Override
    public long skip(long n) throws IOException {
        long skipped = 0;
        while (n-- > 0 && read() >= 0) {
            skipped++;
        }
        return skipped;
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    @Override
    public int available() throws IOException {
        return in.available();
    }
}
