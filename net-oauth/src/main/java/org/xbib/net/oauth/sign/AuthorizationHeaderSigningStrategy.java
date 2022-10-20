package org.xbib.net.oauth.sign;

import org.xbib.net.http.HttpParameters;
import org.xbib.net.http.HttpRequest;
import org.xbib.net.oauth.OAuth;

import java.nio.charset.MalformedInputException;
import java.nio.charset.UnmappableCharacterException;
import java.util.Iterator;

/**
 * Writes to the HTTP Authorization header field.
 */
public class AuthorizationHeaderSigningStrategy implements SigningStrategy {

    @Override
    public String writeSignature(String signature, HttpRequest request,
            HttpParameters requestParameters) throws MalformedInputException, UnmappableCharacterException {
        StringBuilder sb = new StringBuilder();
        sb.append("OAuth ");
        // add the realm parameter, if any
        if (requestParameters.containsKey("realm")) {
            sb.append(requestParameters.getAsHeaderElement("realm"));
            sb.append(", ");
        }
        // add all (x_)oauth parameters
        HttpParameters oauthParams = requestParameters.getOAuthParameters();
        oauthParams.put(OAuth.OAUTH_SIGNATURE, signature, true);
        Iterator<String> iterator = oauthParams.keySet().iterator();
        while (iterator.hasNext()) {
            String key = iterator.next();
            sb.append(oauthParams.getAsHeaderElement(key));
            if (iterator.hasNext()) {
                sb.append(", ");
            }
        }
        String header = sb.toString();
        request.setHeader(OAuth.HTTP_AUTHORIZATION_HEADER, header);
        return header;
    }

}
