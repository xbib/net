package org.xbib.net.util;

import java.io.IOException;
import java.time.Instant;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class JsonUtil {

    private JsonUtil() {
    }

    public static String toString(Map<String, Object> map) throws IOException {
        return map != null ? new JsonBuilder().buildMap(map).build() : null;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> toMap(String json) throws IOException {
        if (json == null) {
            return null;
        }
        JsonParser parser = new JsonParser();
        parser.parse(json);
        Object object = parser.getResult();
        if (object instanceof Map) {
            return (Map<String, Object>) parser.getResult();
        }
        throw new IllegalArgumentException(("unexpected, not a map instance: " + object.getClass()));
    }

    private static class JsonParser {

        private static final char EOS = (char) -1;

        private static final char DOUBLE_QUOTE = '"';

        private static final char BACKSLASH = '\\';

        private static final char OPEN_MAP = '{';

        private static final char CLOSE_MAP = '}';

        private static final char OPEN_LIST = '[';

        private static final char CLOSE_LIST = ']';

        private static final char COMMA = ',';

        private static final char COLON = ':';

        private String input;

        private int i;

        private char ch;

        private Object result;

        private final Deque<Object> stack = new LinkedList<>();

        public JsonParser() {
        }

        public void parse(String input) throws IOException {
            Objects.requireNonNull(input);
            this.input = input;
            this.i = 0;
            stack.clear();
            ch = next();
            skipWhitespace();
            parseValue();
            skipWhitespace();
            if (ch != EOS) {
                throw new IOException("malformed json: " + ch);
            }
        }

        public Object getResult() {
            return result;
        }

        private void parseValue() throws IOException {
            switch (ch) {
                case DOUBLE_QUOTE:
                    ch = next();
                    parseString(false);
                    break;
                case OPEN_MAP:
                    parseMap();
                    break;
                case OPEN_LIST:
                    parseList();
                    break;
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                case '-':
                    parseNumber();
                    break;
                case 't':
                    parseTrue();
                    break;
                case 'f':
                    parseFalse();
                    break;
                case 'n':
                    parseNull();
                    break;
                default:
                    throw new IOException("illegal character: " + ch);
            }
        }

        private void parseNumber() throws IOException {
            boolean minus = false;
            boolean dot = false;
            boolean exponent = false;
            int start = i - 1;
            while (true) {
                if (ch == '-') {
                    if (i - start > 1) {
                        throw new IOException("minus inside number");
                    }
                    ch =next();
                    minus = true;
                } else if (ch == 'e' || ch == 'E') {
                    ch = next();
                    if (exponent) {
                        throw new IOException("double exponents");
                    }
                    exponent = true;
                    ch = next();
                    if (ch == '-' || ch == '+') {
                        ch = next();
                        if (ch < '0' || ch > '9') {
                            throw new IOException("invalid exponent");
                        }
                    } else if (ch < '0' || ch > '9') {
                        throw new IOException("invalid exponent");
                    }
                } else if (ch == '.') {
                    ch = next();
                    if (dot) {
                        throw new IOException("multiple dots");
                    }
                    if (i - start == 1) {
                        throw new IOException("no digit before dot");
                    }
                    dot = true;
                } else if (ch >= '0' && ch <= '9') {
                    ch = next();
                } else {
                    break;
                }
            }
            if (minus && i - start == 1) {
                throw new IOException("isolated minus");
            }
            if (dot || exponent) {
                valueNode(Double.parseDouble(input.substring(start, i - 1)));
            } else {
                valueNode(Long.parseLong(input.substring(start, i - 1)));
            }
        }

        private void parseString(boolean isKey) throws IOException {
            boolean escaped = false;
            int start = i - 1;
            while (true) {
                if (ch == DOUBLE_QUOTE) {
                    if (escaped) {
                        CharSequence s = unescape(input.substring(start, i - 1));
                        if (isKey) {
                            stack.push(new KeyNode(s));
                        } else {
                            valueNode(s);
                        }
                    } else {
                        if (isKey) {
                            stack.push(new KeyNode(input.substring(start, i - 1)));
                        } else {
                            valueNode(input.substring(start, i - 1));
                        }
                    }
                    ch = next();
                    return;
                } else if (ch == BACKSLASH) {
                    escaped = true;
                    ch = next();
                    if (ch == DOUBLE_QUOTE || ch == '/' || ch == BACKSLASH || ch == 'b' || ch == 'f' || ch == 'n' || ch == 'r' || ch == 't') {
                        ch = next();
                    } else if (ch == 'u') {
                        expectHex();
                        expectHex();
                        expectHex();
                        expectHex();
                    } else {
                        throw new IOException("illegal escape char: " + ch);
                    }
                } else if (ch < 32) {
                    throw new IOException("illegal control char: " + ch);
                } else {
                    ch = next();
                }
            }
        }

        private void parseList() throws IOException {
            int count = 0;
            List<Object> list = new LinkedList<>();
            stack.push(list);
            ch = next();
            while (true) {
                skipWhitespace();
                if (ch == CLOSE_LIST) {
                    result = stack.pop();
                    tryAppend(result);
                    ch = next();
                    return;
                }
                if (count > 0) {
                    expectChar(COMMA);
                    ch = next();
                    skipWhitespace();
                }
                parseValue();
                count++;
            }
        }

        private void parseMap() throws IOException {
            int count = 0;
            Map<String, Object> map = new LinkedHashMap<>();
            stack.push(map);
            ch = next();
            while (true) {
                skipWhitespace();
                if (ch == CLOSE_MAP) {
                    result = stack.pop();
                    tryAppend(result);
                    ch = next();
                    return;
                }
                if (count > 0) {
                    expectChar(COMMA);
                    ch = next();
                    skipWhitespace();
                }
                expectChar(DOUBLE_QUOTE);
                ch = next();
                parseString(true);
                skipWhitespace();
                expectChar(COLON);
                ch = next();
                skipWhitespace();
                parseValue();
                count++;
            }
        }

        private void parseNull() throws IOException {
            ch = next();
            expectChar('u');
            ch = next();
            expectChar('l');
            ch = next();
            expectChar('l');
            valueNode(null);
            ch = next();
        }

        private void parseTrue() throws IOException {
            ch = next();
            expectChar('r');
            ch = next();
            expectChar('u');
            ch = next();
            expectChar('e');
            valueNode(true);
            ch = next();
        }

        private void parseFalse() throws IOException {
            ch = next();
            expectChar('a');
            ch = next();
            expectChar('l');
            ch = next();
            expectChar('s');
            ch = next();
            expectChar('e');
            valueNode(false);
            ch = next();
        }

        private void expectChar(char expected) throws IOException {
            if (ch != expected) {
                throw new IOException("expected char " + expected + " but got " + ch);
            }
        }

        private void expectHex() throws IOException {
            ch = next();
            if ((ch >= '0' && ch <= '9') || (ch >= 'a' && ch <= 'f') || (ch >= 'A' && ch <= 'F')) {
                return;
            }
            throw new IOException("invalid hex char " + ch);
        }

        private void skipWhitespace() {
            while (ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r') {
                ch = next();
            }
        }

        private static CharSequence unescape(CharSequence input) {
            StringBuilder result = new StringBuilder(input.length());
            int i = 0;
            while (i < input.length()) {
                if (input.charAt(i) == BACKSLASH) {
                    i++;
                    switch (input.charAt(i)) {
                        case BACKSLASH:
                            result.append(BACKSLASH);
                            break;
                        case '/':
                            result.append('/');
                            break;
                        case DOUBLE_QUOTE:
                            result.append(DOUBLE_QUOTE);
                            break;
                        case 'b':
                            result.append('\b');
                            break;
                        case 'f':
                            result.append('\f');
                            break;
                        case 'n':
                            result.append('\n');
                            break;
                        case 'r':
                            result.append('\r');
                            break;
                        case 't':
                            result.append('\t');
                            break;
                        case 'u': {
                            result.append(Character.toChars(Integer.parseInt(input.toString().substring(i + 1, i + 5), 16)));
                            i += 4;
                        }
                    }
                } else {
                    result.append(input.charAt(i));
                }
                i++;
            }
            return result;
        }

        private char next() {
            try {
                return input.charAt(i++);
            } catch (StringIndexOutOfBoundsException e) {
                return (char) -1;
            }
        }


        private void valueNode(Object object) {
            if (!tryAppend(object)) {
                stack.push(object);
                result = object;
            }
        }

        @SuppressWarnings("unchecked")
        private boolean tryAppend(Object object) {
            if (!stack.isEmpty()) {
                if (stack.peek() instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Object> list = (List<Object>) stack.peek();
                    list.add(object);
                    return true;
                } else if (stack.peek() instanceof KeyNode){
                    KeyNode key = (KeyNode) stack.pop();
                    if (stack.peek() instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> map = (Map<String, Object>) stack.peek();
                        if (map != null) {
                            String k = key != null ? key.get().toString() : null;
                            map.put(k, object);
                            return true;
                        }
                    }
                }
            }
            return false;
        }
    }

    static class KeyNode {

        private final CharSequence value;

        public KeyNode(CharSequence value) {
            this.value = value;
        }

        public CharSequence get() {
            return value;
        }

    }

    static class JsonBuilder {

        private final Appendable appendable;

        private State state;

        protected JsonBuilder() {
            this(new StringBuilder());
        }

        protected JsonBuilder(Appendable appendable) {
            this.appendable = appendable;
            this.state = new State(null, 0, Structure.DOCSTART, true);
        }

        public JsonBuilder beginCollection() throws IOException {
            this.state = new State(state, state.level + 1, Structure.COLLECTION, true);
            appendable.append('[');
            return this;
        }

        public JsonBuilder endCollection() throws IOException {
            if (state.structure != Structure.COLLECTION) {
                throw new IOException("no array to close");
            }
            appendable.append(']');
            this.state = state != null ? state.parent : null;
            return this;
        }

        public JsonBuilder beginMap() throws IOException {
            if (state.structure == Structure.COLLECTION) {
                beginArrayValue();
            }
            this.state = new State(state, state.level + 1, Structure.MAP, true);
            appendable.append('{');
            return this;
        }

        public JsonBuilder endMap() throws IOException {
            if (state.structure != Structure.MAP && state.structure != Structure.KEY) {
                throw new IOException("no object to close");
            }
            appendable.append('}');
            this.state = state != null ? state.parent : null;
            return this;
        }

        public JsonBuilder buildMap(Map<String, Object> map) throws IOException {
            Objects.requireNonNull(map);
            boolean wrap = state.structure != Structure.MAP;
            if (wrap) {
                beginMap();
            }
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                buildKey(entry.getKey());
                buildValue(entry.getValue());
            }
            if (wrap) {
                endMap();
            }
            return this;
        }

        public JsonBuilder buildCollection(Collection<?> collection) throws IOException {
            Objects.requireNonNull(collection);
            beginCollection();
            for (Object object : collection) {
                buildValue(object);
            }
            endCollection();
            return this;
        }

        @SuppressWarnings("unchecked")
        public JsonBuilder buildValue(Object object) throws IOException {
            if (object instanceof Map) {
                buildMap((Map<String, Object>) object);
                return this;
            } else if (object instanceof Collection) {
                buildCollection((Collection<Object>) object);
                return this;
            }
            if (state.structure == Structure.COLLECTION) {
                beginArrayValue();
            }
            if (object == null) {
                buildNull();
            } else if (object instanceof CharSequence) {
                buildString((CharSequence) object, true);
            } else if (object instanceof Boolean) {
                buildBoolean((Boolean) object);
            } else if (object instanceof Byte) {
                buildNumber((byte) object);
            } else if (object instanceof Integer) {
                buildNumber((int) object);
            } else if (object instanceof Long) {
                buildNumber((long) object);
            } else if (object instanceof Float) {
                buildNumber((float) object);
            } else if (object instanceof Double) {
                buildNumber((double) object);
            } else if (object instanceof Number) {
                buildNumber((Number) object);
            } else if (object instanceof Instant) {
                buildInstant((Instant) object);
            } else {
                throw new IllegalArgumentException("unable to write object class " + object.getClass());
            }
            return this;
        }

        public JsonBuilder buildKey(CharSequence string) throws IOException {
            if (state.structure == Structure.COLLECTION) {
                beginArrayValue();
            } else if (state.structure == Structure.MAP || state.structure == Structure.KEY) {
                beginKey(string != null ? string.toString() : null);
            }
            buildString(string, true);
            if (state.structure == Structure.MAP || state.structure == Structure.KEY) {
                endKey(string != null ? string.toString() : null);
            }
            state.structure = Structure.KEY;
            return this;
        }

        public JsonBuilder buildNull() throws IOException {
            if (state.structure == Structure.COLLECTION) {
                beginArrayValue();
            }
            buildString("null", false);
            return this;
        }

        public String build() {
            return appendable.toString();
        }

        private void beginKey(String k) throws IOException {
            if (state.first) {
                state.first = false;
            } else {
                appendable.append(",");
            }
        }

        private void endKey(String k) throws IOException {
            appendable.append(":");
        }

        private void beginArrayValue() throws IOException {
            if (state.first) {
                state.first = false;
            } else {
                appendable.append(",");
            }
        }

        private void buildBoolean(boolean bool) throws IOException {
            buildString(bool ? "true" : "false", false);
        }

        private void buildNumber(Number number) throws IOException {
            buildString(number != null ? number.toString() : null, false);
        }

        private void buildInstant(Instant instant) throws IOException {
            buildString(instant.toString(), true);
        }

        private void buildString(CharSequence string, boolean escape) throws IOException {
            appendable.append(escape ? escapeString(string) : string);
        }

        private CharSequence escapeString(CharSequence string) {
            StringBuilder sb = new StringBuilder();
            sb.append('"');
            int start = 0;
            int l = string.length();
            for (int i = 0; i < l; i++) {
                char c = string.charAt(i);
                if (c == '"' || c == '\\' || c < 32) {
                    if (i > start) {
                        sb.append(string, start, i);
                    }
                    start = i + 1;
                    sb.append(escapeCharacter(c));
                }
            }
            if (l > start) {
                sb.append(string, start, l);
            }
            sb.append('"');
            return sb;
        }

        private static String escapeCharacter(char c) {
            switch (c) {
                case '\n':
                    return "\\n";
                case '\r':
                    return "\\r";
                case '\t':
                    return "\\t";
                case '\\':
                    return "\\\\";
                case '\'':
                    return "\\'";
                case '\"':
                    return "\\\"";
            }
            String hex = Integer.toHexString(c);
            return "\\u0000".substring(0, 6 - hex.length()) + hex;
        }

        private enum Structure {
            DOCSTART, MAP, KEY, COLLECTION
        }

        private static class State {
            State parent;
            int level;
            Structure structure;
            boolean first;

            State(State parent, int level, Structure structure, boolean first) {
                this.parent = parent;
                this.level = level;
                this.structure = structure;
                this.first = first;
            }
        }
    }
}
