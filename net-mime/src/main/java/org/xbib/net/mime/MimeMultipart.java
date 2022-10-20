package org.xbib.net.mime;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Map;

public interface MimeMultipart {

    Map<String, String> getHeaders();

    int getLength();

    ByteBuffer getBody();

    Charset getCharset();
}
