package org.xbib.net.mime.test;

import org.junit.jupiter.api.Test;
import org.xbib.net.mime.MimeException;
import org.xbib.net.mime.MimeMessage;
import org.xbib.net.mime.MimePart;

import java.io.ByteArrayInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class ParsingTest {

    @Test
    public void testParser() throws Exception {
        InputStream in = getClass().getResourceAsStream("msg.txt");
        if (in == null) {
            fail("no msg.txt");
        }
        String boundary = "----=_Part_4_910054940.1065629194743";
        MimeMessage mm = new MimeMessage(in, boundary);
        mm.parseAll();
        List<MimePart> parts = mm.getAttachments();
        assertEquals(2, parts.size());
        assertEquals("139912840220.1065629194743.IBM.WEBSERVICES@ibm-7pr28r4m35k", parts.get(0).getContentId());
        assertEquals("1351327060508.1065629194423.IBM.WEBSERVICES@ibm-7pr28r4m35k", parts.get(1).getContentId());
        {
            byte[] buf = new byte[8192];
            InputStream part0 = parts.get(0).read();
            int len = part0.read(buf, 0, buf.length);
            String str = new String(buf, 0, len);
            assertTrue(str.startsWith("<soapenv:Envelope"));
            assertTrue(str.endsWith("</soapenv:Envelope>"));
            part0.close();
        }
        {
            InputStream part1 = parts.get(1).read();
            assertEquals((byte) part1.read(), (byte) 0xff);
            assertEquals((byte) part1.read(), (byte) 0xd8);
            part1.close();
        }
    }

    @Test
    public void testMsg2() throws Exception {
        InputStream in = getClass().getResourceAsStream("msg2.txt");
        String boundary = "----=_Part_1_807283631.1066069460327";
        MimeMessage mm = new MimeMessage(in, boundary);
        mm.parseAll();
        List<MimePart> parts = mm.getAttachments();
        assertEquals(2, parts.size());
        assertEquals("1071294019496.1066069460327.IBM.WEBSERVICES@ibm-7pr28r4m35k", parts.get(0).getContentId());
        assertEquals("871169419176.1066069460266.IBM.WEBSERVICES@ibm-7pr28r4m35k", parts.get(1).getContentId());
    }

    @Test
    public void testMessage1() throws Exception {
        InputStream in = getClass().getResourceAsStream("message1.txt");
        String boundary = "----=_Part_7_10584188.1123489648993";
        MimeMessage mm = new MimeMessage(in, boundary);
        mm.parseAll();
        List<MimePart> parts = mm.getAttachments();
        assertEquals(2, parts.size());
        assertEquals("soapPart", parts.get(0).getContentId());
        assertEquals("attachmentPart", parts.get(1).getContentId());
        {
            byte[] buf = new byte[18];
            InputStream part0 = parts.get(0).read();
            int len = part0.read(buf, 0, buf.length);
            String str = new String(buf, 0, len);
            assertTrue(str.startsWith("<SOAP-ENV:Envelope"));
            assertEquals(' ', (byte) part0.read());
            buf = new byte[8192];
            len = part0.read(buf, 0, buf.length);
            str = new String(buf, 0, len);
            assertTrue(str.endsWith("</SOAP-ENV:Envelope>"));
            part0.close();
        }
        {
            byte[] buf = new byte[8192];
            InputStream part1 = parts.get(1).read();
            int len = part1.read(buf, 0, buf.length);
            String str = new String(buf, 0, len);
            assertTrue(str.startsWith("<?xml version"));
            assertTrue(str.endsWith("</Envelope>\n"));
            part1.close();
        }
    }

    @Test
    public void testReadEOF() throws Exception {
        InputStream in = getClass().getResourceAsStream("message1.txt");
        String boundary = "----=_Part_7_10584188.1123489648993";
        MimeMessage mm = new MimeMessage(in, boundary);
        mm.parseAll();
        List<MimePart> parts = mm.getAttachments();
        for(MimePart part : parts) {
            testInputStream(part.read());
            testInputStream(part.readOnce());
        }
    }

    @SuppressWarnings("empty-statement")
    private void testInputStream(InputStream is) throws IOException {
        while (is.read() != -1);
        assertEquals(-1, is.read());
        is.close();
        try {
            int i = is.read();
            fail("read() after close() should throw IOException");
        } catch (IOException ioe) {
            // expected exception
        }
    }

    @Test
    public void testEmptyPart() throws Exception {
        InputStream in = getClass().getResourceAsStream("/org/xbib/net/mime/test/emptypart.txt");
        String boundary = "----=_Part_7_10584188.1123489648993";
        MimeMessage mm = new MimeMessage(in, boundary);
        mm.parseAll();
        List<MimePart> parts = mm.getAttachments();
        assertEquals(2, parts.size());
        assertEquals("soapPart", parts.get(0).getContentId());
        assertEquals("attachmentPart", parts.get(1).getContentId());
        {
            try (InputStream is = parts.get(0).read()) {
                while (is.read() != -1) {
                    fail("There should be any bytes since this is empty part");
                }
            }
        }
        {
            byte[] buf = new byte[8192];
            InputStream part1 = parts.get(1).read();
            int len = part1.read(buf, 0, buf.length);
            String str = new  String(buf, 0, len);
            assertTrue(str.startsWith("<?xml version"));
            assertTrue(str.endsWith("</Envelope>\n"));
            part1.close();
        }
    }

    @Test
    public void testNoHeaders() throws Exception {
        InputStream in = getClass().getResourceAsStream("noheaders.txt");
        String boundary = "----=_Part_7_10584188.1123489648993";
        MimeMessage mm = new MimeMessage(in, boundary);
        mm.parseAll();
        List<MimePart> parts = mm.getAttachments();
        assertEquals(2, parts.size());
        assertEquals("0", parts.get(0).getContentId());
        assertEquals("1", parts.get(1).getContentId());
    }

    @Test
    public void testOneByte() throws Exception {
        InputStream in = getClass().getResourceAsStream("onebyte.txt");
        String boundary = "boundary";
        MimeMessage mm = new MimeMessage(in, boundary);
        mm.parseAll();
        List<MimePart> parts = mm.getAttachments();
        assertEquals(2, parts.size());
        assertEquals("0", parts.get(0).getContentId());
        assertEquals("1", parts.get(1).getContentId());
    }

    @Test
    public void testBoundaryWhiteSpace() throws Exception {
        InputStream in = getClass().getResourceAsStream("boundary-lwsp.txt");
        String boundary = "boundary";
        MimeMessage mm = new MimeMessage(in, boundary);
        mm.parseAll();
        List<MimePart> parts = mm.getAttachments();
        assertEquals(2, parts.size());
        assertEquals("part1", parts.get(0).getContentId());
        assertEquals("part2", parts.get(1).getContentId());
    }

    @Test
    public void testBoundaryInBody() throws Exception {
        InputStream in = getClass().getResourceAsStream("boundary-in-body.txt");
        String boundary = "boundary";
        MimeMessage mm = new MimeMessage(in, boundary);
        mm.parseAll();
        List<MimePart> parts = mm.getAttachments();
        assertEquals(2, parts.size());
        assertEquals("part1", parts.get(0).getContentId());
        assertEquals("part2", parts.get(1).getContentId());
    }

    @Test
    public void testNoClosingBoundary() throws Exception {
        boolean gotException = false;
        try {
            String fileName = "msg-no-closing-boundary.txt";
            InputStream in = getClass().getResourceAsStream(fileName);
            assertNotNull(in,"Failed to load test data from " + fileName);
            String boundary = "----=_Part_4_910054940.1065629194743";
            MimeMessage mm = new MimeMessage(in, boundary);
            mm.parseAll();
        } catch (MimeException | NoSuchElementException e) {
            gotException = true;
            String msg = e.getMessage();
            assertNotNull(msg);
            assertTrue(msg.contains("no closing MIME boundary"));
        }
        assertTrue(gotException);
    }

    @Test
    public void testInvalidClosingBoundary() throws Exception {
        boolean gotException = false;
        try {
            String fileName = "msg-invalid-closing-boundary.txt";
            InputStream in = getClass().getResourceAsStream(fileName);
            assertNotNull(in, "Failed to load test data from " + fileName);
            String boundary = "----=_Part_4_910054940.1065629194743";
            MimeMessage mm = new MimeMessage(in, boundary);
            mm.parseAll();
        } catch (MimeException | NoSuchElementException e) {
            gotException = true;
            String msg = e.getMessage();
            assertNotNull(msg);
            assertTrue(msg.contains("no closing MIME boundary"));
        }
        assertTrue(gotException);
    }

    @Test
    public void testInvalidMimeMessage() throws Exception {
        String invalidMessage = "--boundary\nContent-Id: part1\n\n1";
        String boundary = "boundary";
        MimeMessage message = new MimeMessage(new ByteArrayInputStream(invalidMessage.getBytes()), boundary);
        try {
            message.getAttachments();
            fail("Given message is un-parseable. An exception should have been raised");
        } catch (MimeException | NoSuchElementException e) {
            MimePart part = message.getPart(0);
            assertFalse(part.isClosed(), "Part should not be closed at this point");
            message.close();
            assertTrue(part.isClosed(), "Part should be closed by now");
        }
    }
}
