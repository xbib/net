package org.xbib.net.matcher;

import java.util.Arrays;
import java.util.BitSet;

/**
 *
 */
public abstract class CharMatcher {

    private static final String WHITESPACE_TABLE;

    private static final int WHITESPACE_MULTIPLIER;

    private static final int WHITESPACE_SHIFT;

    private static final CharMatcher WHITESPACE;

    private static final CharMatcher JAVA_ISO_CONTROL;

    public static final CharMatcher LITERALS;

    public static final CharMatcher PERCENT;

    public static final CharMatcher HEXDIGIT;

    static {
        WHITESPACE_TABLE = "\u2002\u3000\r\u0085\u200A\u2005\u2000\u3000\u2029\u000B\u3000\u2008\u2003\u205F\u3000" +
                "\u1680\u0009\u0020\u2006\u2001\u202F\u00A0\u000C\u2009\u3000\u2004\u3000\u3000\u2028\n\u2007\u3000";
        WHITESPACE_MULTIPLIER = 1682554634;
        WHITESPACE_SHIFT = Integer.numberOfLeadingZeros(WHITESPACE_TABLE.length() - 1);
        WHITESPACE = new FastMatcher() {
            @Override
            public boolean matches(char c) {
                return WHITESPACE_TABLE.charAt((WHITESPACE_MULTIPLIER * c) >>> WHITESPACE_SHIFT) == c;
            }

            @Override
            void setBits(BitSet table) {
                for (int i = 0; i < WHITESPACE_TABLE.length(); i++) {
                    table.set(WHITESPACE_TABLE.charAt(i));
                }
            }
        };
        JAVA_ISO_CONTROL = inRange('\u0000', '\u001f')
                .or(inRange('\u007f', '\u009f'));
        LITERALS = CharMatcher.JAVA_ISO_CONTROL
                .or(CharMatcher.WHITESPACE)
                .or(CharMatcher.anyOf("\"'<>\\^`{|}"))
                .precomputed().negate();
        PERCENT = CharMatcher.is('%');
        HEXDIGIT = CharMatcher.inRange('0', '9')
                .or(CharMatcher.inRange('a', 'f'))
                .or(CharMatcher.inRange('A', 'F'))
                .precomputed();
    }

    private static final int DISTINCT_CHARS = (Character.MAX_VALUE) - (Character.MIN_VALUE) + 1;

    private static <T> T checkNotNull(T reference) {
        if (reference == null) {
            throw new NullPointerException();
        }
        return reference;
    }

    private static void checkArgument(boolean expression) {
        if (!expression) {
            throw new IllegalArgumentException();
        }
    }

    private static int checkPositionIndex(int index, int size) {
        if (index < 0 || index > size) {
            throw new IndexOutOfBoundsException("index=" + index + " size=" + size);
        }
        return index;
    }

    private static final CharMatcher ANY = new FastMatcher() {
        @Override
        public boolean matches(char c) {
            return true;
        }

        @Override
        int indexIn(CharSequence sequence) {
            return sequence.length() == 0 ? -1 : 0;
        }

        @Override
        int indexIn(CharSequence sequence, int start) {
            int length = sequence.length();
            checkPositionIndex(start, length);
            return start == length ? -1 : start;
        }

        @Override
        int lastIndexIn(CharSequence sequence) {
            return sequence.length() - 1;
        }

        @Override
        public boolean matchesAllOf(CharSequence sequence) {
            checkNotNull(sequence);
            return true;
        }

        @Override
        public boolean matchesNoneOf(CharSequence sequence) {
            return sequence.length() == 0;
        }

        @Override
        String removeFrom(CharSequence sequence) {
            checkNotNull(sequence);
            return "";
        }

        @Override
        String replaceFrom(CharSequence sequence, char replacement) {
            char[] array = new char[sequence.length()];
            Arrays.fill(array, replacement);
            return new String(array);
        }

        @Override
        int countIn(CharSequence sequence) {
            return sequence.length();
        }

        @Override
        public CharMatcher and(CharMatcher other) {
            return checkNotNull(other);
        }

        @Override
        public CharMatcher or(CharMatcher other) {
            checkNotNull(other);
            return this;
        }

        @Override
        public CharMatcher negate() {
            return NONE;
        }
    };

