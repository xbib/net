package org.xbib.net;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class URLParserTest {

    @Test
    void testNull() {
        assertEquals(URL.nullUrl(), URL.from(null));
    }

    @Test
    void testEmpty() {
        assertEquals(URL.nullUrl(), URL.from(""));
    }

    @Test
    void testNewline() {
        assertEquals(URL.nullUrl(), URL.from("\n"));
    }

    @Test
    void testInvalidScheme() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> URL.from("/:23"));
    }

    @Test
    void testScheme() {
        URL url = URL.from("http://");
        assertEquals("http://", url.toExternalForm());
        assertEquals("http://", url.toString());
        assertRoundTrip(url.toExternalForm());
    }

    @Test
    void testPath(){
        URL url = URL.from("http");
        assertFalse(url.isAbsolute());
        assertNull(url.getScheme());
        assertEquals("", url.getHostInfo());
        assertEquals("http", url.getPath());
        assertEquals("http", url.toExternalForm());
        assertEquals("http", url.toString());
    }

    @Test
    void testOpaque() {
        URL url = URL.from("a:b");
        assertEquals("a", url.getScheme());
        assertEquals("b", url.getSchemeSpecificPart());
        assertEquals("a:b", url.toExternalForm());
        assertEquals("a:b", url.toString());
        assertRoundTrip(url.toExternalForm());
    }

    @Test
    void testGopher() {
        URL url = URL.from("gopher:/example.com/");
        assertEquals("gopher:/example.com/", url.toExternalForm());
    }

    @Test
    void testWithoutDoubleSlash() {
        URL url = URL.from("http:foo.com");
        assertEquals("http:foo.com", url.toExternalForm());
        assertEquals("http:foo.com", url.toString());
        assertRoundTrip(url.toExternalForm());
    }

    @Test
    void testSlashAfterScheme() {
        URL url = URL.from("http:/example.com/");
        assertEquals("http:/example.com/", url.toExternalForm());
    }

    @Test
    void testSchemeHost() {
        URL url = URL.from("http://foo.bar");
        assertEquals("http://foo.bar", url.toString());
        assertRoundTrip(url.toExternalForm());
    }

    @Test
    void testSchemeHostPort() {
        URL url = URL.from("http://f:/c");
        assertEquals("http://f:/c", url.toString());
        assertRoundTrip(url.toExternalForm());
    }

    @Test
    void testNetworkLocation() {
        URL url = URL.from("//foo.bar");
        assertEquals("//foo.bar", url.toExternalForm());
        assertEquals("//foo.bar", url.toString());
    }

    @Test
    void testSchemeHostAuthInfo() {
        URL url = URL.from("http://auth@foo.bar");
        assertEquals("http://auth@foo.bar", url.toString());
        assertRoundTrip(url.toExternalForm());
    }

    @Test
    void testSchemeHostAuthInfoPort() {
        URL url = URL.from("http://auth@foo.bar:1");
        assertEquals("http://auth@foo.bar:1", url.toString());
        assertRoundTrip(url.toExternalForm());
    }

    @Test
    void testSchemeHostAuthInfoPortPath() {
        URL url = URL.from("http://auth@foo.bar:1/path");
        assertEquals("http://auth@foo.bar:1/path", url.toString());
        assertRoundTrip(url.toExternalForm());
    }

    @Test
    void testTrailingSlash() {
        URL url = URL.from("http://foo.bar/path/");
        assertEquals("http://foo.bar/path/", url.toString());
        assertRoundTrip(url.toExternalForm());
    }

    @Test
    void testBackslash() {
        URL url = URL.from("http://foo.com/\\@");
        assertEquals("http://foo.com/@", url.toExternalForm());
    }

    @Test
    void testQuery() {
        URL url = URL.from("http://auth@foo.bar:1/path?query");
        assertEquals("http://auth@foo.bar:1/path?query", url.toString());
        assertRoundTrip(url.toExternalForm());
    }

    @Test
    void testFragment() {
        URL url = URL.from("http://auth@foo.bar:1/path#fragment");
        assertEquals("http://auth@foo.bar:1/path#fragment", url.toString());
        assertRoundTrip(url.toExternalForm());
    }

    @Test
    void testReservedChar() {
        URL url = URL.from("http://www.google.com/ig/calculator?q=1USD=?EUR");
        if ("false".equals(System.getProperty("java.net.preferIPv6Addresses"))) {
            assertEquals("http://www.google.com/ig/calculator?q=1USD%3D?EUR", url.toString());
        }
        assertRoundTrip(url.toExternalForm());
    }

    @Test
    void testPassword() {
        URL url = URL.from("ftp://aaa:b%2B1@www.google.com");
        assertEquals("b+1", url.getPassword());
        assertRoundTrip(url.toExternalForm());
        url = URL.from("ftp://aaa:b+1@www.google.com");
        assertEquals("b+1", url.getPassword());
        assertRoundTrip(url.toExternalForm());
    }

    @Test
    void testPlus() {
        URL url = URL.from("http://foobar:8080/test/print?value=%EA%B0%80+%EB%82%98");
        assertEquals("http://foobar:8080/test/print?value=%EA%B0%80%2B%EB%82%98", url.toExternalForm());
        assertRoundTrip(url.toExternalForm());
    }

    @Test
    void testIPv6() {
        URL url = URL.from("http://[2001:db8:85a3::8a2e:370:7334]");
        assertEquals("http://[2001:db8:85a3:0:0:8a2e:370:7334]", url.toString());
        assertRoundTrip(url.toExternalForm());
    }

    @Test
    void testIPv6WithScope() {
        // test scope ID. Must be a valid IPv6
        URL url = URL.from("http://[3002:0:0:0:20c:29ff:fe64:614a%2]:8080/resource");
        assertEquals("http://[3002:0:0:0:20c:29ff:fe64:614a%2]:8080/resource", url.toString());
        assertRoundTrip(url.toExternalForm());
    }

    @Test
    void testIPv6WithIPv4() {
        URL url = URL.from("http://[::192.168.1.1]:8080/resource");
        assertEquals("http://[0:0:0:0:0:0:c0a8:101]:8080/resource", url.toString());
        assertRoundTrip(url.toExternalForm());
    }

    @Test
    void testFromUrlWithEverything() throws Exception {
        assertUrlCompatibility("https://foo.bar.com:3333/foo/ba%20r;mtx1=val1;mtx2=val%202/"
                + "seg%203;m2=v2?q1=v1&q2=v%202#zomg%20it's%20a%20fragment");
    }

    @Test
    void testFromUrlWithEmptyPath() throws Exception {
        assertUrlCompatibility("http://foo.com");
    }

    @Test
    void testFromUrlWithPort() throws Exception {
        assertUrlCompatibility("http://foo.com:1234");
    }

    @Test
    void testFromUrlWithEncodedHost() throws Exception {
        assertUrlCompatibility("http://f%20oo.com/bar");
    }

    @Test
    void testFromUrlWithEncodedPathSegment() throws Exception {
        assertUrlCompatibility("http://foo.com/foo/b%20ar");
    }

    @Test
    void testFromUrlWithEncodedMatrixParam() throws Exception {
        assertUrlCompatibility("http://foo.com/foo;m1=v1;m%202=v%202");
    }

    @Test
    void testFromUrlWithEncodedQueryParam() throws Exception {
        assertUrlCompatibility("http://foo.com/foo?q%201=v%202&q2=v2");
    }

    @Test
    void testFromUrlWithEncodedQueryParamDelimiter() throws Exception {
        assertUrlCompatibility("http://foo.com/foo?q1=%3Dv1&%26q2=v2");
    }

    @Test
    void testFromUrlWithEncodedFragment() throws Exception {
        assertUrlCompatibility("http://foo.com/foo#b%20ar");
    }

    @Test
    void testFromUrlWithEmptyPathSegmentWithMatrixParams() throws Exception {
        assertUrlCompatibility("http://foo.com/foo/;m1=v1");
    }

    @Test
    void testFromUrlWithEmptyPathWithMatrixParams() throws Exception {
        assertUrlCompatibility("http://foo.com/;m1=v1");
    }

    @Test
    void testFromUrlWithEmptyPathWithMultipleMatrixParams() throws Exception {
        assertUrlCompatibility("http://foo.com/;m1=v1;m2=v2");
    }

    @Test
    void testFromUrlMalformedQueryParamNoValue() throws Exception {
        assertUrlCompatibility("http://foo.com/foo?q1=v1&q2");
    }

    @Test
    void testFromUrlMalformedQueryParamMultiValues() {
        assertRoundTrip("http://foo.com/foo?q1=v1=v2");
    }

    @Test
    void testFromUrlQueryWithEscapedChars() {
        assertRoundTrip("http://foo.com/foo?query==&%23");
    }

    @Test
    void testSimple() throws Exception {
        URL url = URL.parser().parse("http://foo.com/seg1/seg2");
        assertEquals("http", url.getScheme());
        assertEquals("foo.com", url.getHostInfo());
        assertEquals("/seg1/seg2", url.getPath());
    }

    @Test
    void testReserved() throws Exception {
        URL url = URL.parser().parse("http://foo.com/seg%2F%3B%3Fment/seg=&2");
        assertEquals("http", url.getScheme());
        assertEquals("foo.com", url.getHostInfo());
        assertEquals("/seg%2F%3B%3Fment/seg=&2", url.getPath());
    }

    @Test
    void testMatrix() throws Exception {
        URL url = URL.parser().parse("http://foo.com/;foo=bar");
        assertEquals("http", url.getScheme());
        assertEquals("foo.com", url.getHostInfo());
        assertEquals("/;foo=bar", url.getPath());
    }

    @Test
    void testMatrix2() throws Exception {
        URL url = URL.parser().parse("http://foo.com/some;p1=v1/path;p2=v2?q1=v3");
        assertEquals("http", url.getScheme());
        assertEquals("foo.com", url.getHostInfo());
        assertEquals("/some;p1=v1/path;p2=v2", url.getPath());
        Iterator<URL.PathSegment> iterator = url.getPathSegments().iterator();
        URL.PathSegment pathSegment = iterator.next();
        assertEquals("", pathSegment.getSegment());
        assertEquals("[]", pathSegment.getMatrixParams().toString());
        pathSegment = iterator.next();
        assertEquals("some", pathSegment.getSegment());
        assertEquals("p1", pathSegment.getMatrixParams().get(0).getFirst());
        assertEquals("v1", pathSegment.getMatrixParams().get(0).getSecond());
        pathSegment = iterator.next();
        assertEquals("path", pathSegment.getSegment());
        assertEquals("p2", pathSegment.getMatrixParams().get(0).getFirst());
        assertEquals("v2", pathSegment.getMatrixParams().get(0).getSecond());
        assertEquals("v3", url.getQueryParams().get("q1").get(0));
    }

    @Test
    void testAnotherQuery() throws Exception {
        URL url = URL.parser().parse("http://foo.com?foo=bar");
        assertEquals("http", url.getScheme());
        assertEquals("foo.com", url.getHostInfo());
        assertEquals("foo=bar", url.getQuery());
    }

    @Test
    void testQueryAndFragment() throws Exception {
        URL url = URL.parser().parse("http://foo.com?foo=bar#fragment");
        assertEquals("http", url.getScheme());
        assertEquals("foo.com", url.getHostInfo());
        assertEquals("foo=bar", url.getQuery());
        assertEquals("fragment", url.getFragment());
    }

    @Test
    void testRelative() throws Exception {
        URL url = URL.parser().parse("/some/path?foo=bar#fragment");
        assertNull(url.getScheme());
        assertEquals("", url.getHostInfo());
        assertEquals("/some/path", url.getPath());
        assertEquals("foo=bar", url.getQuery());
        assertEquals("fragment", url.getFragment());
        assertEquals("[foo=bar]", url.getQueryParams().toString());
    }

    @Test
    void testQueryParams() throws Exception {
        URL url = URL.parser().parse("?foo=bar");
        assertEquals("foo=bar", url.getQuery());
        assertEquals("[foo=bar]", url.getQueryParams().toString());
        assertEquals("[k1=v1, k2=v2]", URL.parseQueryString("k1=v1&k2=v2").toString());
    }

    @Test
    void testRelativeDecoded() throws Exception {
        URL url = URL.parser().parse("/foo/bar%2F?foo=b%2Far#frag%2Fment");
        assertNull(url.getScheme());
        assertEquals("", url.getHostInfo());
        assertEquals("/foo/bar/", url.getDecodedPath());
        assertEquals("foo=b/ar", url.getDecodedQuery());
        assertEquals("frag/ment", url.getDecodedFragment());
    }

    @Test
    void testFileSchemeSpecificPart() throws Exception {
        URL url = URL.parser().parse("file:foo/bar?foo=bar#fragment");
        assertEquals("", url.getHostInfo());
        assertNotNull(url.getSchemeSpecificPart());
        assertEquals("foo/bar?foo=bar#fragment", url.getSchemeSpecificPart());
    }

    @Test
    void testRelativeFilePath() throws Exception {
        URL url = URL.parser().parse("file:/foo/bar?foo=bar#fragment");
        assertEquals("file", url.getScheme());
        assertEquals("", url.getHostInfo());
        assertEquals("/foo/bar", url.getPath());
        assertEquals("foo=bar", url.getQuery());
        assertEquals("fragment", url.getFragment());
    }

    @Test
    void testAbsoluteFilePath() throws Exception {
        URL url = URL.parser().parse("file:///foo/bar?foo=bar#fragment");
        assertEquals("file", url.getScheme());
        assertEquals("", url.getHostInfo());
        assertEquals("/foo/bar", url.getPath());
        assertEquals("foo=bar", url.getQuery());
        assertEquals("fragment", url.getFragment());
    }

    @Test
    void testMoreQuery() throws Exception {
        URL url = URL.parser().parse("http://foo.com?foo=bar%26%3D%23baz&foo=bar?/2");
        assertEquals("foo=bar%26%3D%23baz&foo=bar?/2", url.getQuery());
        assertEquals("foo=bar&=#baz&foo=bar?/2", url.getDecodedQuery());
    }

    @Test
    void testAnotherPlus() throws Exception {
        URL url = URL.parser().parse("http://foo.com/has+plus;plusMtx=pl+us?plusQp=pl%2Bus#plus+frag");
        assertEquals("/has+plus;plusMtx=pl+us", url.getPath());
        assertEquals("plusQp=pl%2Bus", url.getQuery());
        assertEquals("plus+frag", url.getFragment());
    }

    @Test
    void testUserInfo() throws Exception {
        URL url = URL.parser().parse("http://foo:bar@foo.com/");
        assertEquals("foo:bar", url.getUserInfo());
        url = URL.parser().parse("http://foo:foo:bar@foo.com/");
        assertEquals("foo:foo:bar", url.getUserInfo());
        url = URL.parser().parse("http://foo:foo%3Abar@foo.com/");
        assertEquals("foo:foo:bar", url.getUserInfo());
        assertEquals("foo", url.getUser());
        assertEquals("foo:bar", url.getPassword());
    }

    @Test
    void testCharset() throws Exception {
        // default parser uses UTF-8
        Assertions.assertThrows(URLSyntaxException.class, () -> {
            String string = "http%3A%2F%2Flibrary.fes.de%2Flibrary%2Fjournals%2Fde-part%2Fdas-rote-bl%E4ttla%2Findex.html";
            URL url = URL.parser().parse(string);
        });
        String string = "http%3A%2F%2Flibrary.fes.de%2Flibrary%2Fjournals%2Fde-part%2Fdas-rote-bl%E4ttla%2Findex.html";
        URL url = URL.parser(StandardCharsets.ISO_8859_1, CodingErrorAction.REPLACE).parse(string);
        assertEquals("http://library.fes.de/library/journals/de-part/das-rote-blättla/index.html", url.toString());
    }

    @Test
    void testPathQueryFragmentFromPath(){
        URL url = URL.builder()
                .path("/a/b?c=d#e")
                .build();
        assertEquals("/a/b", url.getPath());
        assertEquals("c=d", url.getQuery());
        assertEquals("e", url.getFragment());
    }

    private void assertUrlCompatibility(String url) throws Exception {
        String s = URL.from(url).toExternalForm();
        assertEquals(s, URL.from(s).toExternalForm());
        assertEquals(s, new java.net.URL(url).toExternalForm());
    }

    private void assertRoundTrip(String url) {
        String s = URL.from(url).toExternalForm();
        assertEquals(s, URL.from(s).toExternalForm());
    }
}
