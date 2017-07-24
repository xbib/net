package org.xbib.net;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.text.Normalizer;

/**
 *
 */
public class IRITest {

    @Test
    public void testIpv4() {
        URL iri = URL.create("http://127.0.0.1");
        assertEquals("http://127.0.0.1", iri.toExternalForm());
    }

    @Test
    public void testIpv6() {
        URL iri = URL.from("http://[2001:0db8:85a3:08d3:1319:8a2e:0370:7344]");
        assertTrue(iri.getProtocolVersion().equals(ProtocolVersion.IPV6));
        assertEquals("http://[2001:db8:85a3:8d3:1319:8a2e:370:7344]", iri.toString());
    }

    @Test
    public void testIpv6Invalid() {
        URL iri = URL.from("http://[2001:0db8:85a3:08d3:1319:8a2e:0370:734o]");
        assertEquals(URL.INVALID, iri);
    }

    @Test
    public void testSimple() {
        URL iri = URL.create("http://validator.w3.org/check?uri=http%3A%2F%2Fr\u00E9sum\u00E9.example.org");
        //assertEquals("http://validator.w3.org/check?uri=http%3A%2F%2Fr\u00E9sum\u00E9.example.org", iri.toString());
        assertEquals("http://validator.w3.org/check?uri=http://r%C3%A9sum%C3%A9.example.org",
                iri.toExternalForm());
    }

    @Test
    public void testFile() throws Exception {
        URL iri = URL.create("file:///tmp/test/foo");
        assertEquals("", iri.getHost());
        assertEquals("/tmp/test/foo", iri.getPath());
        assertEquals("file:///tmp/test/foo", iri.toExternalForm());
        assertEquals("file:///tmp/test/foo", iri.toString());
    }

    @Test
    public void testSimple2() throws Exception {
        URL iri = URL.create("http://www.example.org/red%09ros\u00E9#red");
        assertEquals("http://www.example.org/red%09ros%C3%A9#red", iri.toExternalForm());
    }

    @Test
    public void testNotSoSimple() throws Exception {
        URL iri = URL.create("http://example.com/\uD800\uDF00\uD800\uDF01\uD800\uDF02");
        assertEquals("http://example.com/%F0%90%8C%80%F0%90%8C%81%F0%90%8C%82", iri.toExternalForm());
    }

    @Test
    public void testIRItoURI() throws Exception {
        URL iri = URL.from("http://\u7D0D\u8C46.example.org/%E2%80%AE");
        assertEquals("http://xn--99zt52a.example.org/%E2%80%AE", iri.toExternalForm());
    }

    @Test
    public void testComparison() throws Exception {

        URL url1 = URL.create("http://www.example.org/");
        URL url2 = URL.create("http://www.example.org/..");
        URL url3 = URL.create("http://www.Example.org:80");

        assertNotEquals(url1, url2);
        assertNotEquals(url1, url3);
        assertNotEquals(url2, url1);
        assertNotEquals(url2, url3);
        assertNotEquals(url3, url1);
        assertNotEquals(url3, url2);

        assertEquals(url1.normalize(), url2.normalize());
        assertEquals(url1.normalize(), url3.normalize());
        assertEquals(url2.normalize(), url1.normalize());
        assertEquals(url2.normalize(), url3.normalize());
        assertEquals(url3.normalize(), url1.normalize());
        assertEquals(url3.normalize(), url2.normalize());
    }

    @Test
    public void testUCN() throws Exception {
        URL iri1 = URL.create("http://www.example.org/r\u00E9sum\u00E9.html");
        String s = Normalizer.normalize("http://www.example.org/re\u0301sume\u0301.html", Normalizer.Form.NFC);
        URL iri2 = URL.create(s);
        assertEquals(iri2, iri1);
    }

    @Test
    public void testPercent() {
        URL iri1 = URL.create("http://example.org/%7e%2Fuser?%2f");
        URL iri2 = URL.create("http://example.org/%7E%2fuser?/");
        assertEquals(iri1.normalize(), iri2.normalize());
    }

