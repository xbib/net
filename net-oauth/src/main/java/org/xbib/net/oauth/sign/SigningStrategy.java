package org.xbib.net.oauth.sign;

import org.xbib.net.http.HttpParameters;
import org.xbib.net.http.HttpRequest;

import java.nio.charset.MalformedInputException;
import java.nio.charset.UnmappableCharacterException;

/**
 * <p>
 * Defines how an OAuth signature string is written to a request.
 * </p>
 * <p>
 * Unlike {@link OAuthMessageSigner}, which is concerned with <i>how</i> to
 * generate a signature, this class is concered with <i>where</i> to write it
 * (e.g. HTTP header or query string).
 * </p>
 */
public interface SigningStrategy {

    /**
     * Writes an OAuth signature and all remaining required parameters to an
     * HTTP message.
     * 
     * @param signature
     *        the signature to write
     * @param request
     *        the request to sign
     * @param requestParameters
     *        the request parameters
     * @return whatever has been written to the request, e.g. an Authorization
     *         header field
     */
    String writeSignature(String signature, HttpRequest request, HttpParameters requestParameters)
            throws MalformedInputException, UnmappableCharacterException;
    
}
