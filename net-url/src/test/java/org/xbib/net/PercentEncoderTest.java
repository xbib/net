package org.xbib.net;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

import java.nio.charset.StandardCharsets;
import java.util.BitSet;

import static java.nio.charset.CodingErrorAction.REPLACE;

/**
 *
 */
public class PercentEncoderTest {

    private PercentEncoder alnum;
    private PercentEncoder alnum16;

    @Before
    public void setUp() {
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

        this.alnum = new PercentEncoder(bs, StandardCharsets.UTF_8.newEncoder().onMalformedInput(REPLACE)
            .onUnmappableCharacter(REPLACE));
        this.alnum16 = new PercentEncoder(bs, StandardCharsets.UTF_16BE.newEncoder().onMalformedInput(REPLACE)
            .onUnmappableCharacter(REPLACE));
    }

    @Test
    public void testDoesntEncodeSafe() throws Exception {
        BitSet set = new BitSet();
        for (int i = 'a'; i <= 'z'; i++) {
            set.set(i);
        }
        PercentEncoder pe = new PercentEncoder(set, StandardCharsets.UTF_8.newEncoder().onMalformedInput(REPLACE)
            .onUnmappableCharacter(REPLACE));
        assertEquals("abcd%41%42%43%44", pe.encode("abcdABCD"));
    }

    @Test
    public void testEncodeInBetweenSafe() throws Exception {
        assertEquals("abc%20123", alnum.encode("abc 123"));
    }

    @Test
    public void testSafeInBetweenEncoded() throws Exception {
        assertEquals("%20abc%20", alnum.encode(" abc "));
    }

    @Test
    public void testEncodeUtf8() throws Exception {
        assertEquals("snowman%E2%98%83", alnum.encode("snowman\u2603"));
    }

    @Test
    public void testEncodeUtf8SurrogatePair() throws Exception {
        assertEquals("clef%F0%9D%84%9E", alnum.encode("clef\ud834\udd1e"));
    }

    @Test
    public void testEncodeUtf16() throws Exception {
        assertEquals("snowman%26%03", alnum16.encode("snowman\u2603"));
    }

    @Test
    public void testUrlEncodedUtf16SurrogatePair() throws Exception {
        assertEquals("clef%D8%34%DD%1E", alnum16.encode("clef\ud834\udd1e"));
    }
}
