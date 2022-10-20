package org.xbib.net.mime;

@FunctionalInterface
public interface MimeMultipartHandler {

    void handle(MimeMultipart multipart);
}
