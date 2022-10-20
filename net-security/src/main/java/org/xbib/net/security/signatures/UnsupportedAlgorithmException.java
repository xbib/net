package org.xbib.net.security.signatures;

@SuppressWarnings("serial")
public class UnsupportedAlgorithmException extends AuthenticationException {

    public UnsupportedAlgorithmException(final String message) {
        super(message);
    }

    public UnsupportedAlgorithmException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
