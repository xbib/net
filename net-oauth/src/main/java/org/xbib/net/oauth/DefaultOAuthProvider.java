package org.xbib.net.oauth;

import org.xbib.net.http.HttpRequest;
import org.xbib.net.http.HttpResponse;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * This default implementation uses {@link HttpURLConnection} type GET
 * requests to receive tokens from a service provider.
 */
public class DefaultOAuthProvider extends AbstractOAuthProvider {

    public DefaultOAuthProvider(String requestTokenEndpointUrl, String accessTokenEndpointUrl,
            String authorizationWebsiteUrl) {
        super(requestTokenEndpointUrl, accessTokenEndpointUrl, authorizationWebsiteUrl);
    }

    protected HttpRequest createRequest(String endpointUrl) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(endpointUrl).openConnection();
        connection.setRequestMethod("POST");
        connection.setAllowUserInteraction(false);
        connection.setRequestProperty("Content-Length", "0");
        return new HttpURLConnectionRequestAdapter(connection);
    }

    protected HttpResponse sendRequest(HttpRequest request) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) request.unwrap();
        connection.connect();
        return new HttpURLConnectionResponseAdapter(connection);
    }

    @Override
    protected void closeConnection(HttpRequest request, HttpResponse response) {
        HttpURLConnection connection = (HttpURLConnection) request.unwrap();
        if (connection != null) {
            connection.disconnect();
        }
    }
}
