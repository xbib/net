package org.xbib.net.mime.stream;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class QuotedPrintableOutputStream extends FilterOutputStream {

    // The encoding table
    private final static char[] hex = {
            '0','1', '2', '3', '4', '5', '6', '7',
            '8','9', 'A', 'B', 'C', 'D', 'E', 'F'
    };

    private int count = 0;

    private final int bytesPerLine;

    private boolean gotSpace = false;

    private boolean gotCR = false;

    /**
     * Create a QP encoder that encodes the specified input stream
     * @param out        the output stream
     * @param bytesPerLine  the number of bytes per line. The encoder
     *                   inserts a CRLF sequence after this many number
     *                   of bytes.
     */
    public QuotedPrintableOutputStream(OutputStream out, int bytesPerLine) {
        super(out);
        this.bytesPerLine = bytesPerLine - 1;
    }

    /**
     * Create a QP encoder that encodes the specified input stream.
     * Inserts the CRLF sequence after outputting 76 bytes.
     * @param out        the output stream
     */
    public QuotedPrintableOutputStream(OutputStream out) {
        this(out, 76);
    }

    /**
     * Encodes <code>len</code> bytes from the specified
     * <code>byte</code> array starting at offset <code>off</code> to
     * this output stream.
     *
     * @param      b     the data.
     * @param      off   the start offset in the data.
     * @param      len   the number of bytes to write.
     * @exception  IOException  if an I/O error occurs.
     */
    public void write(byte[] b, int off, int len) throws IOException {
        for (int i = 0; i < len; i++) {
            write(b[off + i]);
        }
    }

    /**
     * Encodes <code>b.length</code> bytes to this output stream.
     * @param      b   the data to be written.
     * @exception  IOException  if an I/O error occurs.
     */
    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    /**
     * Encodes the specified <code>byte</code> to this output stream.
     * @param      c   the <code>byte</code>.
     * @exception  IOException  if an I/O error occurs.
     */
    public void write(int c) throws IOException {
        c = c & 0xff;
        if (gotSpace) {
            output(' ', c == '\r' || c == '\n');
            gotSpace = false;
        }
        if (c == '\r') {
            gotCR = true;
            outputCRLF();
        } else {
            if (c == '\n') {
                if (!gotCR) {
                    outputCRLF();
                }
            } else {
                if (c == ' ') {
                    gotSpace = true;
                } else {
                    output(c, c < 32 || c >= 127 || c == '=');
                }
            }
            gotCR = false;
        }
    }

    /**
     * Flushes this output stream and forces any buffered output bytes
     * to be encoded out to the stream.
     * @exception  IOException  if an I/O error occurs.
     */
    public void flush() throws IOException {
        out.flush();
    }

    /**
     * Forces any buffered output bytes to be encoded out to the stream
     * and closes this output stream
     */
    public void close() throws IOException {
        out.close();
    }

    private void outputCRLF() throws IOException {
        out.write('\r');
        out.write('\n');
        count = 0;
    }

    private void output(int c, boolean encode) throws IOException {
        if (encode) {
            if ((count += 3) > bytesPerLine) {
                out.write('=');
                out.write('\r');
                out.write('\n');
                count = 3; // set the next line's length
            }
            out.write('=');
            out.write(hex[c >> 4]);
            out.write(hex[c & 0xf]);
        } else {
            if (++count > bytesPerLine) {
                out.write('=');
                out.write('\r');
                out.write('\n');
                count = 1; // set the next line's length
            }
            out.write(c);
        }
    }
}
