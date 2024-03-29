package org.xbib.net.path;

import org.junit.jupiter.api.Test;
import org.xbib.net.Parameter;
import org.xbib.net.URL;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PathDecoderTest {

    @Test
    void testPlusSign() throws Exception {
        PathDecoder decoder = new PathDecoder("/path?a=b+c", "d=e+f", StandardCharsets.UTF_8);
        assertEquals("[b c]", decoder.getParameter().getAll("a", Parameter.Domain.PATH).toString());
        assertEquals("[e f]", decoder.getParameter().getAll("d", Parameter.Domain.PATH).toString());
    }

    @Test
    void testSlash() throws Exception {
        PathDecoder decoder = new PathDecoder("path/foo/bar/?a=b+c", "d=e+f", StandardCharsets.UTF_8);
        assertEquals("[b c]", decoder.getParameter().getAll("a", Parameter.Domain.PATH).toString());
        assertEquals("[e f]", decoder.getParameter().getAll("d", Parameter.Domain.PATH).toString());
    }

    @Test
    void testDoubleSlashes() {
        PathDecoder decoder = new PathDecoder("//path", "", StandardCharsets.UTF_8);
        assertEquals("/path", decoder.path());
    }

    @Test
    void testSlashes() throws Exception {
        PathDecoder decoder = new PathDecoder("//path?a=b+c", "d=e+f", StandardCharsets.UTF_8);
        assertEquals("/path", decoder.path());
        assertEquals("[b c]", decoder.getParameter().getAll("a", Parameter.Domain.PATH).toString());
        assertEquals("[e f]", decoder.getParameter().getAll("d", Parameter.Domain.PATH).toString());
    }

    @Test
    void testPlusPercent() throws Exception {
        PathDecoder decoder = new PathDecoder("//path?a=b%2Bc", "d=e%2Bf", StandardCharsets.UTF_8);
        assertEquals("/path", decoder.path());
        assertEquals("[b+c]", decoder.getParameter().getAll("a", Parameter.Domain.PATH).toString());
        assertEquals("[e+f]", decoder.getParameter().getAll("d", Parameter.Domain.PATH).toString());
    }

    @Test
    void decodeURL() throws Exception {
        String requestURI = "/pdfconverter/index.gtpl?x-fl-key=20190035592&x-source=ftp://dummy@xbib.org/upload/20190035592/20190035592.pdf&x-fl-target=ftp://dummy@xbib.org/fl/download/20190035592/Fernleihe_Kopienlieferung_null_FB201900373_BLQDMT62_20190035592_20190035592.pdf&x-fl-copy=&x-fl-ack=https://xbib.org/ack/&x-fl-pages=1-";
        URL url = URL.builder().path(requestURI).build();
        assertNull(url.getHost());
        assertNull(url.getPort());
        assertEquals("/pdfconverter/index.gtpl", url.getPath());
        assertNull(url.getFragment());
        PathDecoder decoder = new PathDecoder(requestURI, StandardCharsets.UTF_8);
        if (url.getQuery() != null) {
            decoder.parse(url.getDecodedQuery());
        }
        assertEquals("x-fl-key=20190035592&x-source=ftp://dummy@xbib.org/upload/20190035592/20190035592.pdf&x-fl-target=ftp://dummy@xbib.org/fl/download/20190035592/Fernleihe_Kopienlieferung_null_FB201900373_BLQDMT62_20190035592_20190035592.pdf&x-fl-copy=&x-fl-ack=https://xbib.org/ack/&x-fl-pages=1-", url.getDecodedQuery());
        assertEquals("[x-fl-key=20190035592, x-source=ftp://dummy@xbib.org/upload/20190035592/20190035592.pdf, x-fl-target=ftp://dummy@xbib.org/fl/download/20190035592/Fernleihe_Kopienlieferung_null_FB201900373_BLQDMT62_20190035592_20190035592.pdf, x-fl-copy=, x-fl-ack=https://xbib.org/ack/, x-fl-pages=1-]", decoder.getParameter().toString());
        url = URL.from(decoder.getParameter().getAll("x-fl-target", Parameter.Domain.PATH).get(0).toString());
        assertEquals("ftp://dummy@xbib.org/fl/download/20190035592/Fernleihe_Kopienlieferung_null_FB201900373_BLQDMT62_20190035592_20190035592.pdf", url.toString());
    }
}
