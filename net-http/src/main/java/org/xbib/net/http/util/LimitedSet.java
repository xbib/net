package org.xbib.net.http.util;

import java.util.Objects;
import java.util.TreeSet;

/**
 * Limited set.
 * @param <T> type
 */
@SuppressWarnings("serial")
public class LimitedSet<T extends CharSequence> extends TreeSet<T> {

    private final int sizeLimit;

    private final int elementSizeLimit;

    public LimitedSet() {
        this(1024, 65536);
    }

    public LimitedSet(int sizeLimit, int elementSizeLimit) {
        this.sizeLimit = sizeLimit;
        this.elementSizeLimit = elementSizeLimit;
    }

    @Override
    public boolean add(T t) {
        Objects.requireNonNull(t);
        if (size() < sizeLimit && t.length() <= elementSizeLimit) {
            return super.add(t);
        }
        throw new IllegalArgumentException("limit exceeded");
    }
}
