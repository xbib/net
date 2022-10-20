package org.xbib.net.security.signatures;

@SuppressWarnings("serial")
public class InvalidExpiresFieldException extends AuthenticationException {
    public InvalidExpiresFieldException(final String message) {
        super(message);
    }
}
