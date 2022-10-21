package org.xbib.net.util;

class CharArrayCodepointIterator extends CodepointIterator {
    protected char[] buffer;

    CharArrayCodepointIterator(char[] buffer) {
        this(buffer, 0, buffer.length);
    }

    CharArrayCodepointIterator(char[] buffer, int n, int e) {
        this.buffer = buffer;
        this.position = n;
        this.limit = Math.min(buffer.length - n, e);
    }

    @Override
    protected char get() {
        return (position < limit) ? buffer[position++] : (char) -1;
    }

    @Override
    protected char get(int index) {
        if (index < 0 || index >= limit) {
            throw new ArrayIndexOutOfBoundsException(index);
        }
        return buffer[index];
    }
}
