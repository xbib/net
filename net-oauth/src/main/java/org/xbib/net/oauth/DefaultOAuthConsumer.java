package org.xbib.net.oauth;

import org.xbib.net.http.HttpRequest;

import java.net.HttpURLConnection;

/**
 * The default implementation for an OAuth consumer. Only supports signing
 * {@link HttpURLConnection} type requests.
 */
public class DefaultOAuthConsumer extends AbstractOAuthConsumer {

    public DefaultOAuthConsumer(String consumerKey, String consumerSecret) {
        super(consumerKey, consumerSecret);
    }

    @Override
    protected HttpRequest wrap(Object request) {
        if (!(request instanceof HttpURLConnection)) {
            throw new IllegalArgumentException(
                    "The default consumer expects requests of type java.net.HttpURLConnection");
        }
        return new HttpURLConnectionRequestAdapter((HttpURLConnection) request);
    }

}
