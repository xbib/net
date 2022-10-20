package org.xbib.net.mime;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents MIME message. MIME message parsing is done lazily using a
 * pull parser.
 */
public class MimeMessage implements Closeable {

    private static final Logger LOGGER = Logger.getLogger(MimeMessage.class.getName());

    private final InputStream in;
    private final Iterator<MimeEvent> it;
    private boolean parsed;
    private MimePart currentPart;
    private int currentIndex;

    private final List<MimePart> partsList = new ArrayList<>();
    private final Map<String, MimePart> partsMap = new HashMap<>();

    /**
     * Creates a MIME message from the content's stream. The content stream
     * is closed when EOF is reached.
     *
     * @param in       MIME message stream
     * @param boundary the separator for parts(pass it without --)
     */
    public MimeMessage(InputStream in, String boundary) {
        this.in = in;
        MimeParser parser = new MimeParser(in, boundary);
        it = parser.iterator();
        //if (config.isParseEagerly()) {
        //    parseAll();
        //}
    }

    /**
     * Gets all the attachments by parsing the entire MIME message. Avoid
     * this if possible since it is an expensive operation.
     *
     * @return list of attachments.
     */
    public List<MimePart> getAttachments() throws MimeException, IOException {
        if (!parsed) {
            parseAll();
        }
        return partsList;
    }

    /**
     * Creates nth attachment lazily. It doesn't validate
     * if the message has so many attachments. To
     * do the validation, the message needs to be parsed.
     * The parsing of the message is done lazily and is done
     * while reading the bytes of the part.
     *
     * @param index sequential order of the part. starts with zero.
     * @return attachemnt part
     */
    public MimePart getPart(int index) throws MimeException {
        LOGGER.log(Level.FINE, "index={0}", index);
        MimePart part = (index < partsList.size()) ? partsList.get(index) : null;
        if (parsed && part == null) {
            throw new MimeException("There is no " + index + " attachment part ");
        }
        if (part == null) {
            // Parsing will done lazily and will be driven by reading the part
            part = new MimePart(this);
            partsList.add(index, part);
        }
        LOGGER.log(Level.FINE, "Got attachment at index=" + index + " attachment=" + part);
        return part;
    }

    /**
     * Creates a lazy attachment for a given Content-ID. It doesn't validate
     * if the message contains an attachment with the given Content-ID. To
     * do the validation, the message needs to be parsed. The parsing of the
     * message is done lazily and is done while reading the bytes of the part.
     *
     * @param contentId Content-ID of the part, expects Content-ID without {@code <, >}
     * @return attachemnt part
     */
    public MimePart getPart(String contentId) throws MimeException {
        LOGGER.log(Level.FINE, "Content-ID = " + contentId);
        MimePart part = getDecodedCidPart(contentId);
        if (parsed && part == null) {
            throw new MimeException("There is no part with Content-ID = " + contentId);
        }
        if (part == null) {
            // Parsing is done lazily and is driven by reading the part
            part = new MimePart(this, contentId);
            partsMap.put(contentId, part);
        }
        LOGGER.log(Level.FINE, "got part for Content-ID = " + contentId + " part = " + part);
        return part;
    }

    // this is required for Indigo interop, it writes content-id without escaping
    private MimePart getDecodedCidPart(String cid) {
        MimePart part = partsMap.get(cid);
        if (part == null) {
            if (cid.indexOf('%') != -1) {
                // TODO do not use URLDecoder
                String tempCid = URLDecoder.decode(cid, StandardCharsets.UTF_8);
                part = partsMap.get(tempCid);
            }
        }
        return part;
    }

    /**
     * Parses the whole MIME message eagerly
     */
    public final void parseAll() throws MimeException, IOException {
        while (makeProgress()) {
            // Nothing to do
        }
    }

    /**
     * Closes all parsed {@link MimePart parts} and cleans up
     * any resources that are held by this {@code MimeMessage} (for e.g. deletes temp files).
     * This method is safe to call even if parsing of message failed.
     */
    @Override
    public void close() throws IOException {
        close(partsList);
        close(partsMap.values());
    }

    private void close(final Collection<MimePart> parts) throws IOException {
        for (final MimePart part : parts) {
                part.close();
        }
    }

    /**
     * Parses the MIME message in a pull fashion.
     *
     * @return false if the parsing is completed.
     */
    public synchronized boolean makeProgress() throws MimeException, IOException {
        if (!it.hasNext()) {
            return false;
        }
        MimeEvent event = it.next();
        switch (event.getEventType()) {
            case START_MESSAGE:
                LOGGER.log(Level.FINE, "MIMEEvent=" + MimeEvent.Type.START_MESSAGE);
                break;

            case START_PART:
                LOGGER.log(Level.FINE, "MIMEEvent=" + MimeEvent.Type.START_PART);
                break;

            case HEADERS:
                LOGGER.log(Level.FINE, "MIMEEvent=" + MimeEvent.Type.HEADERS);
                Headers headers = (Headers) event;
                MimeParser.InternetHeaders ih = headers.getHeaders();
                List<String> cids = ih.getHeader("content-id");
                String cid = (cids != null) ? cids.get(0) : currentIndex + "";
                if (cid.length() > 2 && cid.charAt(0) == '<') {
                    cid = cid.substring(1, cid.length() - 1);
                }
                MimePart listPart = (currentIndex < partsList.size()) ? partsList.get(currentIndex) : null;
                MimePart mapPart = getDecodedCidPart(cid);
                if (listPart == null && mapPart == null) {
                    currentPart = getPart(cid);
                    partsList.add(currentIndex, currentPart);
                } else if (listPart == null) {
                    currentPart = mapPart;
                    partsList.add(currentIndex, mapPart);
                } else if (mapPart == null) {
                    currentPart = listPart;
                    currentPart.setContentId(cid);
                    partsMap.put(cid, currentPart);
                } else if (listPart != mapPart) {
                    throw new MimeException("ereated two different attachments using Content-ID and index");
                }
                currentPart.setHeaders(ih);
                break;

            case CONTENT:
                LOGGER.log(Level.FINER, "MIMEEvent=" + MimeEvent.Type.CONTENT);
                Content content = (Content) event;
                ByteBuffer buf = content.getData();
                currentPart.addBody(buf);
                break;

            case END_PART:
                LOGGER.log(Level.FINE, "MIMEEvent=" + MimeEvent.Type.END_PART);
                currentPart.doneParsing();
                ++currentIndex;
                break;

            case END_MESSAGE:
                LOGGER.log(Level.FINE, "MIMEEvent=" + MimeEvent.Type.END_MESSAGE);
                parsed = true;
                try {
                    in.close();
                } catch (IOException ioe) {
                    throw new MimeException(ioe);
                }
                break;
        }
        return true;
    }
}
