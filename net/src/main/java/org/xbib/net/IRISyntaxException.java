package org.xbib.net;

/**
 *
 */
@SuppressWarnings("serial")
public class IRISyntaxException extends RuntimeException {

    IRISyntaxException(String message) {
        super(message);
    }

    IRISyntaxException(Throwable cause) {
        super(cause);
    }

}
