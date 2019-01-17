package org.xbib.net.oauth.sign;

import org.xbib.net.http.HttpParameters;
import org.xbib.net.http.HttpRequest;
import org.xbib.net.oauth.OAuth;

import java.nio.charset.MalformedInputException;
import java.nio.charset.UnmappableCharacterException;
import java.util.Iterator;

/**
 * Writes to a URL query string. <strong>Note that this currently ONLY works
 * when signing a URL directly, not with HTTP request objects.</strong> That's
 * because most HTTP request implementations do not allow the client to change
 * the URL once the request has been instantiated, so there is no way to append
 * parameters to it.
 */
public class QueryStringSigningStrategy implements SigningStrategy {

    @Override
    public String writeSignature(String signature, HttpRequest request,
            HttpParameters requestParameters)
            throws MalformedInputException, UnmappableCharacterException {
        // add all (x_)oauth parameters
        HttpParameters oauthParams = requestParameters.getOAuthParameters();
        oauthParams.put(OAuth.OAUTH_SIGNATURE, signature, true);
        Iterator<String> iterator = oauthParams.keySet().iterator();
        // add the first query parameter (we always have at least the signature)
        String firstKey = iterator.next();
        StringBuilder sb = new StringBuilder(OAuth.addQueryString(request.getRequestUrl(),
            oauthParams.getAsQueryString(firstKey)));
        while (iterator.hasNext()) {
            sb.append("&");
            String key = iterator.next();
            sb.append(oauthParams.getAsQueryString(key));
        }
        String signedUrl = sb.toString();
        request.setRequestUrl(signedUrl);
        return signedUrl;
    }
}
