package org.xbib.net.util;

/**
 * General utilities for dealing with Unicode characters.
 */
public final class CharUtils {

    public static final char LRE = 0x202A;
    public static final char RLE = 0x202B;
    public static final char LRO = 0x202D;
    public static final char RLO = 0x202E;
    public static final char LRM = 0x200E;
    public static final char RLM = 0x200F;
    public static final char PDF = 0x202C;

    private CharUtils() {
    }

    /**
     * True if the character is a valid unicode codepoint.
     * @param c char
     * @return true if the character is a valid unicode codepoint
     */
    public static boolean isValid(int c) {
        return c >= 0x000000 && c <= 0x10ffff;
    }

    /**
     * True if the character is a valid unicode codepoint.
     * @param c code point
     * @return true if the character is a valid unicode codepoint
     */
    public static boolean isValid(Codepoint c) {
        return isValid(c.getValue());
    }

    /**
     * True if all the characters in chars are within the set [low,high].
     * @param chars chars
     * @param low low
     * @param high high
     * @return true if all the characters in chars are within the set [low,high]
     */
    public static boolean inRange(char[] chars, char low, char high) {
        for (char aChar : chars) {
            if (aChar < low || aChar > high) {
                return false;
            }
        }
        return true;
    }

    /**
     * True if all the characters in chars are within the set [low,high].
     * @param chars chars
     * @param low low
     * @param high high
     * @return true if all the characters in chars are within the set [low,high]
     */
    public static boolean inRange(char[] chars, int low, int high) {
        for (int i = 0; i < chars.length; i++) {
            char n = chars[i];
            Codepoint cp =
                    (isHighSurrogate(n) && i + 1 < chars.length && isLowSurrogate(chars[i + 1]))
                            ? toSupplementary(n, chars[i++]) : new Codepoint(n);
            int c = cp.getValue();
            if (c < low || c > high) {
                return false;
            }
        }
        return true;
    }

    /**
     * True if the codepoint is within the set [low,high].
     * @param codepoint code point
     * @param low low
     * @param high high
     * @return true if the codepoint is within the set [low,high]
     */
    public static boolean inRange(int codepoint, int low, int high) {
        return codepoint >= low && codepoint <= high;
    }

    /**
     * Get the high surrogate for a particular unicode codepoint.
     * @param c char
     * @return high surrugate
     */
    public static char getHighSurrogate(int c) {
        return c >= 0x10000 ? (char) ((0xD800 - (0x10000 >> 10)) + (c >> 10)) : 0;
    }

    /**
     * Get the low surrogate for a particular unicode codepoint.
     * @param c char
     * @return low surrogate
     */
    public static char getLowSurrogate(int c) {
        return c >= 0x10000 ? (char) (0xDC00 + (c & 0x3FF)) : (char) c;
    }

    /**
     * True if the specified char is a high surrogate.
     * @param c char
     * @return true if the specified char is a high surrogate
     */
    public static boolean isHighSurrogate(char c) {
        return c <= '\uDBFF' && c >= '\uD800';
    }

    /**
     * True if the specified char is a low surrogate.
     * @param c  char
     * @return true if the specified char is a low surrogate
     */
    public static boolean isLowSurrogate(char c) {
        return c <= '\uDFFF' && c >= '\uDC00';
    }

    /**
     * True if the specified character is supplemental.
     * @param c  char
     * @return true if the specified character is supplemental
     */
    public static boolean isSupplementary(int c) {
        return c <= 0x10ffff && c >= 0x010000;
    }

    /**
     * True if the two chars represent a surrogate pair.
     * @param high high char
     * @param low low char
     * @return true if the two chars represent a surrogate pair
     */
    public static boolean isSurrogatePair(char high, char low) {
        return isHighSurrogate(high) && isLowSurrogate(low);
    }

    /**
     * Converts the high and low surrogate into a supplementary codepoint.
     * @param high high char
     * @param low low char
     * @return code point
     */
    public static Codepoint toSupplementary(char high, char low) {
        if (!isHighSurrogate(high)) {
            throw new IllegalArgumentException("Invalid High Surrogate");
        }
        if (!isLowSurrogate(low)) {
            throw new IllegalArgumentException("Invalid Low Surrogate");
        }
        return new Codepoint(((high - '\uD800') << 10) + (low - '\uDC00') + 0x010000);
    }

