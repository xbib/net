package org.xbib.net.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Provides an iterator over Unicode Codepoints.
 */
public abstract class CodepointIterator implements Iterator<Codepoint> {

    protected int position = -1;

    protected int limit = -1;

    public CodepointIterator() {
    }

    /**
     * Get a CodepointIterator for the specified char array.
     * @param array char array
     * @return code point iterator
     */
    public static CodepointIterator forCharArray(char[] array) {
        return new CharArrayCodepointIterator(array);
    }

    /**
     * Get a CodepointIterator for the specified CharSequence.
     * @param seq char sequence
     * @return code point iterator
     */
    public static CodepointIterator forCharSequence(CharSequence seq) {
        return new CharSequenceCodepointIterator(seq);
    }

    public static CodepointIterator restrict(CodepointIterator ci, CodepointFilter filter) {
        return new RestrictedCodepointIterator(ci, filter, false);
    }

    public static CodepointIterator restrict(CodepointIterator ci, CodepointFilter filter, boolean scanning) {
        return new RestrictedCodepointIterator(ci, filter, scanning);
    }

    public static CodepointIterator restrict(CodepointIterator ci, CodepointFilter filter, boolean scanning, boolean invert) {
        return new RestrictedCodepointIterator(ci, filter, scanning, invert);
    }

    public CodepointIterator restrict(CodepointFilter filter) {
        return restrict(this, filter);
    }

    public CodepointIterator restrict(CodepointFilter filter, boolean scanning) {
        return restrict(this, filter, scanning);
    }

    public CodepointIterator restrict(CodepointFilter filter, boolean scanning, boolean invert) {
        return restrict(this, filter, scanning, invert);
    }

    /**
     * Get the next char.
     * @return char
     */
    protected abstract char get();

    /**
     * Get the specified char.
     * @param index index
     * @return char
     */
    protected abstract char get(int index);

    /**
     * Checks if there are codepoints remaining.
     * @return true if there are codepoints remaining
     */
    @Override
    public boolean hasNext() {
        return remaining() > 0;
    }

    /**
     * Return the final index position.
     * @return final index position
     */
    public int lastPosition() {
        int p = position();
        return (p > -1) ? (p >= limit()) ? p : p - 1 : -1;
    }

    /**
     * Return the next chars. If the codepoint is not supplemental, the char array will have a single member. If the
     * codepoint is supplemental, the char array will have two members, representing the high and low surrogate chars.
     * @return next chars
     */
    public char[] nextChars(){
        if (hasNext()) {
            if (isNextSurrogate()) {
                char c1 = get();
                if (CharUtils.isHighSurrogate(c1) && position() < limit()) {
                    char c2 = get();
                    if (CharUtils.isLowSurrogate(c2)) {
                        return new char[]{c1, c2};
                    } else {
                        throw new InvalidCharacterException(c2);
                    }
                } else if (CharUtils.isLowSurrogate(c1) && position() > 0) {
                    char c2 = get(position() - 2);
                    if (CharUtils.isHighSurrogate(c2)) {
                        return new char[]{c1, c2};
                    } else {
                        throw new InvalidCharacterException(c2);
                    }
                }
            }
            return new char[]{get()};
        }
        return null;
    }

    /**
     * Peek the next chars in the iterator. If the codepoint is not supplemental, the char array will have a single
     * member. If the codepoint is supplemental, the char array will have two members, representing the high and low
     * surrogate chars.
     * @return chars
     */
    public char[] peekChars() {
        return peekChars(position());
    }

    /**
     * Peek the specified chars in the iterator. If the codepoint is not supplemental, the char array will have a single
     * member. If the codepoint is supplemental, the char array will have two members, representing the high and low
     * surrogate chars.
     * @return chars
     */
    private char[] peekChars(int pos) {
        if (pos < 0 || pos >= limit()) {
            return null;
        }
        char c1 = get(pos);
        if (CharUtils.isHighSurrogate(c1) && pos < limit()) {
            char c2 = get(pos + 1);
            if (CharUtils.isLowSurrogate(c2)) {
                return new char[]{c1, c2};
            } else {
                throw new InvalidCharacterException(c2);
            }
        } else if (CharUtils.isLowSurrogate(c1) && pos > 1) {
            char c2 = get(pos - 1);
            if (CharUtils.isHighSurrogate(c2)) {
                return new char[]{c2, c1};
            } else {
                throw new InvalidCharacterException(c2);
            }
        } else {
            return new char[]{c1};
        }
    }

    /**
     * Return the next codepoint.
     * @return code point
     */
    @Override
    public Codepoint next() {
        if (remaining() > 0) {
            return toCodepoint(nextChars());
        } else {
            throw new NoSuchElementException();
        }
    }

    /**
     * Peek the next codepoint.
     * @return code point
     */
    public Codepoint peek() {
        return toCodepoint(peekChars());
    }

    /**
     * Peek the specified codepoint.
     * @param index index
     * @return code point
     */
    public Codepoint peek(int index) {
        return toCodepoint(peekChars(index));
    }

    private Codepoint toCodepoint(char[] chars) {
        return (chars == null) ? null : (chars.length == 1) ? new Codepoint(chars[0]) : CharUtils
                .toSupplementary(chars[0], chars[1]);
    }

    /**
     * Set the iterator position.
     * @param n iterator position
     */
    public void position(int n) {
        if (n < 0 || n > limit()) {
            throw new ArrayIndexOutOfBoundsException(n);
        }
        position = n;
    }

    /**
     * Get the iterator position.
     * @return position
     */
    public int position() {
        return position;
    }

    /**
     * Return the iterator limit.
     * @return limit
     */
    public int limit() {
        return limit;
    }

    /**
     * Return the remaining iterator size.
     * @return remaining size
     */
    public int remaining() {
        return limit - position();
    }

    private boolean isNextSurrogate() {
        if (!hasNext()) {
            return false;
        }
        char c = get(position());
        return CharUtils.isHighSurrogate(c) || CharUtils.isLowSurrogate(c);
    }

    /**
     * Returns true if the char at the specified index is a high surrogate.
     * @param index index
     * @return  true if the char at the specified index is a high surrogate
     */
    public boolean isHigh(int index) {
        if (index < 0 || index > limit()) {
            throw new ArrayIndexOutOfBoundsException(index);
        }
        return CharUtils.isHighSurrogate(get(index));
    }

    /**
     * Returns true if the char at the specified index is a low surrogate.
     * @param index index
     * @return true if the char at the specified index is a low surrogate
     */
    public boolean isLow(int index) {
        if (index < 0 || index > limit()) {
            throw new ArrayIndexOutOfBoundsException(index);
        }
        return CharUtils.isLowSurrogate(get(index));
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

}
