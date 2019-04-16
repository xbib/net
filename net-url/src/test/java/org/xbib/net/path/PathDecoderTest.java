package org.xbib.net.path;

import org.junit.Test;
import org.xbib.net.URL;

import static org.junit.Assert.assertEquals;

import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnmappableCharacterException;
import java.util.logging.Level;
import java.util.logging.Logger;

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

    @Test
    public void testPlusPercent() throws Exception {
        PathDecoder decoder = new PathDecoder("//path?a=b%2Bc", "d=e%2Bf", StandardCharsets.UTF_8);
        assertEquals("/path", decoder.path());
        assertEquals("[b+c]", decoder.params().get("a").toString());
        assertEquals("[e+f]", decoder.params().get("d").toString());
    }

    @Test
    public void decodeURL() throws MalformedInputException, UnmappableCharacterException {
        String requestURI = "/pdfconverter/index.gtpl?x-fl-key=20190035592&x-fl-source=ftp://DE-465:r09t00k25@herakles.hbz-nrw.de/fl/upload/20190035592/20190035592.pdf&x-fl-target=ftp://DE-1073:haribo%2B1@herakles.hbz-nrw.de/fl/download/20190035592/Fernleihe_Kopienlieferung_null_FB201900373_BLQDMT62_20190035592_20190035592.pdf&x-fl-copy=&x-fl-ack=https://fl.hbz-nrw.de/app/ack/index.gtpl&x-fl-pages=1-";
        URL url = URL.builder().path(requestURI).build();
        log.log(Level.INFO, "URL: url=" + url + " path=" + url.getPath() + " query=" + url.getQuery() +
                " fragment=" + url.getFragment());
        PathDecoder decoder = new PathDecoder(requestURI, StandardCharsets.UTF_8);
        if (url.getQuery() != null) {
            decoder.parse(url.getDecodedQuery());
        }
        log.log(Level.INFO, "decoded query=" + url.getDecodedQuery());
        log.log(Level.INFO, "path decoder params=" + decoder.params());
        url = URL.from(decoder.params().get("x-fl-target").get(0));
        log.log(Level.INFO, "url=" + url);
    }

    private static final Logger log = Logger.getLogger(PathDecoderTest.class.getName());
}
