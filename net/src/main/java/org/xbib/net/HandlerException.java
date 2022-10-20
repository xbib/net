package org.xbib.net;

@SuppressWarnings("serial")
public class HandlerException extends RuntimeException {

    public HandlerException() {
        super();
    }

    public HandlerException(String message) {
        super(message);
    }

    public HandlerException(Exception e) {
        super(e);
    }

    public HandlerException(String message, Exception e) {
        super(message, e);
    }
}
