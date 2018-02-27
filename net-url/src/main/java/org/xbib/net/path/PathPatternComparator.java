package org.xbib.net.path;

import java.util.Comparator;

/**
 * Path pattern comparator.
 */
public class PathPatternComparator implements Comparator<String> {

    private final String path;

    PathPatternComparator(String path) {
        this.path = path;
    }

    @Override
    public int compare(String pattern1, String pattern2) {
        PathPatternInfo info1 = new PathPatternInfo(pattern1);
        PathPatternInfo info2 = new PathPatternInfo(pattern2);
        if (info1.isLeastSpecific() && info2.isLeastSpecific()) {
            return 0;
        } else if (info1.isLeastSpecific()) {
            return 1;
        } else if (info2.isLeastSpecific()) {
            return -1;
        }
        boolean pattern1EqualsPath = pattern1.equals(path);
        boolean pattern2EqualsPath = pattern2.equals(path);
        if (pattern1EqualsPath && pattern2EqualsPath) {
            return 0;
        } else if (pattern1EqualsPath) {
            return -1;
        } else if (pattern2EqualsPath) {
            return 1;
        }
        if (info1.isPrefixPattern() && info2.getDoubleWildcards() == 0) {
            return 1;
        } else if (info2.isPrefixPattern() && info1.getDoubleWildcards() == 0) {
            return -1;
        }
        if (info1.getTotalCount() != info2.getTotalCount()) {
            return info1.getTotalCount() - info2.getTotalCount();
        }
        if (info1.getLength() != info2.getLength()) {
            return info2.getLength() - info1.getLength();
        }
        if (info1.getSingleWildcards() < info2.getSingleWildcards()) {
            return -1;
        } else if (info2.getSingleWildcards() < info1.getSingleWildcards()) {
            return 1;
        }
        if (info1.getUriVars() < info2.getUriVars()) {
            return -1;
        } else if (info2.getUriVars() < info1.getUriVars()) {
            return 1;
        }
        return 0;
    }
}
