package org.xbib.net.path.simple;

import java.util.Comparator;

/**
 * Path pattern comparator.
 */
@SuppressWarnings("serial")
public class PathComparator implements Comparator<Path> {

    private final String path;

    public PathComparator(String path) {
        this.path = path;
    }

    @Override
    public int compare(Path path1, Path path2) {
        if (path1 == null) {
            if (path2 == null) {
                return 0;
            } else {
                return 1;
            }
        }
        if (path2 == null) {
            return -1;
        }
        if (path1.isLeastSpecific() && path2.isLeastSpecific()) {
            return 0;
        } else if (path1.isLeastSpecific()) {
            return 1;
        } else if (path2.isLeastSpecific()) {
            return -1;
        }
        boolean pattern1EqualsPath = path1.getPattern().equals(path);
        boolean pattern2EqualsPath = path2.getPattern().equals(path);
        if (pattern1EqualsPath && pattern2EqualsPath) {
            return 0;
        } else if (pattern1EqualsPath) {
            return -1;
        } else if (pattern2EqualsPath) {
            return 1;
        }
        if (path1.isPrefixPattern() && path2.getDoubleWildcards() == 0) {
            return 1;
        } else if (path2.isPrefixPattern() && path1.getDoubleWildcards() == 0) {
            return -1;
        }
        if (path1.getTotalCount() != path2.getTotalCount()) {
            return path1.getTotalCount() - path2.getTotalCount();
        }
        if (path1.getLength() != path2.getLength()) {
            return path2.getLength() - path1.getLength();
        }
        if (path1.getSingleWildcards() < path2.getSingleWildcards()) {
            return -1;
        } else if (path2.getSingleWildcards() < path1.getSingleWildcards()) {
            return 1;
        }
        if (path1.getUriVars() < path2.getUriVars()) {
            return -1;
        } else if (path2.getUriVars() < path1.getUriVars()) {
            return 1;
        }
        return 0;
    }
}
