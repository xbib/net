package org.xbib.net.mime;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MimeParser implements Iterable<MimeEvent> {

    private static final Logger logger = Logger.getLogger(MimeParser.class.getName());

    private static final int NO_WHITESPACE_LENGTH = 1000;

    private final InputStream inputStream;

    private final byte[] boundaryBytes;
    
    private final int boundaryLength;

    private final int[] badCharacterShift;

    private final int[] goodSuffixShiftTable;

    private final int capacity;

    private final int chunkSize;

    private State state;

    private boolean parsed;

    private boolean done;

    private boolean eof;

    private byte[] buf;

    private int length;

    private boolean bol;

    private int offset;

    public MimeParser(InputStream inputStream, String boundary) {
        this(inputStream, boundary, 8192);
    }

    public MimeParser(InputStream inputStream, String boundary, int chunkSize) {
        this.inputStream = inputStream;
        this.boundaryBytes = getBytes("--" + boundary);
        this.boundaryLength = boundaryBytes.length;
        this.chunkSize = chunkSize;
        this.state = State.START_MESSAGE;
        this.goodSuffixShiftTable = new int[boundaryLength];
        this.badCharacterShift = new int[128];
        this.done = false;
        compileBoundaryPattern();
        this.capacity = chunkSize + 2 + boundaryLength + 4 + NO_WHITESPACE_LENGTH;
        this.buf = new byte[capacity];
    }

    private static byte[] getBytes(String s) {
        char[] chars = s.toCharArray();
        int size = chars.length;
        byte[] bytes = new byte[size];
        for (int i = 0; i < size; i++) {
            bytes[i] = (byte) chars[i];
        }
        return bytes;
    }

    @Override
    public MimeEventIterator iterator() {
        return new MimeEventIterator();
    }

    /**
     * Collects the headers for the current part by parsing the stream.
     *
     * @return headers for the current part
     */
    private InternetHeaders readHeaders() throws IOException {
        if (!eof) {
            fillBuf();
        }
        this.offset = 0;
        return new InternetHeaders();
    }

    /**
     * Reads and saves the part of the current attachment part's content.
     * At the end of this method, buf should have the remaining data
     * at index 0.
     *
     * @return a chunk of the part's content
     */
    private ByteBuffer readBody() throws MimeException, IOException {
        if (!eof) {
            fillBuf();
        }
        int start = matchBoundary(buf, 0, length);
        if (start == -1) {
            int chunkSize = eof ? length : this.chunkSize;
            if (eof) {
                done = true;
                throw new MimeException("reached EOF, but there is no closing MIME boundary");
            }
            return adjustBuf(chunkSize, length - chunkSize);
        }
        int chunkLen = start;
        if (bol && start == 0) {
        } else if (start > 0 && (buf[start - 1] == '\n' || buf[start - 1] == '\r')) {
            --chunkLen;
            if (buf[start - 1] == '\n' && start > 1 && buf[start - 2] == '\r') {
                --chunkLen;
            }
        } else {
            return adjustBuf(start + 1, length - start - 1);
        }
        if (start + boundaryLength + 1 < length && buf[start + boundaryLength] == '-' && buf[start + boundaryLength + 1] == '-') {
            state = State.END_PART;
            done = true;
            return adjustBuf(chunkLen, 0);
        }
        int whitespaceLength = 0;
        for (int i = start + boundaryLength; i < length && (buf[i] == ' ' || buf[i] == '\t'); i++) {
            ++whitespaceLength;
        }
        if (start + boundaryLength + whitespaceLength < length && buf[start + boundaryLength + whitespaceLength] == '\n') {
            state = State.END_PART;
            return adjustBuf(chunkLen, length - start - boundaryLength - whitespaceLength - 1);
        } else if (start + boundaryLength + whitespaceLength + 1 < length && buf[start + boundaryLength + whitespaceLength] == '\r' &&
                buf[start + boundaryLength + whitespaceLength + 1] == '\n') {
            state = State.END_PART;
            return adjustBuf(chunkLen, length - start - boundaryLength - whitespaceLength - 2);
        } else if (start + boundaryLength + whitespaceLength + 1 < length) {
            return adjustBuf(chunkLen + 1, length - chunkLen - 1);
        } else if (eof) {
            done = true;
            throw new MimeException("reached EOF, but there is no closing MIME boundary");
        }
        return adjustBuf(chunkLen, length - chunkLen);
    }

    /**
     * Returns a chunk from the original buffer. A new buffer is
     * created with the remaining bytes.
     *
     * @param chunkSize create a chunk with these many bytes
     * @param remaining bytes from the end of the buffer that need to be copied to
     *                  the beginning of the new buffer
     * @return chunk
     */
    private ByteBuffer adjustBuf(int chunkSize, int remaining) {
        byte[] temp = buf;
        buf = new byte[Math.min(capacity, remaining)];
        System.arraycopy(temp, length - remaining, buf, 0, remaining);
        length = remaining;
        return ByteBuffer.wrap(temp, 0, chunkSize);
    }

    /**
     * Skips the preamble to find the first attachment part
     */
    private void skipPreamble() throws MimeException, IOException {
        while (true) {
            if (!eof) {
                fillBuf();
            }
            int start = matchBoundary(buf, 0, length);
            if (start == -1) {
                if (eof) {
                    throw new MimeException("missing start boundary");
                } else {
                    adjustBuf(length - boundaryLength + 1, boundaryLength - 1);
                    continue;
                }
            }
            if (start > chunkSize) {
                adjustBuf(start, length - start);
                continue;
            }
            int whitespaceLength = 0;
            for (int i = start + boundaryLength; i < length && (buf[i] == ' ' || buf[i] == '\t'); i++) {
                ++whitespaceLength;
            }
            if (start + boundaryLength + whitespaceLength < length && (buf[start + boundaryLength + whitespaceLength] == '\n' || buf[start + boundaryLength + whitespaceLength] == '\r')) {
                if (buf[start + boundaryLength + whitespaceLength] == '\n') {
                    adjustBuf(start + boundaryLength + whitespaceLength + 1, length - start - boundaryLength - whitespaceLength - 1);
                    break;
                } else if (start + boundaryLength + whitespaceLength + 1 < length && buf[start + boundaryLength + whitespaceLength + 1] == '\n') {
                    adjustBuf(start + boundaryLength + whitespaceLength + 2, length - start - boundaryLength - whitespaceLength - 2);
                    break;
                }
            }
            adjustBuf(start + 1, length - start - 1);
        }
        logger.log(Level.FINER, "skipped the preamble. buffer length =" + length);
    }

    /**
     * Boyer-Moore search method. Copied from java.util.regex.Pattern.java
     * Pre calculates arrays needed to generate the bad character
     * shift and the good suffix shift. Only the last seven bits
     * are used to see if chars match; This keeps the tables small
     * and covers the heavily used ASCII range, but occasionally
     * results in an aliased match for the bad character shift.
     */
    private void compileBoundaryPattern() {
        int i;
        int j;
        for (i = 0; i < boundaryLength; i++) {
            badCharacterShift[boundaryBytes[i] & 0x7F] = i + 1;
        }
        NEXT:
        for (i = boundaryLength; i > 0; i--) {
            for (j = boundaryLength - 1; j >= i; j--) {
                if (boundaryBytes[j] == boundaryBytes[j - i]) {
                    goodSuffixShiftTable[j - 1] = i;
                } else {
                    continue NEXT;
                }
            }
            while (j > 0) {
                goodSuffixShiftTable[--j] = i;
            }
        }
        goodSuffixShiftTable[boundaryLength - 1] = 1;
    }

    /**
     * Finds the boundary in the given buffer using Boyer-Moore algo.
     * Copied from java.util.regex.Pattern.java
     *
     * @param buffer boundary to be searched
     * @param off   start index
     * @param len   number of bytes
     * @return -1 if there is no match or index where the match starts
     */
    private int matchBoundary(byte[] buffer, int off, int len) {
        logger.log(Level.FINER, "matching boundary = " + new String(boundaryBytes) + " len = " + boundaryLength);
        logger.log(Level.FINER, "off = " + off + " len = " + len + " buffer = " + new String(buffer, off, len));
        int last = len - boundaryLength;
        NEXT:
        while (off <= last) {
            for (int j = boundaryLength - 1; j >= 0; j--) {
                byte ch = buffer[off + j];
                if (ch != boundaryBytes[j]) {
                    off += Math.max(j + 1 - badCharacterShift[ch & 0x7F], goodSuffixShiftTable[j]);
                    continue NEXT;
                }
            }
            logger.log(Level.FINER, "found at " + off);
            return off;
        }
        logger.log(Level.FINER, "not found!");
        return -1;
    }

    /**
     * Fills the remaining buf to the full capacity
     */
    private void fillBuf() throws IOException {
        if (logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, "before fillBuf(): buffer len=" + length);
        }
        while (length < buf.length) {
            int read = inputStream.read(buf, length, buf.length - length);
            if (read == -1) {
                eof = true;
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("closing the input stream");
                }
                inputStream.close();
                break;
            } else {
                length += read;
            }
        }
        if (logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, "after fillBuf(): buffer len=" + length);
        }
    }

    private void doubleBuf() throws IOException {
        byte[] temp = new byte[2 * length];
        System.arraycopy(buf, 0, temp, 0, length);
        buf = temp;
        if (!eof) {
            fillBuf();
        }
    }

    private String readLine() throws IOException {
        int headerLength = 0;
        int whitespaceLength = 0;
        while (offset + headerLength < length) {
            if (buf[offset + headerLength] == '\n') {
                whitespaceLength = 1;
                break;
            }
            if (offset + headerLength + 1 == length) {
                doubleBuf();
            }
            if (offset + headerLength + 1 >= length) {
                return null;
            }
            if (buf[offset + headerLength] == '\r' && buf[offset + headerLength + 1] == '\n') {
                whitespaceLength = 2;
                break;
            }
            ++headerLength;
        }
        if (headerLength == 0) {
            adjustBuf(offset + whitespaceLength, length - offset - whitespaceLength);
            return null;
        }
        String hdr = new String(buf, offset, headerLength, StandardCharsets.ISO_8859_1);
        offset += headerLength + whitespaceLength;
        return hdr;
    }

    private enum State {
        START_MESSAGE, SKIP_PREAMBLE, START_PART, HEADERS, BODY, END_PART, END_MESSAGE
    }

    static class Hdr implements Header {

        private final String name;

        private String line;

        private Hdr(String l) {
            int i = l.indexOf(':');
            if (i < 0) {
                name = l.trim();
            } else {
                name = l.substring(0, i).trim();
            }
            line = l;
        }

        Hdr(String n, String v) {
            name = n;
            line = n + ": " + v;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getValue() {
            int i = line.indexOf(':');
            if (i < 0) {
                return line;
            }
            int j;
            if (name.equalsIgnoreCase("Content-Description")) {
                // Content-Description should retain the folded whitespace after header unfolding -
                // rf. RFC2822 section 2.2.3, rf. RFC2822 section 3.2.3
                for (j = i + 1; j < line.length(); j++) {
                    char c = line.charAt(j);
                    if (!(c == '\t' || c == '\r' || c == '\n')) {
                        break;
                    }
                }
            } else {
                // skip whitespace after ':'
                for (j = i + 1; j < line.length(); j++) {
                    char c = line.charAt(j);
                    if (!(c == ' ' || c == '\t' || c == '\r' || c == '\n')) {
                        break;
                    }
                }
            }
            return line.substring(j);
        }
    }

    public class MimeEventIterator implements Iterator<MimeEvent> {

        MimeEventIterator() {
        }

        @Override
        public boolean hasNext() {
            return !parsed;
        }

        @SuppressWarnings("fallthrough")
        @Override
        public MimeEvent next() {
            try {
                if (parsed) {
                    throw new NoSuchElementException();
                }
                if (logger.isLoggable(Level.FINER)) {
                    logger.log(Level.FINER, "MIMEParser state=" + state.name());
                }
                switch (state) {
                    case START_MESSAGE:
                        state = State.SKIP_PREAMBLE;
                        return StartMessage.INSTANCE;
                    case SKIP_PREAMBLE:
                        skipPreamble();
                        // fall through
                    case START_PART:
                        state = State.HEADERS;
                        return StartPart.INSTANCE;
                    case HEADERS:
                        InternetHeaders ih = readHeaders();
                        state = State.BODY;
                        bol = true;
                        return new Headers(ih);
                    case BODY:
                        ByteBuffer buf = readBody();
                        bol = false;
                        return new Content(buf);
                    case END_PART:
                        if (done) {
                            state = State.END_MESSAGE;
                        } else {
                            state = State.START_PART;
                        }
                        return EndPart.INSTANCE;
                    case END_MESSAGE:
                        parsed = true;
                        return EndMessage.INSTANCE;
                }
                // unreachable
                return null;
            } catch (Exception e) {
                throw new NoSuchElementException(e.getMessage());
            }
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * A utility class that manages RFC822 style
     * headers. Given an RFC822 format message stream, it reads lines
     * until the blank line that indicates end of header. The input stream
     * is positioned at the start of the body. The lines are stored
     * within the object and can be extracted as either Strings or
     * {@link Header} objects.
     *
     * This class is mostly intended for service providers. MimeMessage
     * and MimeBody use this class for holding their headers.
     *
     * A note on RFC822 and MIME headers
     *
     * RFC822 and MIME header fields must contain only
     * US-ASCII characters. If a header contains non US-ASCII characters,
     * it must be encoded as per the rules in RFC 2047. The MimeUtility
     * class provided in this package can be used to to achieve this.
     * Callers of the <code>setHeader</code>, <code>addHeader</code>, and
     * <code>addHeaderLine</code> methods are responsible for enforcing
     * the MIME requirements for the specified headers.  In addition, these
     * header fields must be folded (wrapped) before being sent if they
     * exceed the line length limitation for the transport (1000 bytes for
     * SMTP).  Received headers may have been folded.  The application is
     * responsible for folding and unfolding headers as appropriate.
     */
    public class InternetHeaders {

        private final List<Hdr> headers = new ArrayList<>();

        /**
         * Read and parse the given RFC822 message stream till the
         * blank line separating the header from the body. Store the
         * header lines inside this InternetHeaders object.
         * Note that the header lines are added into this InternetHeaders
         * object, so any existing headers in this object will not be
         * affected.
         */
        public InternetHeaders() throws IOException {
            String line;
            String prevline = null;
            StringBuilder stringBuilder = new StringBuilder();
            do {
                line = readLine();
                if (line != null && (line.startsWith(" ") || line.startsWith("\t"))) {
                    if (prevline != null) {
                        stringBuilder.append(prevline);
                        prevline = null;
                    }
                    stringBuilder.append("\r\n").append(line);
                } else {
                    if (prevline != null) {
                        addHeaderLine(prevline);
                    } else if (stringBuilder.length() > 0) {
                        addHeaderLine(stringBuilder.toString());
                        stringBuilder.setLength(0);
                    }
                    prevline = line;
                }
            } while (line != null && line.length() > 0);
        }

        /**
         * Return all the values for the specified header. The
         * values are String objects. Returns <code>null</code>
         * if no headers with the specified name exist.
         *
         * @param    name header name
         * @return array of header values, or null if none
         */
        public List<String> getHeader(String name) {
            List<String> v = new ArrayList<>();
            for (Hdr h : headers) {
                if (name.equalsIgnoreCase(h.name)) {
                    v.add(h.getValue());
                }
            }
            return v.isEmpty() ? null : v;
        }

        /**
         * Return all the headers as an Enumeration of
         * {@link Header} objects.
         *
         * @return Header objects
         */
        public List<? extends Header> getAllHeaders() {
            return headers;
        }

        /**
         * Add an RFC822 header line to the header store.
         * If the line starts with a space or tab (a continuation line),
         * add it to the last header line in the list.
         * Note that RFC822 headers can only contain US-ASCII characters
         *
         * @param line raw RFC822 header line
         */
        private void addHeaderLine(String line) {
            char c = line.charAt(0);
            if (c == ' ' || c == '\t') {
                Hdr h = headers.get(headers.size() - 1);
                h.line += "\r\n" + line;
            } else {
                headers.add(new Hdr(line));
            }
        }
    }
}
