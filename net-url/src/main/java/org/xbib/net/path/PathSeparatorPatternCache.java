package org.xbib.net.path;

/**
 */
class PathSeparatorPatternCache {

    private final String endsOnWildCard;

    private final String endsOnDoubleWildCard;

    PathSeparatorPatternCache(String pathSeparator) {
        this.endsOnWildCard = pathSeparator + "*";
        this.endsOnDoubleWildCard = pathSeparator + "**";
    }

    String getEndsOnWildCard() {
        return endsOnWildCard;
    }

    String getEndsOnDoubleWildCard() {
        return endsOnDoubleWildCard;
    }
}
