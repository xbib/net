package org.xbib.net.security.signatures;

@SuppressWarnings("serial")
public class InvalidCreatedFieldException extends AuthenticationException {
    public InvalidCreatedFieldException(final String message) {
        super(message);
    }
}
