package org.xbib.net.http;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * A representation of an HTTP request. Contains methods to access all
 * those parts of an HTTP request.
 */
public interface HttpRequest {

    String getMethod();

    String getRequestUrl();

    void setRequestUrl(String url);

    void setHeader(String name, String value);

    String getHeader(String name);

    Map<String, String> getAllHeaders();

    InputStream getMessagePayload() throws IOException;

    String getContentType();

    Object unwrap();
}
