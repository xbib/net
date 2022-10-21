package org.xbib.net.util;

class RestrictedCodepointIterator extends DelegatingCodepointIterator {

    private final CodepointFilter filter;
    private final boolean scanningOnly;
    private final boolean notset;

    RestrictedCodepointIterator(CodepointIterator internal, CodepointFilter filter, boolean scanningOnly) {
        this(internal, filter, scanningOnly, false);
    }

    RestrictedCodepointIterator(CodepointIterator internal,
                                CodepointFilter filter,
                                boolean scanningOnly,
                                boolean notset) {
        super(internal);
        this.filter = filter;
        this.scanningOnly = scanningOnly;
        this.notset = notset;
    }

    @Override
    public boolean hasNext() {
        boolean b = super.hasNext();
        if (scanningOnly) {
            try {
                int cp = super.peek(super.position()).getValue();
                if (b && cp != -1 && check(cp)) {
                    return false;
                }
            } catch (InvalidCharacterException e) {
                return false;
            }
        }
        return b;
    }

    @Override
    public Codepoint next() {
        Codepoint cp = super.next();
        int v = cp.getValue();
        if (v != -1 && check(v)) {
            if (scanningOnly) {
                super.position(super.position() - 1);
                return null;
            } else {
                throw new InvalidCharacterException(v);
            }
        }
        return cp;
    }

    private boolean check(int cp) {
        return notset == !filter.accept(cp);
    }

    @Override
    public char[] nextChars() {
        char[] chars = super.nextChars();
        if (chars != null && chars.length > 0) {
            if (chars.length == 1 && check(chars[0])) {
                if (scanningOnly) {
                    super.position(super.position() - 1);
                    return null;
                } else {
                    throw new InvalidCharacterException(chars[0]);
                }
            } else if (chars.length == 2) {
                int cp = CharUtils.toSupplementary(chars[0], chars[1]).getValue();
                if (check(cp)) {
                    if (scanningOnly) {
                        super.position(super.position() - 2);
                        return null;
                    } else {
                        throw new InvalidCharacterException(cp);
                    }
                }
            }
        }
        return chars;
    }
}
