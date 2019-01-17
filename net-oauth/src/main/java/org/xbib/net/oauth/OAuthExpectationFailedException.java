package org.xbib.net.oauth;

@SuppressWarnings("serial")
public class OAuthExpectationFailedException extends OAuthException {

    public OAuthExpectationFailedException(String message) {
        super(message);
    }
}
