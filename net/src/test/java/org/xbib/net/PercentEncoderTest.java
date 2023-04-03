package org.xbib.net;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.BitSet;

import static java.nio.charset.CodingErrorAction.REPLACE;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PercentEncoderTest {

    private static PercentEncoder alnum;

    private static PercentEncoder alnum16;

    @BeforeAll
    static void setUp() {
        BitSet bs = new BitSet();
        for (int i = 'a'; i <= 'z'; i++) {
            bs.set(i);
        }
        for (int i = 'A'; i <= 'Z'; i++) {
            bs.set(i);
        }
        for (int i = '0'; i <= '9'; i++) {
            bs.set(i);
        }

        alnum = new PercentEncoder(bs, StandardCharsets.UTF_8.newEncoder().onMalformedInput(REPLACE)
            .onUnmappableCharacter(REPLACE));
        alnum16 = new PercentEncoder(bs, StandardCharsets.UTF_16BE.newEncoder().onMalformedInput(REPLACE)
            .onUnmappableCharacter(REPLACE));
    }

    @Test
    void testDoesntEncodeSafe() throws Exception {
        BitSet set = new BitSet();
        for (int i = 'a'; i <= 'z'; i++) {
            set.set(i);
        }
        PercentEncoder pe = new PercentEncoder(set, StandardCharsets.UTF_8.newEncoder().onMalformedInput(REPLACE)
            .onUnmappableCharacter(REPLACE));
        assertEquals("abcd%41%42%43%44", pe.encode("abcdABCD"));
    }

    @Test
    void testEncodeInBetweenSafe() throws Exception {
        assertEquals("abc%20123", alnum.encode("abc 123"));
    }

    @Test
    void testSafeInBetweenEncoded() throws Exception {
        assertEquals("%20abc%20", alnum.encode(" abc "));
    }

    @Test
    void testEncodeUtf8() throws Exception {
        assertEquals("snowman%E2%98%83", alnum.encode("snowman\u2603"));
    }

    @Test
    void testEncodeUtf8SurrogatePair() throws Exception {
        assertEquals("clef%F0%9D%84%9E", alnum.encode("clef\ud834\udd1e"));
    }

    @Test
    void testEncodeUtf16() throws Exception {
        assertEquals("snowman%26%03", alnum16.encode("snowman\u2603"));
    }

    @Test
    void testUrlEncodedUtf16SurrogatePair() throws Exception {
        assertEquals("clef%D8%34%DD%1E", alnum16.encode("clef\ud834\udd1e"));
    }

    @Test
    void testQueryParameterEncoding() throws Exception {
        PercentEncoder queryParamEncoder = PercentEncoders.getQueryParamEncoder(StandardCharsets.UTF_8);
        assertEquals("%20a%20%3D%20b%20", queryParamEncoder.encode(" a = b "));
    }
}
