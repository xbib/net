package org.xbib.net.http.util;

import java.util.SortedSet;
import java.util.TreeSet;

public class LimitedSortedStringSet extends TreeSet<String> implements SortedSet<String> {

    private final int sizeLimit;

    private final int elementSizeLimit;

    public LimitedSortedStringSet() {
        this(1024, 65536);
    }

    public LimitedSortedStringSet(int sizeLimit, int elementSizeLimit) {
        this.sizeLimit = sizeLimit;
        this.elementSizeLimit = elementSizeLimit;
    }

    @Override
    public boolean add(String string) {
        if (size() < sizeLimit && string.length() <= elementSizeLimit ) {
            return super.add(string);
        }
        return false;
    }
}
