package org.xbib.net.mime;

public interface Header {

    /**
     * Returns the name of this header.
     *
     * @return name of the header
     */
    String getName();

    /**
     * Returns the value of this header.
     *
     * @return value of the header
     */
    String getValue();
}

