package org.xbib.net.util;

@SuppressWarnings("serial")
public class InvalidCharacterException extends RuntimeException {

    private final int input;

    public InvalidCharacterException(int input) {
        this.input = input;
    }

    @Override
    public String getMessage() {
        return "Invalid Character 0x" + Integer.toHexString(input) + "(" + (char) input + ")";
    }

}
