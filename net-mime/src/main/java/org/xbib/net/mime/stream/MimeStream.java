package org.xbib.net.mime.stream;

import org.xbib.net.mime.MimeException;

import java.io.InputStream;

public class MimeStream {

    private MimeStream() {
    }

    public static InputStream decode(InputStream inputStream, String encoding) throws MimeException {
        if (encoding.equalsIgnoreCase("base64"))
            return new Base64DecoderStream(inputStream);
        else if (encoding.equalsIgnoreCase("quoted-printable"))
            return new QuotedPrintableInputStream(inputStream);
        else if (encoding.equalsIgnoreCase("binary") ||
                encoding.equalsIgnoreCase("7bit") ||
                encoding.equalsIgnoreCase("8bit"))
            return inputStream;
        else {
            throw new MimeException("Unknown encoding: " + encoding);
        }
    }
}
