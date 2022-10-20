package org.xbib.net.mime;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A MIME multi part message parser (RFC 2046).
 */
public class MimeMultipartParser {

    private static final Logger logger = Logger.getLogger(MimeMultipartParser.class.getName());

    private final byte[] boundary;

    private final String subType;

    private String type;

    public MimeMultipartParser(String contentType) throws MimeException {
        Objects.requireNonNull(contentType);
        int pos = contentType.indexOf(';');
        this.type = pos >= 0 ? contentType.substring(0, pos) : contentType;
        this.type = type.trim().toLowerCase();
        this.subType = "multipart".equals(type) ? null : type.substring(10).trim();
        Map<String, String> headers = parseHeaderLine(contentType);
        this.boundary = headers.containsKey("boundary") ? headers.get("boundary").getBytes(StandardCharsets.US_ASCII) : new byte[]{};
        logger.log(Level.INFO, "headers = " + headers + " boundary = " + new String(boundary));
    }

    public String getType() {
        return type;
    }

    public String getSubType() {
        return subType;
    }

    public void parse(ByteBuffer byteBuffer, MimeMultipartHandler handler) throws MimeException {
        if (boundary == null) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        boolean inHeader = true;
        boolean inBody = false;
        Integer start = null;
        Map<String, String> headers = new LinkedHashMap<>();
        int eol = 0;
        byte[] buffer = new byte[byteBuffer.remaining()];
        byteBuffer.get(buffer);
        for (int i = 0; i < buffer.length; i++) {
            byte b = buffer[i];
            if (inHeader) {
                switch (b) {
                    case '\r':
                        break;
                    case '\n':
                        if (sb.length() > 0) {
                            List<String> s = new ArrayList<>();
                            StringTokenizer tokenizer = new StringTokenizer(sb.toString(), ":");
                            while (tokenizer.hasMoreElements()) {
                                s.add(tokenizer.nextToken());
                            }
                            String k = s.size() > 0 ? s.get(0) : "";
                            String v = s.size() > 1 ? s.get(1) : "";
                            if (!k.startsWith("--")) {
                                headers.put(k.toLowerCase(), v.trim());
                            }
                            eol = 0;
                            sb.setLength(0);
                        } else {
                            eol++;
                            if (eol >= 1) {
                                eol = 0;
                                sb.setLength(0);
                                inHeader = false;
                                inBody = true;
                            }
                        }
                        break;
                    default:
                        eol = 0;
                        sb.append((char) b);
                        break;
                }
            }
            if (inBody) {
                int len = headers.containsKey("content-length") ? Integer.parseInt(headers.get("content-length")) : -1;
                if (len > 0) {
                    inBody = false;
                    inHeader = true;
                } else {
                    if (b != ('\r') && b != ('\n')) {
                        start = i;
                    }
                    if (start != null) {
                        i = indexOf(buffer, boundary, start, buffer.length);
                        if (i == -1) {
                            throw new MimeException("boundary not found");
                        }
                        int l = i - start;
                        if (l > 4) {
                            l = l - 4;
                        }
                        ByteBuffer body = ByteBuffer.wrap(buffer, start, l);
                        Map<String, String> m = new LinkedHashMap<>();
                        for (Map.Entry<String, String> entry : headers.entrySet()) {
                            m.putAll(parseHeaderLine(entry.getValue()));
                        }
                        headers.putAll(m);
                        Charset charset = StandardCharsets.ISO_8859_1;
                        if (m.containsKey("charset")) {
                            charset = Charset.forName(m.get("charset"));
                        }
                        if (handler != null) {
                            handler.handle(new MimePart(headers, body, l, charset));
                        }
                        inBody = false;
                        inHeader = true;
                        headers = new LinkedHashMap<>();
                        start = null;
                        eol = -1;
                    }
                }
            }
        }
    }

    private static Map<String, String> parseHeaderLine(String line) throws MimeException {
        Map<String, String> params = new LinkedHashMap<>();
        int pos = line.indexOf(';');
        String spec = line.substring(pos + 1);
        if (pos < 0) {
            return params;
        }
        String key = "";
        String value;
        boolean inKey = true;
        boolean inString = false;
        int start = 0;
        int i;
        for (i = 0; i < spec.length(); i++) {
            switch (spec.charAt(i)) {
                case '=':
                    if (inKey) {
                        key = spec.substring(start, i).trim().toLowerCase();
                        start = i + 1;
                        inKey = false;
                    } else if (!inString) {
                        throw new MimeException(" value has illegal character '=' at " + i + ": " + spec);
                    }
                    break;
                case ';':
                    if (inKey) {
                        if (spec.substring(start, i).trim().length() > 0) {
                            throw new MimeException(" parameter missing value at " + i + ": " + spec);
                        } else {
                            throw new MimeException("parameter key has illegal character ';' at " + i + ": " + spec);
                        }
                    } else if (!inString) {
                        value = spec.substring(start, i).trim();
                        params.put(key, value);
                        key = null;
                        start = i + 1;
                        inKey = true;
                    }
                    break;
                case '"':
                    if (inKey) {
                        throw new MimeException("key has illegal character '\"' at " + i + ": " + spec);
                    } else if (inString) {
                        value = spec.substring(start, i).trim();
                        params.put(key, value);
                        key = null;
                        for (i++; i < spec.length() && spec.charAt(i) != (';'); i++) {
                            if (!Character.isWhitespace(spec.charAt(i))) {
                                throw new MimeException(" value has garbage after quoted string at " + i + ": " + spec);
                            }
                        }
                        start = i + 1;
                        inString = false;
                        inKey = true;
                    } else {
                        if (spec.substring(start, i).trim().length() > 0) {
                            throw new MimeException("value has garbage before quoted string at " + i + ": " + spec);
                        }
                        start = i + 1;
                        inString = true;
                    }
                    break;
            }
        }
        if (inKey) {
            if (pos > start && spec.substring(start, i).trim().length() > 0) {
                throw new MimeException(" missing value at " + i + ": " + spec);
            }
        } else if (!inString) {
            value = spec.substring(start, i).trim();
            params.put(key, value);
        } else {
            throw new MimeException("has an unterminated quoted string: " + spec);
        }
        return params;
    }

    private static int indexOf(byte[] array, byte[] target, int start, int end) {
        if (target.length == 0) {
            return 0;
        }
        outer:
        for (int i = start; i < end - target.length + 1; i++) {
            for (int j = 0; j < target.length; j++) {
                if (array[i + j] != target[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    public static class MimePart implements MimeMultipart {

        private final Map<String, String> headers;

        private final ByteBuffer body;

        private final int length;

        private final Charset charset;

        public MimePart(Map<String, String> headers, ByteBuffer body, int length, Charset charSet) {
            this.headers = headers;
            this.body = body;
            this.length = length;
            this.charset = charSet;
        }

        @Override
        public Map<String, String> getHeaders() {
            return headers;
        }

        @Override
        public ByteBuffer getBody() {
            return body;
        }

        @Override
        public Charset getCharset() {
            return charset;
        }

        @Override
        public int getLength() {
            return length;
        }
    }
}