    private static final CharMatcher NONE = new FastMatcher() {
        @Override
        public boolean matches(char c) {
            return false;
        }

        @Override
        int indexIn(CharSequence sequence) {
            checkNotNull(sequence);
            return -1;
        }

        @Override
        int indexIn(CharSequence sequence, int start) {
            int length = sequence.length();
            checkPositionIndex(start, length);
            return -1;
        }

        @Override
        int lastIndexIn(CharSequence sequence) {
            checkNotNull(sequence);
            return -1;
        }

        @Override
        public boolean matchesAllOf(CharSequence sequence) {
            return sequence.length() == 0;
        }

        @Override
        public boolean matchesNoneOf(CharSequence sequence) {
            checkNotNull(sequence);
            return true;
        }

        @Override
        String removeFrom(CharSequence sequence) {
            return sequence.toString();
        }

        @Override
        String replaceFrom(CharSequence sequence, char replacement) {
            return sequence.toString();
        }

        @Override
        int countIn(CharSequence sequence) {
            checkNotNull(sequence);
            return 0;
        }

        @Override
        public CharMatcher and(CharMatcher other) {
            checkNotNull(other);
            return this;
        }

        @Override
        public CharMatcher or(CharMatcher other) {
            return checkNotNull(other);
        }

        @Override
        public CharMatcher negate() {
            return ANY;
        }
    };

    public static CharMatcher is(char match) {
        return new FastMatcher() {
            @Override
            public boolean matches(char c) {
                return c == match;
            }

            @Override
            String replaceFrom(CharSequence sequence, char replacement) {
                return sequence.toString().replace(match, replacement);
            }

            @Override
            public CharMatcher and(CharMatcher other) {
                return other.matches(match) ? this : NONE;
            }

            @Override
            public CharMatcher or(CharMatcher other) {
                return other.matches(match) ? other : super.or(other);
            }

            @Override
            public CharMatcher negate() {
                return isNot(match);
            }

            @Override
            void setBits(BitSet table) {
                table.set((int) match);
            }
        };
    }

    public static CharMatcher isNot(char match) {
        return new FastMatcher() {
            @Override
            public boolean matches(char c) {
                return c != match;
            }

            @Override
            public CharMatcher and(CharMatcher other) {
                return other.matches(match) ? super.and(other) : other;
            }

            @Override
            public CharMatcher or(CharMatcher other) {
                return other.matches(match) ? ANY : this;
            }

            @Override
            void setBits(BitSet table) {
                table.set(0, match);
                table.set((match) + 1, (Character.MAX_VALUE) + 1);
            }

            @Override
            public CharMatcher negate() {
                return is(match);
            }
        };
    }

    public static CharMatcher anyOf(CharSequence sequence) {
        switch (sequence.length()) {
            case 0:
                return NONE;
            case 1:
                return is(sequence.charAt(0));
            case 2:
                return isEither(sequence.charAt(0), sequence.charAt(1));
            default:
                break;
        }
        char[] chars = sequence.toString().toCharArray();
        Arrays.sort(chars);
        return new CharMatcher() {
            @Override
            public boolean matches(char c) {
                return Arrays.binarySearch(chars, c) >= 0;
            }

            @Override
            void setBits(BitSet table) {
                for (char c : chars) {
                    table.set(c);
                }
            }
        };
    }

    public static CharMatcher isEither(char match1, char match2) {
        return new FastMatcher() {
            @Override
            public boolean matches(char c) {
                return c == match1 || c == match2;
            }

            @Override
            void setBits(BitSet table) {
                table.set(match1);
                table.set(match2);
            }
        };
    }

    public static CharMatcher noneOf(CharSequence sequence) {
        return anyOf(sequence).negate();
    }

    public static CharMatcher inRange(char startInclusive, char endInclusive) {
        checkArgument(endInclusive >= startInclusive);
        return new FastMatcher() {
            @Override
            public boolean matches(char c) {
                return startInclusive <= c && c <= endInclusive;
            }

            @Override
            void setBits(BitSet table) {
                table.set(startInclusive, (endInclusive) + 1);
            }
        };
    }

    protected CharMatcher() {
    }

    public abstract boolean matches(char c);

    public CharMatcher negate() {
        return new NegatedMatcher(this);
    }

    public CharMatcher and(CharMatcher other) {
        return new And(this, checkNotNull(other));
    }

    public CharMatcher or(CharMatcher other) {
        return new Or(this, other);
    }

    public CharMatcher precomputed() {
        return precomputedInternal();
    }

