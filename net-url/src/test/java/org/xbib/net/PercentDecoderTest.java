package org.xbib.net;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnmappableCharacterException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static java.lang.Character.isHighSurrogate;
import static java.lang.Character.isLowSurrogate;
import static java.lang.Integer.toHexString;

/**
 */
public class PercentDecoderTest {

    private static final int CODE_POINT_IN_SUPPLEMENTARY = 2;
    private static final int CODE_POINT_IN_BMP = 1;

    private PercentDecoder decoder;

    @Before
    public void setUp() {
        decoder = new PercentDecoder(StandardCharsets.UTF_8.newDecoder());
    }

    @Test
    public void testDecodesWithoutPercents() throws Exception {
        assertEquals("asdf", decoder.decode("asdf"));
    }

    @Test
    public void testDecodeSingleByte() throws Exception {
        assertEquals("#", decoder.decode("%23"));
    }

    @Test
    public void testIncompletePercentPairNoNumbers() throws Exception {
        try {
            decoder.decode("%");
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("could not percent decode <%>: incomplete %-pair at position 0", e.getMessage());
        }
    }

    @Test
    public void testIncompletePercentPairOneNumber() throws Exception {
        try {
            decoder.decode("%2");
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("could not percent decode <%2>: incomplete %-pair at position 0", e.getMessage());
        }
    }

    @Test
    public void testInvalidHex() throws Exception {
        try {
            decoder.decode("%xz");
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("invalid %-tuple <%xz>", e.getMessage());
        }
    }

    @Test
    public void testRandomStrings() throws MalformedInputException, UnmappableCharacterException {
        PercentEncoder encoder = PercentEncoders.getQueryEncoder(StandardCharsets.UTF_8);
        Random rand = new Random();
        long seed = rand.nextLong();
        rand.setSeed(seed);
        char[] charBuf = new char[2];
        List<Integer> codePoints = new ArrayList<>();
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            buf.setLength(0);
            codePoints.clear();
            randString(buf, codePoints, charBuf, rand, 1 + rand.nextInt(1000));
            byte[] origBytes = buf.toString().getBytes(StandardCharsets.UTF_8);
            byte[] decodedBytes = null;
            String codePointsHex = String.join("", codePoints.stream().map(Integer::toHexString).collect(Collectors.toList()));
            try {
                decodedBytes = decoder.decode(encoder.encode(buf.toString())).getBytes(StandardCharsets.UTF_8);
                assertEquals("Seed: $seed Code points: $codePointsHex", toHex(origBytes), toHex(decodedBytes));
            } catch (IllegalArgumentException e) {
                List<String> charHex = new ArrayList<>();
                for (int j = 0; j < buf.toString().length(); j++) {
                    charHex.add(toHexString((int) buf.toString().charAt(j)));
                }
                fail("seed: " + seed + " code points: " + codePointsHex + " chars " + charHex + " " + e.getMessage());
            }
            assertEquals(toHex(origBytes), toHex(decodedBytes));
        }
    }

    /**
     * Generate a random string.
     * @param buf buffer to write into
     * @param codePoints list of code points to write into
     * @param charBuf char buf for temporary char wrangling (size 2)
     * @param rand random source
     * @param length max string length
     */
    private static void randString(StringBuilder buf, List<Integer> codePoints, char[] charBuf, Random rand,
                                   int length) {
        while (buf.length() < length) {
            int codePoint = rand.nextInt(17 * 65536);
            if (Character.isDefined(codePoint)) {
                int res = Character.toChars(codePoint, charBuf, 0);
                if (res == CODE_POINT_IN_BMP && (isHighSurrogate(charBuf[0]) || isLowSurrogate(charBuf[0]))) {
                    continue;
                }
                buf.append(charBuf[0]);
                codePoints.add(codePoint);
                if (res == CODE_POINT_IN_SUPPLEMENTARY) {
                    buf.append(charBuf[1]);
                }
            }
        }
    }

    private static List<String> toHex(byte[] bytes) {
        List<String> list = new ArrayList<>();
        for (byte b: bytes) {
            list.add(Integer.toHexString((int) b & 0xFF));
        }
        return list;
    }
}
