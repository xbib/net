package org.xbib.net.mime;

public final class StartMessage implements MimeEvent {

    static final StartMessage INSTANCE = new StartMessage();

    public StartMessage() {
    }

    @Override
    public Type getEventType() {
        return Type.START_MESSAGE;
    }
}
