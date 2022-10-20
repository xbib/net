package org.xbib.net.mime.test;

import org.junit.jupiter.api.Test;
import org.xbib.net.mime.MimeException;
import org.xbib.net.mime.MimeMultipartParser;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MimeMultipartTest {

    private static final Logger logger = Logger.getLogger(MimeMultipartTest.class.getName());

    @Test
    public void multiPartTest() throws MimeException, IOException {
        InputStream inputStream = getClass().getResourceAsStream("/org/xbib/net/mime/test/msg.txt");
        Objects.requireNonNull(inputStream);
        MimeMultipartParser parser = new MimeMultipartParser("multipart/mixed; boundary=\"----=_Part_4_910054940.1065629194743\"; charset=\"ISO-8859-1\"");
        parser.parse(ByteBuffer.wrap(inputStream.readAllBytes()),
                e -> logger.log(Level.INFO, e.getHeaders().toString() + " length = " + e.getLength() + " content = " + e.getCharset().decode(e.getBody())));
    }
}
