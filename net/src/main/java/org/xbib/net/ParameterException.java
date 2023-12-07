package org.xbib.net;

@SuppressWarnings("serial")
public class ParameterException extends Exception {

    public ParameterException(Exception e) {
        super(e);
    }

    public ParameterException(String message) {
        super(message);
    }
}
