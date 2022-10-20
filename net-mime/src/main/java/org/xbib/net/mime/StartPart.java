package org.xbib.net.mime;

public final class StartPart implements MimeEvent {

    static final StartPart INSTANCE = new StartPart();

    public StartPart() {
    }

    @Override
    public Type getEventType() {
        return Type.START_PART;
    }
}
