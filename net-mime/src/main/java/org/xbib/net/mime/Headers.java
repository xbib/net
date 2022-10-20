package org.xbib.net.mime;

public final class Headers implements MimeEvent {
    MimeParser.InternetHeaders ih;

    Headers(MimeParser.InternetHeaders ih) {
        this.ih = ih;
    }

    @Override
    public Type getEventType() {
        return Type.HEADERS;
    }

    public MimeParser.InternetHeaders getHeaders() {
        return ih;
    }
}
