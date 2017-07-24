package org.xbib.net;

/**
 *
 */
public class IRISyntaxException extends RuntimeException {

    private static final long serialVersionUID = 1813084470937980392L;

    IRISyntaxException(String message) {
        super(message);
    }

    IRISyntaxException(Throwable cause) {
        super(cause);
    }

}
