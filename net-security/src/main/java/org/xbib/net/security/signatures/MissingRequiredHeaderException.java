package org.xbib.net.security.signatures;

@SuppressWarnings("serial")
public class MissingRequiredHeaderException extends AuthenticationException {

    public MissingRequiredHeaderException(final String key) {
        super(key);
    }

}
