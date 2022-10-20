package org.xbib.net.mime.test;

import org.junit.jupiter.api.Test;
import org.xbib.net.mime.stream.QuotedPrintableInputStream;
import org.xbib.net.mime.stream.QuotedPrintableOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class QuotedPrintableTest {
    @Test
    public void testUTF8RoundTrip() throws Exception {
        char[] RUSSIAN_STUFF_UNICODE = {
                0x412, 0x441, 0x435, 0x43C, 0x5F, 0x43F, 0x440, 0x438, 0x432, 0x435, 0x442
        };
        String stringInput = new String(RUSSIAN_STUFF_UNICODE);
        byte[] byteInput = stringInput.getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        QuotedPrintableOutputStream outputStream = new QuotedPrintableOutputStream(byteArrayOutputStream);
        outputStream.write(byteInput);
        String encoded = byteArrayOutputStream.toString(StandardCharsets.UTF_8);
        assertEquals("=D0=92=D1=81=D0=B5=D0=BC_=D0=BF=D1=80=D0=B8=D0=B2=D0=B5=D1=82", encoded);
        QuotedPrintableInputStream inputStream = new QuotedPrintableInputStream(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()));
        byte[] decodedInput = inputStream.readAllBytes();
        assertArrayEquals(byteInput, decodedInput);

        char[] SWISS_GERMAN_STUFF_UNICODE = {
                0x47, 0x72, 0xFC, 0x65, 0x7A, 0x69, 0x5F, 0x7A, 0xE4, 0x6D, 0xE4
        };
        stringInput = new String(SWISS_GERMAN_STUFF_UNICODE);
        byteInput = stringInput.getBytes(StandardCharsets.UTF_8);
        byteArrayOutputStream = new ByteArrayOutputStream();
        outputStream = new QuotedPrintableOutputStream(byteArrayOutputStream);
        outputStream.write(byteInput);
        encoded = byteArrayOutputStream.toString(StandardCharsets.UTF_8);
        assertEquals("Gr=C3=BCezi_z=C3=A4m=C3=A4", encoded);
        inputStream = new QuotedPrintableInputStream(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()));
        decodedInput = inputStream.readAllBytes();
        assertArrayEquals(byteInput, decodedInput);
    }
}
