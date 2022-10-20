package org.xbib.net.security.ssl.exception;

@SuppressWarnings("serial")
public final class GenericTrustManagerException extends GenericSecurityException {

    public GenericTrustManagerException(String message) {
        super(message);
    }

    public GenericTrustManagerException(Throwable cause) {
        super(cause);
    }

}
