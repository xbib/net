package org.xbib.net;

/**
 *
 */
public class URLSyntaxException extends Exception {

    private static final long serialVersionUID = 1813084470937980392L;

    URLSyntaxException(String message) {
        super(message);
    }

    URLSyntaxException(Throwable cause) {
        super(cause);
    }

}
