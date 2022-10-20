package org.xbib.net.security.ssl.exception;

@SuppressWarnings("serial")
public final class GenericIOException extends GenericSecurityException {

    public GenericIOException(Throwable cause) {
        super(cause);
    }

    public GenericIOException(String message, Throwable cause) {
        super(message, cause);
    }

}
