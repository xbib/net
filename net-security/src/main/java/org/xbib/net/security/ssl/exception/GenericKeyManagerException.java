package org.xbib.net.security.ssl.exception;

@SuppressWarnings("serial")
public final class GenericKeyManagerException extends GenericSecurityException {

    public GenericKeyManagerException(String message) {
        super(message);
    }

    public GenericKeyManagerException(Throwable cause) {
        super(cause);
    }

}