    private CharMatcher precomputedInternal() {
        BitSet table = new BitSet();
        setBits(table);
        int totalCharacters = table.cardinality();
        if (totalCharacters * 2 <= DISTINCT_CHARS) {
            return precomputedPositive(totalCharacters, table);
        } else {
            table.flip(Character.MIN_VALUE, (Character.MAX_VALUE) + 1);
            int negatedCharacters = DISTINCT_CHARS - totalCharacters;
            return new NegatedFastMatcher(precomputedPositive(negatedCharacters, table));
        }
    }

    private static CharMatcher precomputedPositive(int totalCharacters, BitSet table) {
        switch (totalCharacters) {
            case 0:
                return NONE;
            case 1:
                return is((char) table.nextSetBit(0));
            case 2:
                char c1 = (char) table.nextSetBit(0);
                char c2 = (char) table.nextSetBit((c1) + 1);
                return isEither(c1, c2);
            default:
                return isSmall(totalCharacters, table.length()) ?
                        SmallCharMatcher.from(table) : new BitSetMatcher(table);
        }
    }

    private static boolean isSmall(int totalCharacters, int tableLength) {
        return totalCharacters <= SmallCharMatcher.MAX_SIZE &&
                tableLength > (totalCharacters * 4 * Character.SIZE);
    }

    void setBits(BitSet table) {
        for (int c = Character.MAX_VALUE; c >= Character.MIN_VALUE; c--) {
            if (matches((char) c)) {
                table.set(c);
            }
        }
    }

    public boolean matchesAnyOf(CharSequence sequence) {
        return !matchesNoneOf(sequence);
    }

