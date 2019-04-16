package org.xbib.net;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 *
 */
public class URLParserTest {

    @Test
    public void testNull() {
        assertEquals(URL.nullUrl(), URL.from(null));
    }

    @Test
    public void testEmpty() {
        assertEquals(URL.nullUrl(), URL.from(""));
    }

    @Test
    public void testNewline() {
        assertEquals(URL.nullUrl(), URL.from("\n"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidScheme() {
        URL.from("/:23");
    }

    @Test
    public void testScheme() throws Exception {
        URL url = URL.from("http://");
        assertEquals("http://", url.toExternalForm());
        assertEquals("http://", url.toString());
        assertRoundTrip(url.toExternalForm());
    }

    @Test
    public void testPath(){
        URL url = URL.from("http");
        assertFalse(url.isAbsolute());
        assertNull(url.getScheme());
        assertEquals("", url.getHostInfo());
        assertEquals("http", url.getPath());
        assertEquals("http", url.toExternalForm());
        assertEquals("http", url.toString());
    }

    @Test
    public void testOpaque() throws Exception {
        URL url = URL.from("a:b");
        assertEquals("a", url.getScheme());
        assertEquals("b", url.getSchemeSpecificPart());
        assertEquals("a:b", url.toExternalForm());
        assertEquals("a:b", url.toString());
        assertRoundTrip(url.toExternalForm());
    }

    @Test
    public void testGopher() {
        URL url = URL.from("gopher:/example.com/");
        assertEquals("gopher:/example.com/", url.toExternalForm());
    }

    @Test
    public void testWithoutDoubleSlash() throws Exception {
        URL url = URL.from("http:foo.com");
        assertEquals("http:foo.com", url.toExternalForm());
        assertEquals("http:foo.com", url.toString());
        assertRoundTrip(url.toExternalForm());
    }

    @Test
    public void testSlashAfterScheme() {
        URL url = URL.from("http:/example.com/");
        assertEquals("http:/example.com/", url.toExternalForm());
    }

    @Test
    public void testSchemeHost() throws Exception {
        URL url = URL.from("http://foo.bar");
        assertEquals("http://foo.bar", url.toString());
        assertRoundTrip(url.toExternalForm());
    }

    @Test
    public void testSchemeHostPort() throws Exception {
        URL url = URL.from("http://f:/c");
        assertEquals("http://f:/c", url.toString());
        assertRoundTrip(url.toExternalForm());
    }

    @Test
    public void testNetworkLocation() {
        URL url = URL.from("//foo.bar");
        assertEquals("//foo.bar", url.toExternalForm());
        assertEquals("//foo.bar", url.toString());
    }

    @Test
    public void testSchemeHostAuthInfo() throws Exception {
        URL url = URL.from("http://auth@foo.bar");
        assertEquals("http://auth@foo.bar", url.toString());
        assertRoundTrip(url.toExternalForm());
    }

    @Test
    public void testSchemeHostAuthInfoPort() throws Exception {
        URL url = URL.from("http://auth@foo.bar:1");
        assertEquals("http://auth@foo.bar:1", url.toString());
        assertRoundTrip(url.toExternalForm());
    }

    @Test
    public void testSchemeHostAuthInfoPortPath() throws Exception {
        URL url = URL.from("http://auth@foo.bar:1/path");
        assertEquals("http://auth@foo.bar:1/path", url.toString());
        assertRoundTrip(url.toExternalForm());
    }

    @Test
    public void testTrailingSlash() throws Exception {
        URL url = URL.from("http://foo.bar/path/");
        assertEquals("http://foo.bar/path/", url.toString());
        assertRoundTrip(url.toExternalForm());
    }

    @Test
    public void testBackslash() {
        URL url = URL.from("http://foo.com/\\@");
        assertEquals("http://foo.com/@", url.toExternalForm());
    }

    @Test
    public void testQuery() throws Exception {
        URL url = URL.from("http://auth@foo.bar:1/path?query");
        assertEquals("http://auth@foo.bar:1/path?query", url.toString());
        assertRoundTrip(url.toExternalForm());
    }

    @Test
    public void testFragment() throws Exception {
        URL url = URL.from("http://auth@foo.bar:1/path#fragment");
        assertEquals("http://auth@foo.bar:1/path#fragment", url.toString());
        assertRoundTrip(url.toExternalForm());
    }

    @Test
    public void testReservedChar() throws Exception {
        URL url = URL.from("http://www.google.com/ig/calculator?q=1USD=?EUR");
        if ("false".equals(System.getProperty("java.net.preferIPv6Addresses"))) {
            assertEquals("http://www.google.com/ig/calculator?q=1USD%3D?EUR", url.toString());
        }
        assertRoundTrip(url.toExternalForm());
    }

    @Test
    public void testPassword() throws Exception {
        URL url = URL.from("ftp://aaa:b%2B1@www.google.com");
        assertEquals("b+1", url.getPassword());
        assertRoundTrip(url.toExternalForm());
        url = URL.from("ftp://aaa:b+1@www.google.com");
        assertEquals("b+1", url.getPassword());
        assertRoundTrip(url.toExternalForm());
    }

    @Test
    public void testPlus() throws Exception {
        URL url = URL.from("http://foobar:8080/test/print?value=%EA%B0%80+%EB%82%98");
        assertEquals("http://foobar:8080/test/print?value=%EA%B0%80%2B%EB%82%98", url.toExternalForm());
        assertRoundTrip(url.toExternalForm());
    }

    @Test
    public void testIPv6() throws Exception {
        URL url = URL.from("http://[2001:db8:85a3::8a2e:370:7334]");
        assertEquals("http://[2001:db8:85a3:0:0:8a2e:370:7334]", url.toString());
        assertRoundTrip(url.toExternalForm());
    }

    @Test
    public void testIPv6WithScope() throws Exception {
        // test scope ID. Must be a valid IPv6
        URL url = URL.from("http://[3002:0:0:0:20c:29ff:fe64:614a%2]:8080/resource");
        assertEquals("http://[3002:0:0:0:20c:29ff:fe64:614a%2]:8080/resource", url.toString());
        assertRoundTrip(url.toExternalForm());
    }

    @Test
    public void testIPv6WithIPv4() throws Exception {
        URL url = URL.from("http://[::192.168.1.1]:8080/resource");
        assertEquals("http://[0:0:0:0:0:0:c0a8:101]:8080/resource", url.toString());
        assertRoundTrip(url.toExternalForm());
    }

    @Test
    public void testFromUrlWithEverything() throws Exception {
        assertUrlCompatibility("https://foo.bar.com:3333/foo/ba%20r;mtx1=val1;mtx2=val%202/"
                + "seg%203;m2=v2?q1=v1&q2=v%202#zomg%20it's%20a%20fragment");
    }

    @Test
    public void testFromUrlWithEmptyPath() throws Exception {
        assertUrlCompatibility("http://foo.com");
    }

    @Test
    public void testFromUrlWithPort() throws Exception {
        assertUrlCompatibility("http://foo.com:1234");
    }

    @Test
    public void testFromUrlWithEncodedHost() throws Exception {
        assertUrlCompatibility("http://f%20oo.com/bar");
    }

    @Test
    public void testFromUrlWithEncodedPathSegment() throws Exception {
        assertUrlCompatibility("http://foo.com/foo/b%20ar");
    }

    @Test
    public void testFromUrlWithEncodedMatrixParam() throws Exception {
        assertUrlCompatibility("http://foo.com/foo;m1=v1;m%202=v%202");
    }

    @Test
    public void testFromUrlWithEncodedQueryParam() throws Exception {
        assertUrlCompatibility("http://foo.com/foo?q%201=v%202&q2=v2");
    }

    @Test
    public void testFromUrlWithEncodedQueryParamDelimiter() throws Exception {
        assertUrlCompatibility("http://foo.com/foo?q1=%3Dv1&%26q2=v2");
    }

    @Test
    public void testFromUrlWithEncodedFragment() throws Exception {
        assertUrlCompatibility("http://foo.com/foo#b%20ar");
    }

    @Test
    public void testFromUrlWithEmptyPathSegmentWithMatrixParams() throws Exception {
        assertUrlCompatibility("http://foo.com/foo/;m1=v1");
    }

    @Test
    public void testFromUrlWithEmptyPathWithMatrixParams() throws Exception {
        assertUrlCompatibility("http://foo.com/;m1=v1");
    }

    @Test
    public void testFromUrlWithEmptyPathWithMultipleMatrixParams() throws Exception {
        assertUrlCompatibility("http://foo.com/;m1=v1;m2=v2");
    }

    @Test
    public void testFromUrlMalformedQueryParamNoValue() throws Exception {
        assertUrlCompatibility("http://foo.com/foo?q1=v1&q2");
    }

    @Test
    public void testFromUrlMalformedQueryParamMultiValues() throws Exception {
        assertRoundTrip("http://foo.com/foo?q1=v1=v2");
    }

    @Test
    public void testFromUrlQueryWithEscapedChars() throws Exception {
        assertRoundTrip("http://foo.com/foo?query==&%23");
    }

    @Test
    public void testSimple() throws Exception {
        URL url = URL.parser().parse("http://foo.com/seg1/seg2");
        assertEquals("http", url.getScheme());
        assertEquals("foo.com", url.getHostInfo());
        assertEquals("/seg1/seg2", url.getPath());
    }

    @Test
    public void testReserved() throws Exception {
        URL url = URL.parser().parse("http://foo.com/seg%2F%3B%3Fment/seg=&2");
        assertEquals("http", url.getScheme());
        assertEquals("foo.com", url.getHostInfo());
        assertEquals("/seg%2F%3B%3Fment/seg=&2", url.getPath());
    }

    @Test
    public void testMatrix() throws Exception {
        URL url = URL.parser().parse("http://foo.com/;foo=bar");
        assertEquals("http", url.getScheme());
        assertEquals("foo.com", url.getHostInfo());
        assertEquals("/;foo=bar", url.getPath());
    }

    @Test
    public void testAnotherQuery() throws Exception {
        URL url = URL.parser().parse("http://foo.com?foo=bar");
        assertEquals("http", url.getScheme());
        assertEquals("foo.com", url.getHostInfo());
        assertEquals("foo=bar", url.getQuery());
    }

    @Test
    public void testQueryAndFragment() throws Exception {
        URL url = URL.parser().parse("http://foo.com?foo=bar#fragment");
        assertEquals("http", url.getScheme());
        assertEquals("foo.com", url.getHostInfo());
        assertEquals("foo=bar", url.getQuery());
        assertEquals("fragment", url.getFragment());
    }

    @Test
    public void testRelative() throws Exception {
        URL url = URL.parser().parse("/foo/bar?foo=bar#fragment");
        assertNull(url.getScheme());
        assertEquals("", url.getHostInfo());
        assertEquals("/foo/bar", url.getPath());
        assertEquals("foo=bar", url.getQuery());
        assertEquals("fragment", url.getFragment());
    }

    @Test
    public void testRelativeDecoded() throws Exception {
        URL url = URL.parser().parse("/foo/bar%2F?foo=b%2Far#frag%2Fment");
        assertNull(url.getScheme());
        assertEquals("", url.getHostInfo());
        assertEquals("/foo/bar/", url.getDecodedPath());
        assertEquals("foo=b/ar", url.getDecodedQuery());
        assertEquals("frag/ment", url.getDecodedFragment());
    }

    @Test
    public void testFileSchemeSpecificPart() throws Exception {
        URL url = URL.parser().parse("file:foo/bar?foo=bar#fragment");
        assertEquals("", url.getHostInfo());
        assertNotNull(url.getSchemeSpecificPart());
        assertEquals("foo/bar?foo=bar#fragment", url.getSchemeSpecificPart());
    }

    @Test
    public void testRelativeFilePath() throws Exception {
        URL url = URL.parser().parse("file:/foo/bar?foo=bar#fragment");
        assertEquals("file", url.getScheme());
        assertEquals("", url.getHostInfo());
        assertEquals("/foo/bar", url.getPath());
        assertEquals("foo=bar", url.getQuery());
        assertEquals("fragment", url.getFragment());
    }

    @Test
    public void testAbsoluteFilePath() throws Exception {
        URL url = URL.parser().parse("file:///foo/bar?foo=bar#fragment");
        assertEquals("file", url.getScheme());
        assertEquals("", url.getHostInfo());
        assertEquals("/foo/bar", url.getPath());
        assertEquals("foo=bar", url.getQuery());
        assertEquals("fragment", url.getFragment());
    }

    @Test
    public void testMoreQuery() throws Exception {
        URL url = URL.parser().parse("http://foo.com?foo=bar%26%3D%23baz&foo=bar?/2");
        assertEquals("foo=bar%26%3D%23baz&foo=bar?/2", url.getQuery());
        assertEquals("foo=bar&=#baz&foo=bar?/2", url.getDecodedQuery());
    }

    @Test
    public void testAnotherPlus() throws Exception {
        URL url = URL.parser().parse("http://foo.com/has+plus;plusMtx=pl+us?plusQp=pl%2Bus#plus+frag");
        assertEquals("/has+plus;plusMtx=pl+us", url.getPath());
        assertEquals("plusQp=pl%2Bus", url.getQuery());
        assertEquals("plus+frag", url.getFragment());
    }

    @Test
    public void testUserInfo() throws Exception {
        URL url = URL.parser().parse("http://foo:bar@foo.com/");
        assertEquals("foo:bar", url.getUserInfo());
        url = URL.parser().parse("http://foo:foo:bar@foo.com/");
        assertEquals("foo:foo:bar", url.getUserInfo());
        url = URL.parser().parse("http://foo:foo%3Abar@foo.com/");
        assertEquals("foo:foo:bar", url.getUserInfo());
        assertEquals("foo", url.getUser());
        assertEquals("foo:bar", url.getPassword());
    }

    private void assertUrlCompatibility(String url) throws Exception {
        String s = URL.from(url).toExternalForm();
        assertEquals(s, URL.from(s).toExternalForm());
        assertEquals(s, new java.net.URL(url).toExternalForm());
    }

    private void assertRoundTrip(String url) throws Exception {
        String s = URL.from(url).toExternalForm();
        assertEquals(s, URL.from(s).toExternalForm());
    }
}