    /**
     * Return the codepoint at the given location, automatically dealing with surrogate pairs.
     * @param s string
     * @param i location
     * @return code point
     */
    public static Codepoint codepointAt(String s, int i) {
        char c = s.charAt(i);
        if (c < 0xD800 || c > 0xDFFF) {
            return new Codepoint(c);
        }
        if (isHighSurrogate(c) && s.length() != i) {
            char low = s.charAt(i + 1);
            if (isLowSurrogate(low)) {
                return toSupplementary(c, low);
            }
        } else if (isLowSurrogate(c) && i >= 1) {
            char high = s.charAt(i - 1);
            if (isHighSurrogate(high)) {
                return toSupplementary(high, c);
            }
        }
        return new Codepoint(c);
    }

    /**
     * Return the number of characters used to represent the codepoint (will return 1 or 2).
     * @param c code point
     * @return the number of characters used to represent the codepoint
     */
    public static int length(Codepoint c) {
        return c.getCharCount();
    }

    /**
     * Return the number of characters used to represent the codepoint (will return 1 or 2).
     * @param c code point
     * @return the number of characters used to represent the codepoint
     */
    public static int length(int c) {
        return new Codepoint(c).getCharCount();
    }

    /**
     * Return the total number of codepoints in the buffer. Each surrogate pair counts as a single codepoint.
     * @param c code point
     * @return the total number of codepoints in the buffer
     */
    public static int length(CharSequence c) {
        return length(CodepointIterator.forCharSequence(c));
    }

    /**
     * Return the total number of codepoints in the buffer. Each surrogate pair counts as a single codepoint.
     * @param c chars
     * @return the total number of codepoints in the buffer
     */
    public static int length(char[] c) {
        return length(CodepointIterator.forCharArray(c));
    }

    private static int length(CodepointIterator ci) {
        int n = 0;
        while (ci.hasNext()) {
            ci.next();
            n++;
        }
        return n;
    }

    private static String supplementaryToString(int c) {
        return String.valueOf(getHighSurrogate(c)) + getLowSurrogate(c);
    }

    /**
     * Return the String representation of the codepoint, automatically dealing with surrogate pairs.
     * @param c char
     * @return string representation of the codepoint
     */
    public static String toString(int c) {
        return isSupplementary(c) ? supplementaryToString(c) : String.valueOf((char) c);
    }

