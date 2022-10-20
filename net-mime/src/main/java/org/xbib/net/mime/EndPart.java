package org.xbib.net.mime;

public final class EndPart implements MimeEvent {

    public static final EndPart INSTANCE = new EndPart();

    public EndPart() {
    }

    @Override
    public Type getEventType() {
        return Type.END_PART;
    }
}