    @Test
    public void testIDN() {
        URL iri1 = URL.from("http://r\u00E9sum\u00E9.example.org");
        assertEquals("xn--rsum-bpad.example.org", iri1.getHost());
    }

    @Test
    public void testResolveRelative() {
        URL base = URL.create("http://example.org/foo/");
        assertEquals("http://example.org/", base.resolve("/").toString());
        assertEquals("http://example.org/test", base.resolve("/test").toString());
        assertEquals("http://example.org/foo/test", base.resolve("test").toString());
        assertEquals("http://example.org/test", base.resolve("../test").toString());
        assertEquals("http://example.org/foo/test", base.resolve("./test").toString());
        assertEquals("http://example.org/foo/", base.resolve("test/test/../../").toString());
        assertEquals("http://example.org/foo/?test", base.resolve("?test").toString());
        assertEquals("http://example.org/foo/#test", base.resolve("#test").toString());
        assertEquals("http://example.org/foo/", base.resolve(".").toString());
    }

    @Test
    public void testSchemes() {

        URL iri = URL.create("http://a:b@c.org:80/d/e?f#g");
        assertEquals("http", iri.getScheme());
        assertEquals("a:b", iri.getUserInfo());
        assertEquals("c.org", iri.getHost());
        assertEquals(Integer.valueOf(80), iri.getPort());
        assertEquals("/d/e", iri.getPath());
        assertEquals("f", iri.getQuery());
        assertEquals("g", iri.getFragment());

        iri = URL.create("https://a:b@c.org:80/d/e?f#g");
        assertEquals("https", iri.getScheme());
        assertEquals("a:b", iri.getUserInfo());
        assertEquals("c.org", iri.getHost());
        assertEquals(Integer.valueOf(80), iri.getPort());
        assertEquals("/d/e", iri.getPath());
        assertEquals("f", iri.getQuery());
        assertEquals("g", iri.getFragment());

        iri = URL.create("ftp://a:b@c.org:80/d/e?f#g");
        assertEquals("ftp", iri.getScheme());
        assertEquals("a:b", iri.getUserInfo());
        assertEquals("c.org", iri.getHost());
        assertEquals(Integer.valueOf(80), iri.getPort());
        assertEquals("/d/e", iri.getPath());
        assertEquals("f", iri.getQuery());
        assertEquals("g", iri.getFragment());

        iri = URL.create("mailto:joe@example.org?subject=foo");
        assertEquals("mailto", iri.getScheme());
        assertEquals(null, iri.getUserInfo());
        assertEquals(null, iri.getHost());
        assertEquals(null, iri.getPort());
        assertEquals("joe@example.org?subject=foo", iri.getSchemeSpecificPart());
        assertEquals(null, iri.getFragment());

        iri = URL.create("tag:example.org,2006:foo");
        assertEquals("tag", iri.getScheme());
        assertEquals(null, iri.getUserInfo());
        assertEquals(null, iri.getHost());
        assertEquals(null, iri.getPort());
        assertEquals("example.org,2006:foo", iri.getSchemeSpecificPart());
        assertEquals(null, iri.getQuery());
        assertEquals(null, iri.getFragment());

        iri = URL.create("urn:lsid:ibm.com:example:82437234964354895798234d");
        assertEquals("urn", iri.getScheme());
        assertEquals(null, iri.getUserInfo());
        assertEquals(null, iri.getHost());
        assertEquals(null, iri.getPort());
        assertEquals("lsid:ibm.com:example:82437234964354895798234d", iri.getSchemeSpecificPart());
        assertEquals(null, iri.getQuery());
        assertEquals(null, iri.getFragment());

        iri = URL.create("data:image/gif;base64,R0lGODdhMAAwAPAAAAAAAP");
        assertEquals("data", iri.getScheme());
        assertEquals(null, iri.getUserInfo());
        assertEquals(null, iri.getHost());
        assertEquals(null, iri.getPort());
        assertEquals("image/gif;base64,R0lGODdhMAAwAPAAAAAAAP", iri.getSchemeSpecificPart());
        assertEquals(null, iri.getQuery());
        assertEquals(null, iri.getFragment());

    }
}
