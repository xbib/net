package org.xbib.net.oauth.sign;

import org.xbib.net.http.HttpParameters;
import org.xbib.net.http.HttpRequest;
import org.xbib.net.oauth.OAuth;
import org.xbib.net.oauth.OAuthMessageSignerException;

import java.io.UnsupportedEncodingException;
import java.nio.charset.MalformedInputException;
import java.nio.charset.UnmappableCharacterException;
import java.security.GeneralSecurityException;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

@SuppressWarnings("serial")
public class HmacSha256MessageSigner extends OAuthMessageSigner {

    private static final String MAC_NAME = "HmacSHA256";

    @Override
    public String getSignatureMethod() {
        return "HMAC-SHA256";
    }

    @Override
    public String sign(HttpRequest request, HttpParameters requestParams)
            throws OAuthMessageSignerException {
        try {
            String keyString = OAuth.percentEncoder.encode(getConsumerSecret()) + '&'
                    + OAuth.percentEncoder.encode(getTokenSecret());
            byte[] keyBytes = keyString.getBytes(OAuth.ENCODING);
            SecretKey key = new SecretKeySpec(keyBytes, MAC_NAME);
            Mac mac = Mac.getInstance(MAC_NAME);
            mac.init(key);
            String sbs = new SignatureBaseString(request, requestParams).generate();
            byte[] text = sbs.getBytes(OAuth.ENCODING);
            return base64Encode(mac.doFinal(text)).trim();
        } catch (GeneralSecurityException | UnsupportedEncodingException |
                MalformedInputException| UnmappableCharacterException e) {
            throw new OAuthMessageSignerException(e);
        }
    }
}
