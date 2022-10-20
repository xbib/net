package org.xbib.net.mime;

import java.nio.ByteBuffer;

public final class Content implements MimeEvent {

    private final ByteBuffer buf;

    Content(ByteBuffer buf) {
        this.buf = buf;
    }

    @Override
    public Type getEventType() {
        return Type.CONTENT;
    }

    public ByteBuffer getData() {
        return buf;
    }
}