    /**
     * Removes leading and trailing bidi controls from the string.
     * @param string string
     * @return string without bidi controls
     */
    public static String stripBidi(String string) {
        String s = string;
        if (s == null || s.length() <= 1) {
            return s;
        }
        if (isBidi(s.charAt(0))) {
            s = s.substring(1);
        }
        if (isBidi(s.charAt(s.length() - 1))) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    private static String wrap(String s, char c1, char c2) {
        StringBuilder buf = new StringBuilder(s);
        if (buf.length() > 1) {
            if (buf.charAt(0) != c1) {
                buf.insert(0, c1);
            }
            if (buf.charAt(buf.length() - 1) != c2) {
                buf.append(c2);
            }
        }
        return buf.toString();
    }

    /**
     * Wrap the string with the specified bidi control.
     * @param s string
     * @param c char
     * @return string with specified bidi control
     */
    public static String wrapBidi(String s, char c) {
        switch (c) {
            case RLE:
                return wrap(s, RLE, PDF);
            case RLO:
                return wrap(s, RLO, PDF);
            case LRE:
                return wrap(s, LRE, PDF);
            case LRO:
                return wrap(s, LRO, PDF);
            case RLM:
                return wrap(s, RLM, RLM);
            case LRM:
                return wrap(s, LRM, LRM);
            default:
                return s;
        }
    }

    /**
     * True if the codepoint is a digit.
     * @param codepoint code point
     * @return true if the codepoint is a digit
     */
    public static boolean isDigit(int codepoint) {
        return inRange(codepoint, '0', '9');
    }

    /**
     * True if the codepoint is part of the ASCII alphabet (a-z, A-Z).
     * @param codepoint code point
     * @return true if the codepoint is a digit
     */
    public static boolean isAlpha(int codepoint) {
        return inRange(codepoint, 'A', 'Z') || inRange(codepoint, 'a', 'z');
    }

    /**
     * True if isAlpha and isDigit both return true.
     * @param codepoint code point
     * @return true if isAlpha and isDigit both return true
     */
    public static boolean isAlphaDigit(int codepoint) {
        return isDigit(codepoint) || isAlpha(codepoint);
    }

    public static boolean isHex(int codepoint) {
        return isDigit(codepoint) || inRange(codepoint, 'a', 'f') || inRange(codepoint, 'A', 'F');
    }

    /**
     * True if the codepoint is a bidi control character.
     * @param codepoint code point
     * @return true if the codepoint is a bidi control character
     */
    public static boolean isBidi(int codepoint) {
        return codepoint == LRM ||
                codepoint == RLM ||
                codepoint == LRE ||
                codepoint == RLE ||
                codepoint == LRO ||
                codepoint == RLO ||
                codepoint == PDF;
    }

    public static boolean isPctEnc(int codepoint) {
        return codepoint == '%' || isDigit(codepoint) ||
                inRange(codepoint, 'A', 'F') ||
                inRange(codepoint, 'a', 'f');
    }

    public static boolean isMark(int codepoint) {
        return codepoint == '-' ||
                codepoint == '_' ||
                codepoint == '.' ||
                codepoint == '!' ||
                codepoint == '~' ||
                codepoint == '*' ||
                codepoint == '\\' ||
                codepoint == '\'' ||
                codepoint == '(' ||
                codepoint == ')';
    }

    public static boolean isUnreserved(int codepoint) {
        return isAlphaDigit(codepoint) ||
                codepoint == '-' ||
                codepoint == '.' ||
                codepoint == '_' ||
                codepoint == '~';
    }

    public static boolean isReserved(int codepoint) {
        return codepoint == '$' ||
                codepoint == '&' ||
                codepoint == '+' ||
                codepoint == ',' ||
                codepoint == '/' ||
                codepoint == ':' ||
                codepoint == ';' ||
                codepoint == '=' ||
                codepoint == '?' ||
                codepoint == '@' ||
                codepoint == '[' ||
                codepoint == ']';
    }

    public static boolean isGenDelim(int codepoint) {
        return codepoint == '#' || codepoint == '/'
                || codepoint == ':'
                || codepoint == '?'
                || codepoint == '@'
                || codepoint == '['
                || codepoint == ']';
    }

    public static boolean isSubDelim(int codepoint) {
        return codepoint == '!' ||
                codepoint == '$' ||
                codepoint == '&' ||
                codepoint == '\'' ||
                codepoint == '(' ||
                codepoint == ')' ||
                codepoint == '*' ||
                codepoint == '+' ||
                codepoint == ',' ||
                codepoint == ';' ||
                codepoint == '=' ||
                codepoint == '\\';
    }

    public static boolean isPchar(int codepoint) {
        return isUnreserved(codepoint) || codepoint == ':'
                || codepoint == '@'
                || codepoint == '&'
                || codepoint == '='
                || codepoint == '+'
                || codepoint == '$'
                || codepoint == ',';
    }

    public static boolean isPath(int codepoint) {
        return isPchar(codepoint) || codepoint == ';' || codepoint == '/' || codepoint == '%' || codepoint == ',';
    }

    public static boolean isPathNoDelims(int codepoint) {
        return isPath(codepoint) && !isGenDelim(codepoint);
    }

    public static boolean isScheme(int codepoint) {
        return isAlphaDigit(codepoint) || codepoint == '+' || codepoint == '-' || codepoint == '.';
    }

    public static boolean isUserInfo(int codepoint) {
        return isUnreserved(codepoint) || isSubDelim(codepoint) || isPctEnc(codepoint);
    }

    public static boolean isQuery(int codepoint) {
        return isPchar(codepoint) || codepoint == ';' || codepoint == '/' || codepoint == '?' || codepoint == '%';
    }

    public static boolean isFragment(int codepoint) {
        return isPchar(codepoint) || codepoint == '/' || codepoint == '?' || codepoint == '%';
    }

    public static boolean isUcsChar(int codepoint) {
        return inRange(codepoint, '\u00A0', '\uD7FF') ||
                inRange(codepoint, '\uF900', '\uFDCF') ||
                inRange(codepoint, '\uFDF0', '\uFFEF') ||
                inRange(codepoint, 0x10000, 0x1FFFD) ||
                inRange(codepoint, 0x20000, 0x2FFFD) ||
                inRange(codepoint, 0x30000, 0x3FFFD) ||
                inRange(codepoint, 0x40000, 0x4FFFD) ||
                inRange(codepoint, 0x50000, 0x5FFFD) ||
                inRange(codepoint, 0x60000, 0x6FFFD) ||
                inRange(codepoint, 0x70000, 0x7FFFD) ||
                inRange(codepoint, 0x80000, 0x8FFFD) ||
                inRange(codepoint, 0x90000, 0x9FFFD) ||
                inRange(codepoint, 0xA0000, 0xAFFFD) ||
                inRange(codepoint, 0xB0000, 0xBFFFD) ||
                inRange(codepoint, 0xC0000, 0xCFFFD) ||
                inRange(codepoint, 0xD0000, 0xDFFFD) ||
                inRange(codepoint, 0xE1000, 0xEFFFD);
    }

    public static boolean isIprivate(int codepoint) {
        return inRange(codepoint, '\uE000', '\uF8FF') ||
                inRange(codepoint, 0xF0000, 0xFFFFD) ||
                inRange(codepoint, 0x100000, 0x10FFFD);
    }

    public static boolean isIunreserved(int codepoint) {
        return isAlphaDigit(codepoint) || isMark(codepoint) || isUcsChar(codepoint);
    }

    public static boolean isIpchar(int codepoint) {
        return isIunreserved(codepoint) ||
                isSubDelim(codepoint) ||
                codepoint == ':' ||
                codepoint == '@' ||
                codepoint == '&' ||
                codepoint == '=' ||
                codepoint == '+' ||
                codepoint == '$';
    }

    public static boolean isIpath(int codepoint) {
        return isIpchar(codepoint) ||
                codepoint == ';' ||
                codepoint == '/' ||
                codepoint == '%' ||
                codepoint == ',';
    }

    public static boolean isIpathnodelims(int codepoint) {
        return isIpath(codepoint) && !isGenDelim(codepoint);
    }

    public static boolean isIquery(int codepoint) {
        return isIpchar(codepoint) ||
                isIprivate(codepoint) ||
                codepoint == ';' ||
                codepoint == '/' ||
                codepoint == '?' ||
                codepoint == '%';
    }

    public static boolean isIfragment(int codepoint) {
        return isIpchar(codepoint) || isIprivate(codepoint)
                || codepoint == '/'
                || codepoint == '?'
                || codepoint == '%';
    }

    public static boolean isIregname(int codepoint) {
        return isIunreserved(codepoint) || codepoint == '!'
                || codepoint == '$'
                || codepoint == '&'
                || codepoint == '\''
                || codepoint == '('
                || codepoint == ')'
                || codepoint == '*'
                || codepoint == '+'
                || codepoint == ','
                || codepoint == ';'
                || codepoint == '='
                || codepoint == '"';
    }

    public static boolean isIpliteral(int codepoint) {
        return isHex(codepoint) || codepoint == ':'
                || codepoint == '['
                || codepoint == ']';
    }

    public static boolean isIhost(int codepoint) {
        return isIregname(codepoint) || isIpliteral(codepoint);
    }

    public static boolean isRegname(int codepoint) {
        return isUnreserved(codepoint) || codepoint == '!'
                || codepoint == '$'
                || codepoint == '&'
                || codepoint == '\''
                || codepoint == '('
                || codepoint == ')'
                || codepoint == '*'
                || codepoint == '+'
                || codepoint == ','
                || codepoint == ';'
                || codepoint == '='
                || codepoint == '"';
    }

    public static boolean isIuserinfo(int codepoint) {
        return isIunreserved(codepoint) || codepoint == ';'
                || codepoint == ':'
                || codepoint == '&'
                || codepoint == '='
                || codepoint == '+'
                || codepoint == '$'
                || codepoint == ',';
    }

    public static boolean isIserver(int codepoint) {
        return isIuserinfo(codepoint) || isIregname(codepoint)
                || isAlphaDigit(codepoint)
                || codepoint == '.'
                || codepoint == ':'
                || codepoint == '@'
                || codepoint == '['
                || codepoint == ']'
                || codepoint == '%'
                || codepoint == '-';
    }

    /**
     * Verifies a sequence of codepoints using the specified filter.
     * @param ci code point iterator
     * @param profile profile
     */
    public static void verify(CodepointIterator ci, Profile profile) {
        CodepointIterator rci = CodepointIterator.restrict(ci, profile.filter());
        while (rci.hasNext()) {
            rci.next();
        }
    }

    /**
     * Verifies a sequence of codepoints using the specified profile.
     * @param s string
     * @param profile profile
     */
    public static void verify(String s, Profile profile) {
        if (s == null) {
            return;
        }
        verify(CodepointIterator.forCharSequence(s), profile);
    }

}
