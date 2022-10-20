package org.xbib.net.mime;

public final class EndMessage implements MimeEvent {

    static final EndMessage INSTANCE = new EndMessage();

    public EndMessage() {
    }

    @Override
    public Type getEventType() {
        return Type.END_MESSAGE;
    }
}
