package org.xbib.net.mime;

public interface MimeEvent {

    /**
     * Returns a event for parser's current cursor location in the MIME message.
     *
     * {@link Type#START_MESSAGE} and {@link Type#START_MESSAGE} events
     * are generated only once.
     *
     * {@link Type#START_PART}, {@link Type#END_PART}, {@link Type#HEADERS}
     * events are generated only once for each attachment part.
     *
     * {@link Type#CONTENT} event may be generated more than once for an attachment
     * part.
     *
     * @return event type
     */
    Type getEventType();

    enum Type {
        START_MESSAGE, START_PART, HEADERS, CONTENT, END_PART, END_MESSAGE
    }
}
