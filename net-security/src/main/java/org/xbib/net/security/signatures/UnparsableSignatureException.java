package org.xbib.net.security.signatures;

@SuppressWarnings("serial")
public class UnparsableSignatureException extends AuthenticationException {

    public UnparsableSignatureException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