    public boolean matchesAllOf(CharSequence sequence) {
        for (int i = sequence.length() - 1; i >= 0; i--) {
            if (!matches(sequence.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public boolean matchesNoneOf(CharSequence sequence) {
        return indexIn(sequence) == -1;
    }

    int indexIn(CharSequence sequence) {
        int length = sequence.length();
        for (int i = 0; i < length; i++) {
            if (matches(sequence.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    int indexIn(CharSequence sequence, int start) {
        int length = sequence.length();
        checkPositionIndex(start, length);
        for (int i = start; i < length; i++) {
            if (matches(sequence.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    int lastIndexIn(CharSequence sequence) {
        for (int i = sequence.length() - 1; i >= 0; i--) {
            if (matches(sequence.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    int countIn(CharSequence sequence) {
        int count = 0;
        for (int i = 0; i < sequence.length(); i++) {
            if (matches(sequence.charAt(i))) {
                count++;
            }
        }
        return count;
    }

    String removeFrom(CharSequence sequence) {
        String string = sequence.toString();
        int pos = indexIn(string);
        if (pos == -1) {
            return string;
        }
        char[] chars = string.toCharArray();
        int spread = 1;
        OUT:
        while (true) {
            pos++;
            while (true) {
                if (pos == chars.length) {
                    break OUT;
                }
                if (matches(chars[pos])) {
                    break;
                }
                chars[pos - spread] = chars[pos];
                pos++;
            }
            spread++;
        }
        return new String(chars, 0, pos - spread);
    }

    String retainFrom(CharSequence sequence) {
        return negate().removeFrom(sequence);
    }

    String replaceFrom(CharSequence sequence, char replacement) {
        String string = sequence.toString();
        int pos = indexIn(string);
        if (pos == -1) {
            return string;
        }
        char[] chars = string.toCharArray();
        chars[pos] = replacement;
        for (int i = pos + 1; i < chars.length; i++) {
            if (matches(chars[i])) {
                chars[i] = replacement;
            }
        }
        return new String(chars);
    }

    boolean apply(Character character) {
        return matches(character);
    }

    private abstract static class FastMatcher extends CharMatcher {
        FastMatcher() {
            super();
        }

        @Override
        public CharMatcher precomputed() {
            return this;
        }

        @Override
        public CharMatcher negate() {
            return new NegatedFastMatcher(this);
        }
    }

    private static class NegatedMatcher extends CharMatcher {
        CharMatcher original;

        NegatedMatcher(CharMatcher original) {
            super();
            this.original = original;
        }

        @Override
        public boolean matches(char c) {
            return !original.matches(c);
        }

        @Override
        public boolean matchesAllOf(CharSequence sequence) {
            return original.matchesNoneOf(sequence);
        }

        @Override
        public boolean matchesNoneOf(CharSequence sequence) {
            return original.matchesAllOf(sequence);
        }

        @Override
        int countIn(CharSequence sequence) {
            return sequence.length() - original.countIn(sequence);
        }

        @Override
        void setBits(BitSet table) {
            BitSet tmp = new BitSet();
            original.setBits(tmp);
            tmp.flip((Character.MIN_VALUE), (Character.MAX_VALUE) + 1);
            table.or(tmp);
        }

        @Override
        public CharMatcher negate() {
            return original;
        };
    }

    private static class NegatedFastMatcher extends NegatedMatcher {
        NegatedFastMatcher(CharMatcher original) {
            super(original);
        }

        @Override
        public CharMatcher precomputed() {
            return this;
        }
    }

    private static class And extends CharMatcher {
        private final CharMatcher first;
        private final CharMatcher second;

        And(CharMatcher a, CharMatcher b) {
            super();
            first = checkNotNull(a);
            second = checkNotNull(b);
        }

        @Override
        public boolean matches(char c) {
            return first.matches(c) && second.matches(c);
        }

        @Override
        void setBits(BitSet table) {
            BitSet tmp1 = new BitSet();
            first.setBits(tmp1);
            BitSet tmp2 = new BitSet();
            second.setBits(tmp2);
            tmp1.and(tmp2);
            table.or(tmp1);
        }
    }

    private static class Or extends CharMatcher {
        private final CharMatcher first;
        private final CharMatcher second;

        Or(CharMatcher a, CharMatcher b) {
            super();
            first = checkNotNull(a);
            second = checkNotNull(b);
        }

        @Override
        void setBits(BitSet table) {
            first.setBits(table);
            second.setBits(table);
        }

        @Override
        public boolean matches(char c) {
            return first.matches(c) || second.matches(c);
        }
    }

    private static class BitSetMatcher extends FastMatcher {
        private final BitSet table;

        private BitSetMatcher(BitSet table) {
            if (table.length() + Long.SIZE < table.size()) {
                table = (BitSet) table.clone();
            }
            this.table = table;
        }

        @Override
        public boolean matches(char c) {
            return table.get(c);
        }

        @Override
        void setBits(BitSet bitSet) {
            bitSet.or(table);
        }
    }

    private static class SmallCharMatcher extends FastMatcher {

        static final int MAX_SIZE = 1023;

        private static final int C1 = 0xcc9e2d51;

        private static final int C2 = 0x1b873593;

        private static final double DESIRED_LOAD_FACTOR = 0.5d;

        private final char[] table;

        private final boolean containsZero;

        private final long filter;

        private SmallCharMatcher(char[] table, long filter, boolean containsZero) {
            super();
            this.table = table;
            this.filter = filter;
            this.containsZero = containsZero;
        }

        static int smear(int hashCode) {
            return C2 * Integer.rotateLeft(hashCode * C1, 15);
        }

        private boolean checkFilter(int c) {
            return 1 == (1 & (filter >> c));
        }

        static int chooseTableSize(int setSize) {
            if (setSize == 1) {
                return 2;
            }
            int tableSize = Integer.highestOneBit(setSize - 1) << 1;
            while (tableSize * DESIRED_LOAD_FACTOR < setSize) {
                tableSize <<= 1;
            }
            return tableSize;
        }

        static CharMatcher from(BitSet chars) {
            long filter = 0;
            int size = chars.cardinality();
            boolean containsZero = chars.get(0);
            char[] table = new char[chooseTableSize(size)];
            int mask = table.length - 1;
            for (int c = chars.nextSetBit(0); c != -1; c = chars.nextSetBit(c + 1)) {
                filter |= 1L << c;
                int index = smear(c) & mask;
                while (true) {
                    if (table[index] == 0) {
                        table[index] = (char) c;
                        break;
                    }
                    index = (index + 1) & mask;
                }
            }
            return new SmallCharMatcher(table, filter, containsZero);
        }

        @Override
        public boolean matches(char c) {
            if (c == 0) {
                return containsZero;
            }
            if (!checkFilter(c)) {
                return false;
            }
            int mask = table.length - 1;
            int startingIndex = smear(c) & mask;
            int index = startingIndex;
            while (true) {
                if (table[index] == 0) {
                    return false;
                } else if (table[index] == c) {
                    return true;
                } else {
                    index = (index + 1) & mask;
                }
                if (index == startingIndex) {
                    break;
                }
            }
            return false;
        }

        @Override
        void setBits(BitSet table) {
            if (containsZero) {
                table.set(0);
            }
            for (char c : this.table) {
                if (c != 0) {
                    table.set(c);
                }
            }
        }
    }
}
