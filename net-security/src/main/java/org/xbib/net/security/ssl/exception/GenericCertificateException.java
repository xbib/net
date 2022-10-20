package org.xbib.net.security.ssl.exception;

@SuppressWarnings("serial")
public final class GenericCertificateException extends GenericSecurityException {

    public GenericCertificateException(Throwable cause) {
        super(cause);
    }

    public GenericCertificateException(String message) {
        super(message);
    }

    public GenericCertificateException(String message, Throwable cause) {
        super(message, cause);
    }

}
