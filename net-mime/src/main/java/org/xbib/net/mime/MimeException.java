package org.xbib.net.mime;

@SuppressWarnings("serial")
public class MimeException extends Exception {

    public MimeException(String message) {
        super(message);
    }

    public MimeException(Throwable throwable) {
        super(throwable);
    }

    public MimeException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
