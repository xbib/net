package org.xbib.net.path;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

import java.nio.charset.StandardCharsets;

/**
 */
public class PathDecoderTest {

    @Test
    public void testPlusSign() throws Exception {
        PathDecoder decoder = new PathDecoder("/path?a=b+c", "d=e+f", StandardCharsets.UTF_8);
        assertEquals("[b c]", decoder.params().get("a").toString());
        assertEquals("[e f]", decoder.params().get("d").toString());
    }

    @Test
    public void testSlash() throws Exception {
        PathDecoder decoder = new PathDecoder("path/foo/bar/?a=b+c", "d=e+f", StandardCharsets.UTF_8);
        assertEquals("[b c]", decoder.params().get("a").toString());
        assertEquals("[e f]", decoder.params().get("d").toString());
    }

    @Test
    public void testDoubleSlashes() throws Exception {
        PathDecoder decoder = new PathDecoder("//path", "", StandardCharsets.UTF_8);
        assertEquals("/path", decoder.path());
    }

    @Test
    public void testSlashes() throws Exception {
        PathDecoder decoder = new PathDecoder("//path?a=b+c", "d=e+f", StandardCharsets.UTF_8);
        assertEquals("/path", decoder.path());
        assertEquals("[b c]", decoder.params().get("a").toString());
        assertEquals("[e f]", decoder.params().get("d").toString());
    }
}
