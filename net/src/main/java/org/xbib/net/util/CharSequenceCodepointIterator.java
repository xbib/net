package org.xbib.net.util;

class CharSequenceCodepointIterator extends CodepointIterator {
    private final CharSequence buffer;

    CharSequenceCodepointIterator(CharSequence buffer) {
        this(buffer, 0, buffer.length());
    }

    CharSequenceCodepointIterator(CharSequence buffer, int n, int e) {
        this.buffer = buffer;
        this.position = n;
        this.limit = Math.min(buffer.length() - n, e);
    }

    @Override
    protected char get() {
        return buffer.charAt(position++);
    }

    @Override
    protected char get(int index) {
        return buffer.charAt(index);
    }
}
