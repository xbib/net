package org.xbib.net.util;

/**
 * Represents a single Unicode Codepoint.
 */
public class Codepoint implements Comparable<Codepoint> {

    private final int value;

    /**
     * Create a codepoint from a single char.
     * @param value char
     */
    public Codepoint(char value) {
        this((int) value);
    }

    /**
     * Create a codepoint from a specific integer value.
     * @param value value
     */
    public Codepoint(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("invalid codepoint");
        }
        this.value = value;
    }

    /**
     * The codepoint value.
     * @return value
     */
    public int getValue() {
        return value;
    }

    @Override
    public int compareTo(Codepoint o) {
        return value < o.value ? -1 : value == o.value ? 0 : 1;
    }

    @Override
    public String toString() {
        return CharUtils.toString(value);
    }

    public char[] toChars() {
        return toString().toCharArray();
    }

    /**
     * Get the number of chars necessary to represent this codepoint. Returns 2 if this is a supplementary codepoint.
     * @return char count
     */
    public int getCharCount() {
        return toChars().length;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + value;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Codepoint other = (Codepoint) obj;
        return value == other.value;
    }

    /**
     * Get the next codepoint.
     * @return next code point
     */
    public Codepoint next() {
        if (value == 0x10ffff) {
            throw new IndexOutOfBoundsException();
        }
        return new Codepoint(value + 1);
    }
}
