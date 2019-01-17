package org.xbib.net.http;

import java.io.IOException;
import java.io.InputStream;

public interface HttpResponse {

    int getStatusCode() throws IOException;

    String getReasonPhrase() throws Exception;

    InputStream getContent() throws IOException;

    Object unwrap();
}
