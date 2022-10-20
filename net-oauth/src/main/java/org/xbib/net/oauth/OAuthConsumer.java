package org.xbib.net.oauth;

import org.xbib.net.http.HttpParameters;
import org.xbib.net.http.HttpRequest;
import org.xbib.net.oauth.sign.OAuthMessageSigner;
import org.xbib.net.oauth.sign.SigningStrategy;

/**
 * <p>
 * Exposes a simple interface to sign HTTP requests using a given OAuth token
 * and secret. Refer to {@link OAuthProvider} how to retrieve a valid token and
 * token secret.
 * </p>
 * HTTP messages are signed as follows:
 *
 * <pre>
 * // exchange the arguments with the actual token/secret pair
 * OAuthConsumer consumer = new DefaultOAuthConsumer(&quot;1234&quot;, &quot;5678&quot;);
 * URL url = new URL(&quot;http://example.com/protected.xml&quot;);
 * HttpURLConnection request = (HttpURLConnection) url.openConnection();
 * consumer.sign(request);
 * request.connect();
 * </pre>
 * 
 */
public interface OAuthConsumer {

    /**
     * Sets the message signer that should be used to generate the OAuth
     * signature.
     * 
     * @param messageSigner
     *        the signer
     */
    void setMessageSigner(OAuthMessageSigner messageSigner);

    /**
     * Allows you to add parameters (typically OAuth parameters such as
     * oauth_callback or oauth_verifier) which will go directly into the signer,
     * i.e. you don't have to put them into the request first. The consumer's
     * signing strategy will then take care of writing them to the
     * correct part of the request before it is sent. This is useful if you want
     * to pre-set custom OAuth parameters. Note that these parameters are
     * expected to already be percent encoded -- they will be simply merged
     * as-is. <b>BE CAREFUL WITH THIS METHOD! Your service provider may decide
     * to ignore any non-standard OAuth params when computing the signature.</b>
     * 
     * @param additionalParameters
     *        the parameters
     */
    void setAdditionalParameters(HttpParameters additionalParameters);

    /**
     * Defines which strategy should be used to write a signature to an HTTP
     * request.
     * 
     * @param signingStrategy
     *        the strategy
     */
    void setSigningStrategy(SigningStrategy signingStrategy);

    /**
     * <p>
     * Causes the consumer to always include the oauth_token parameter to be
     * sent, even if blank. If you're seeing 401s during calls to
     * {@link OAuthProvider#retrieveRequestToken}, try setting this to true.
     * </p>
     * 
     * @param enable
     *        true or false
     */
    void setSendEmptyTokens(boolean enable);

    /**
     * Signs the given HTTP request by writing an OAuth signature (and other
     * required OAuth parameters) to it. Where these parameters are written
     * depends on the current {@link SigningStrategy}.
     * 
     * @param request
     *        the request to sign
     * @return the request object passed as an argument
     * @throws OAuthMessageSignerException
     * @throws OAuthExpectationFailedException
     * @throws OAuthCommunicationException
     */
    HttpRequest sign(HttpRequest request) throws OAuthMessageSignerException,
            OAuthExpectationFailedException, OAuthCommunicationException;

    /**
     * <p>
     * Signs the given HTTP request by writing an OAuth signature (and other
     * required OAuth parameters) to it. Where these parameters are written
     * depends on the current {@link SigningStrategy}.
     * </p>
     * This method accepts HTTP library specific request objects; the consumer
     * implementation must ensure that only those request types are passed which
     * it supports.
     * 
     * @param request
     *        the request to sign
     * @return the request object passed as an argument
     * @throws OAuthMessageSignerException
     * @throws OAuthExpectationFailedException
     * @throws OAuthCommunicationException
     */
    HttpRequest sign(Object request) throws OAuthMessageSignerException,
            OAuthExpectationFailedException, OAuthCommunicationException;

    /**
     * "Signs" the given URL by appending all OAuth parameters to it which are
     * required for message signing. The assumed HTTP method is GET.
     * Essentially, this is equivalent to signing an HTTP GET request, but it
     * can be useful if your application requires clickable links to protected
     * resources, i.e. when your application does not have access to the actual
     * request that is being sent.
     *
     * @param url
     *        the input URL. May have query parameters.
     * @return the input URL, with all necessary OAuth parameters attached as a
     *         query string. Existing query parameters are preserved.
     * @throws OAuthMessageSignerException
     * @throws OAuthExpectationFailedException
     * @throws OAuthCommunicationException
     */
    String sign(String url) throws OAuthMessageSignerException,
            OAuthExpectationFailedException, OAuthCommunicationException;

    /**
     * Sets the OAuth token and token secret used for message signing.
     * 
     * @param token
     *        the token
     * @param tokenSecret
     *        the token secret
     */
    void setTokenWithSecret(String token, String tokenSecret);

    String getToken();

    String getTokenSecret();

    String getConsumerKey();

    String getConsumerSecret();

    /**
     * Returns all parameters collected from the HTTP request during message
     * signing (this means the return value may be NULL before a call to
     * {@link #sign}), plus all required OAuth parameters that were added
     * because the request didn't contain them beforehand. In other words, this
     * is the exact set of parameters that were used for creating the message
     * signature.
     * 
     * @return the request parameters used for message signing
     */
    HttpParameters getRequestParameters();
}
