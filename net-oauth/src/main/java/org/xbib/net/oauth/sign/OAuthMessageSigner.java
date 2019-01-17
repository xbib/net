package org.xbib.net.oauth.sign;

import org.xbib.net.http.HttpParameters;
import org.xbib.net.http.HttpRequest;
import org.xbib.net.oauth.OAuthMessageSignerException;

import java.util.Base64;


public abstract class OAuthMessageSigner {

    private Base64.Encoder base64Encoder;

    private Base64.Decoder base64Decoder;

    private String consumerSecret;

    private String tokenSecret;

    public OAuthMessageSigner() {
        this.base64Encoder = Base64.getEncoder();
        this.base64Decoder = Base64.getDecoder();
    }

    public abstract String sign(HttpRequest request, HttpParameters requestParameters)
            throws OAuthMessageSignerException;

    public abstract String getSignatureMethod();

    public String getConsumerSecret() {
        return consumerSecret;
    }

    public String getTokenSecret() {
        return tokenSecret;
    }

    public void setConsumerSecret(String consumerSecret) {
        this.consumerSecret = consumerSecret;
    }

    public void setTokenSecret(String tokenSecret) {
        this.tokenSecret = tokenSecret;
    }

    protected byte[] decodeBase64(String s) {
        return base64Decoder.decode(s.getBytes());
    }

    protected String base64Encode(byte[] b) {
        return new String(base64Encoder.encode(b));
    }

}
