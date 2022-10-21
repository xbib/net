package org.xbib.net.util;

/**
 * Filters are used in a variety of ways to filter or verify unicode codepoints.
 */
@FunctionalInterface
public interface CodepointFilter {

    boolean accept(int ch);
}
