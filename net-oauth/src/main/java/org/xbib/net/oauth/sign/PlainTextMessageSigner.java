package org.xbib.net.oauth.sign;

import org.xbib.net.http.HttpParameters;
import org.xbib.net.http.HttpRequest;
import org.xbib.net.oauth.OAuth;
import org.xbib.net.oauth.OAuthMessageSignerException;

import java.nio.charset.MalformedInputException;
import java.nio.charset.UnmappableCharacterException;

@SuppressWarnings("serial")
public class PlainTextMessageSigner extends OAuthMessageSigner {

    @Override
    public String getSignatureMethod() {
        return "PLAINTEXT";
    }

    @Override
    public String sign(HttpRequest request, HttpParameters requestParams)
            throws OAuthMessageSignerException {
        try {
            return OAuth.percentEncoder.encode(getConsumerSecret()) + '&'
                    + OAuth.percentEncoder.encode(getTokenSecret());
        } catch (MalformedInputException | UnmappableCharacterException e) {
            throw new OAuthMessageSignerException(e);
        }
    }
}
