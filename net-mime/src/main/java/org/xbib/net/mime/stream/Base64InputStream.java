package org.xbib.net.mime.stream;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * An InputStream that does Base64 decoding on the data read through it.
 */
public class Base64InputStream extends FilterInputStream {

    private static final byte[] EMPTY = new byte[0];

    private static final int BUFFER_SIZE = 8192;

    private final Base64.Coder coder;

    private boolean eof;

    private byte[] inputBuffer;

    private int outputStart;

    private int outputEnd;

    public Base64InputStream(InputStream inputStream) {
        this(inputStream, Base64.DEFAULT);
    }

    /**
     * An InputStream that performs Base64 decoding on the data read
     * from the wrapped stream.
     *
     * @param inputStream the InputStream to read the source data from
     * @param flags bit flags for controlling the decoder; see the
     *        constants in {@link Base64}
     */
    public Base64InputStream(InputStream inputStream, int flags) {
        this(inputStream, flags, false);
    }

    /**
     * Performs Base64 encoding or decoding on the data read from the
     * wrapped InputStream.
     *
     * @param inputStream the InputStream to read the source data from
     * @param flags bit flags for controlling the decoder; see the
     *        constants in {@link Base64}
     * @param encode true to encode, false to decode
     */
    public Base64InputStream(InputStream inputStream, int flags, boolean encode) {
        super(inputStream);
        eof = false;
        inputBuffer = new byte[BUFFER_SIZE];
        if (encode) {
            coder = new Base64.Encoder(flags, null);
        } else {
            coder = new Base64.Decoder(flags, null);
        }
        coder.output = new byte[coder.maxOutputSize(BUFFER_SIZE)];
        outputStart = 0;
        outputEnd = 0;
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    @Override
    public void mark(int readlimit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void reset() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() throws IOException {
        in.close();
        inputBuffer = null;
    }

    @Override
    public int available() {
        return outputEnd - outputStart;
    }

    @Override
    public long skip(long n) throws IOException {
        if (outputStart >= outputEnd) {
            refill();
        }
        if (outputStart >= outputEnd) {
            return 0;
        }
        long bytes = Math.min(n, outputEnd-outputStart);
        outputStart = outputStart + (int)bytes;
        return bytes;
    }

    @Override
    public int read() throws IOException {
        if (outputStart >= outputEnd) {
            refill();
        }
        if (outputStart >= outputEnd) {
            return -1;
        } else {
            return coder.output[outputStart++] & 0xff;
        }
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (outputStart >= outputEnd) {
            refill();
        }
        if (outputStart >= outputEnd) {
            return -1;
        }
        int bytes = Math.min(len, outputEnd-outputStart);
        System.arraycopy(coder.output, outputStart, b, off, bytes);
        outputStart = outputStart + bytes;
        return bytes;
    }

    /**
     * Read data from the input stream into inputBuffer, then
     * decode/encode it into the empty coder.output, and reset the
     * outputStart and outputEnd pointers.
     */
    private void refill() throws IOException {
        if (eof) {
            return;
        }
        int bytesRead = in.read(inputBuffer);
        boolean success;
        if (bytesRead == -1) {
            eof = true;
            success = coder.process(EMPTY, 0, 0, true);
        } else {
            success = coder.process(inputBuffer, 0, bytesRead, false);
        }
        if (!success) {
            throw new IOException("bad base64 data");
        }
        outputEnd = coder.op;
        outputStart = 0;
    }
}