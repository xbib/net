package org.xbib.net.path;

import org.junit.jupiter.api.Test;
import org.xbib.net.URL;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PathDecoderTest {

    @Test
    void testPlusSign() throws Exception {
        PathDecoder decoder = new PathDecoder("/path?a=b+c", "d=e+f");
        assertEquals("[b c]", decoder.getParameter().getAll("a", "DEFAULT").toString());
        assertEquals("[e f]", decoder.getParameter().getAll("d", "DEFAULT").toString());
    }

    @Test
    void testSlash() throws Exception {
        PathDecoder decoder = new PathDecoder("path/foo/bar/?a=b+c", "d=e+f");
        assertEquals("[b c]", decoder.getParameter().getAll("a", "DEFAULT").toString());
        assertEquals("[e f]", decoder.getParameter().getAll("d", "DEFAULT").toString());
    }

    @Test
    void testDoubleSlashes() throws Exception {
        PathDecoder decoder = new PathDecoder("//path", "");
        assertEquals("/path", decoder.path());
    }

    @Test
    void testSlashes() throws Exception {
        PathDecoder decoder = new PathDecoder("//path?a=b+c", "d=e+f");
        assertEquals("/path", decoder.path());
        assertEquals("[b c]", decoder.getParameter().getAll("a", "DEFAULT").toString());
        assertEquals("[e f]", decoder.getParameter().getAll("d", "DEFAULT").toString());
    }

    @Test
    void testPlusPercent() throws Exception {
        PathDecoder decoder = new PathDecoder("//path?a=b%2Bc", "d=e%2Bf");
        assertEquals("/path", decoder.path());
        assertEquals("[b+c]", decoder.getParameter().getAll("a", "DEFAULT").toString());
        assertEquals("[e+f]", decoder.getParameter().getAll("d", "DEFAULT").toString());
    }

    @Test
    void decodeURL() {
        String requestURI = "/pdfconverter/index.gtpl?x-fl-key=20190035592&x-source=ftp://dummy@xbib.org/upload/20190035592/20190035592.pdf&x-fl-target=ftp://dummy@xbib.org/fl/download/20190035592/Fernleihe_Kopienlieferung_null_FB201900373_BLQDMT62_20190035592_20190035592.pdf&x-fl-copy=&x-fl-ack=https://xbib.org/ack/&x-fl-pages=1-";
        URL url = URL.builder().path(requestURI).build();
        assertNull(url.getHost());
        assertNull(url.getPort());
        assertEquals("/pdfconverter/index.gtpl", url.getPath());
        assertNull(url.getFragment());
        PathDecoder decoder = new PathDecoder(requestURI);
        if (url.getQuery() != null) {
            decoder.parse(url.getDecodedQuery());
        }
        assertEquals("x-fl-key=20190035592&x-source=ftp://dummy@xbib.org/upload/20190035592/20190035592.pdf&x-fl-target=ftp://dummy@xbib.org/fl/download/20190035592/Fernleihe_Kopienlieferung_null_FB201900373_BLQDMT62_20190035592_20190035592.pdf&x-fl-copy=&x-fl-ack=https://xbib.org/ack/&x-fl-pages=1-", url.getDecodedQuery());
        assertEquals("[x-fl-key=20190035592, x-source=ftp://dummy@xbib.org/upload/20190035592/20190035592.pdf, x-fl-target=ftp://dummy@xbib.org/fl/download/20190035592/Fernleihe_Kopienlieferung_null_FB201900373_BLQDMT62_20190035592_20190035592.pdf, x-fl-copy=, x-fl-ack=https://xbib.org/ack/, x-fl-pages=1-]", decoder.getParameter().toString());
        url = URL.from(decoder.getParameter().getAll("x-fl-target", "DEFAULT").get(0).toString());
        assertEquals("ftp://dummy@xbib.org/fl/download/20190035592/Fernleihe_Kopienlieferung_null_FB201900373_BLQDMT62_20190035592_20190035592.pdf", url.toString());
    }
}
