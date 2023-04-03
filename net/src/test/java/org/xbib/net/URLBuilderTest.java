package org.xbib.net;

import org.junit.jupiter.api.Test;

import java.net.SocketException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class URLBuilderTest {

    @Test
    void testNoUrlParts() {
        assertUrl(URL.http().resolveFromHost("foo.com").toUrlString(), "http://foo.com");
    }

    @Test
    void testWithPort() {
        assertUrl(URL.http().resolveFromHost("foo.com").port(33).toUrlString(), "http://foo.com:33");
    }

    @Test
    void testSimplePath() {
        assertUrl(URL.http().resolveFromHost("foo.com")
                        .pathSegment("seg1")
                        .pathSegment("seg2")
                        .toUrlString(),
                "http://foo.com/seg1/seg2");
    }

    @Test
    void testPathWithReserved() {
        // RFC 1738 S3.3
        assertUrl(URL.http().resolveFromHost("foo.com")
                .pathSegment("seg/;?ment")
                .pathSegment("seg=&2")
                .toUrlString(), "http://foo.com/seg%2F%3B%3Fment/seg=&2");
    }

    @Test
    void testPathSegments() {
        assertUrl(URL.http().resolveFromHost("foo.com")
                .pathSegments("seg1", "seg2", "seg3")
                .toUrlString(), "http://foo.com/seg1/seg2/seg3");
    }

    @Test
    void testMatrixWithReserved() {
        assertUrl(URL.http().resolveFromHost("foo.com")
                .pathSegment("foo")
                .matrixParam("foo", "bar")
                .matrixParam("res;=?#/erved", "value")
                .pathSegment("baz")
                .toUrlString(), "http://foo.com/foo;foo=bar;res%3B%3D%3F%23%2Ferved=value/baz");
    }

    @Test
    void testUrlEncodedPathSegmentUtf8() {
        assertUrl(URL.http().resolveFromHost("foo.com")
                .pathSegment("snowman").pathSegment("\u2603")
                .toUrlString(), "http://foo.com/snowman/%E2%98%83");
    }

    @Test
    void testUrlEncodedPathSegmentUtf8SurrogatePair() {
        assertUrl(URL.http().resolveFromHost("foo.com")
                .pathSegment("clef").pathSegment("\ud834\udd1e")
                .toUrlString(), "http://foo.com/clef/%F0%9D%84%9E");
    }

    @Test
    void testQueryParamNoPath() {
        assertUrl(URL.http().resolveFromHost("foo.com")
                .queryParam("foo", "bar")
                .toUrlString(), "http://foo.com?foo=bar");
    }

    @Test
    void testQueryParamEncodedWithSpaces() {
        assertUrl(URL.http()
                .resolveFromHost("foo.com")
                .queryParam(" foo ", " bar ")
                .build()
                .toExternalForm(), "http://foo.com?%20foo%20=%20bar%20");
    }

    @Test
    void testFromUrlQueryParamEncodedWithSpaces() {
        assertUrl(URL.from("http://foo.com? foo = bar ")
                .toExternalForm(), "http://foo.com?%20foo%20=%20bar%20");
    }

    @Test
    void testQueryParamsDuplicated() {
        assertUrl(URL.http().resolveFromHost("foo.com")
                .queryParam("foo", "bar")
                .queryParam("foo", "bar2")
                .queryParam("baz", "quux")
                .queryParam("baz", "quux2")
                .toUrlString(), "http://foo.com?foo=bar&foo=bar2&baz=quux&baz=quux2");
    }

    @Test
    void testEncodeQueryParams() {
        assertUrl(URL.http().resolveFromHost("foo.com")
                .queryParam("foo", "bar&=#baz")
                .queryParam("foo", "bar?/2")
                .toUrlString(), "http://foo.com?foo=bar%26%3D%23baz&foo=bar?/2");
    }

    @Test
    void testEncodeQueryParamWithSpaceAndPlus() {
        assertUrl(URL.http().resolveFromHost("foo.com")
                .queryParam("foo", "spa ce")
                .queryParam("fo+o", "plus+")
                .toUrlString(), "http://foo.com?foo=spa%20ce&fo%2Bo=plus%2B");
    }

    @Test
    void testPlusInVariousParts() {
        assertUrl(URL.http().resolveFromHost("foo.com")
                .pathSegment("has+plus")
                .matrixParam("plusMtx", "pl+us")
                .queryParam("plusQp", "pl+us")
                .fragment("plus+frag")
                .toUrlString(), "http://foo.com/has+plus;plusMtx=pl+us?plusQp=pl%2Bus#plus+frag");
    }

    @Test
    void testFragment() {
        assertUrl(URL.http().resolveFromHost("foo.com")
                .queryParam("foo", "bar")
                .fragment("#frag/?")
                .toUrlString(), "http://foo.com?foo=bar#%23frag/?");
    }

    @Test
    void testAllParts() {
        assertUrl(URL.https().resolveFromHost("foobar.com").port(3333)
                .pathSegment("foo")
                .pathSegment("bar")
                .matrixParam("mtx1", "val1")
                .matrixParam("mtx2", "val2")
                .queryParam("q1", "v1")
                .queryParam("q2", "v2")
                .fragment("zomg it's a fragment")
                .toUrlString(),
                "https://foobar.com:3333/foo/bar;mtx1=val1;mtx2=val2?q1=v1&q2=v2#zomg%20it's%20a%20fragment");
    }

    @Test
    void testSlashHost() {
        URL.http().resolveFromHost("/").toUrlString();
    }

    @Test
    void testGoogle() {
        URL url = URL.https().resolveFromHost("google.com").build();
        assertEquals("https://google.com", url.toString());
    }

    @Test
    void testBadIPv4LiteralDoesntChoke() {
        assertUrl(URL.http().resolveFromHost("300.100.50.1")
                .toUrlString(), "http://300.100.50.1");
    }

    @Test
    void testIPv4Literal() throws SocketException {
        String s = URL.http().resolveFromHost("127.0.0.1").toUrlString();
        if (!NetworkUtils.isIpv6Active()) {
            assertUrl(s, "http://localhost");
        } else {
            // we assume entry
            // 127.0.0.1   localhost localhost.localdomain localhost4 localhost4.localdomain4
            // in /etc/hosts
            assertEquals("http://localhost", s);
        }
    }

    @Test
    void testIPv6Literal() throws SocketException {
        if (NetworkUtils.isIpv6Active()) {
            String s = URL.http().resolveFromHost("[2001:db8:85a3::8a2e:370:7334]").toUrlString();
            assertEquals("http://[2001:db8:85a3:0:0:8a2e:370:7334]", s);
        }
    }

    @Test
    void testEncodedRegNameSingleByte() {
        String s = URL.http().resolveFromHost("host?name;").toUrlString();
        assertEquals("http://host%3Fname;", s);
    }

    @Test
    void testEncodedRegNameMultiByte() {
        String s = URL.http().host("snow\u2603man")
                .toUrlString();
        assertEquals("http://snow%E2%98%83man", s);
    }

    @Test
    void testThreePathSegments() {
        String s = URL.https().resolveFromHost("foo.com")
                .pathSegments("a", "b", "c")
                .toUrlString();
        assertEquals("https://foo.com/a/b/c", s);
    }

    @Test
    void testThreePathSegmentsWithQueryParams() {
        String s = URL.https().resolveFromHost("foo.com")
                .pathSegments("a", "b", "c")
                .queryParam("foo", "bar")
                .toUrlString();
        assertEquals("https://foo.com/a/b/c?foo=bar", s);
    }

    @Test
    void testIntermingledMatrixParamsAndPathSegments() {
        String s = URL.http().resolveFromHost("foo.com")
                .pathSegments("seg1", "seg2")
                .matrixParam("m1", "v1")
                .pathSegment("seg3")
                .matrixParam("m2", "v2")
                .toUrlString();
        assertEquals("http://foo.com/seg1/seg2;m1=v1/seg3;m2=v2", s);
    }

    @Test
    void testUseQueryParamAfterQuery() {
        String s = URL.http().resolveFromHost("foo.com")
                .query("q")
                .queryParam("foo", "bar")
                .toUrlString();
        assertEquals("http://foo.com?q", s);
    }

    @Test
    void testUseQueryAfterQueryParam() {
        String s = URL.http().resolveFromHost("foo.com")
                .queryParam("foo", "bar")
                .query("q")
                .toUrlString();
        assertEquals("http://foo.com?q", s);
    }

    @Test
    void testQueryWithNoSpecialChars() {
        String s = URL.http().resolveFromHost("foo.com")
                .query("q")
                .toUrlString();
        assertEquals("http://foo.com?q", s);
    }

    @Test
    void testQueryWithOkSpecialChars() {
        String s = URL.http().resolveFromHost("foo.com")
                .query("q?/&=").toUrlString();
        assertEquals("http://foo.com?q?/&=", s);
    }

    @Test
    void testQueryWithEscapedSpecialChars() {
        String s = URL.http().resolveFromHost("foo.com")
                .query("q#+").toUrlString();
        assertEquals("http://foo.com?q#+", s);
    }

    @Test
    void testMutator() {
        URLBuilder builder = URL.from("http://google.com:8008/foobar").mutator();
        builder.queryParam("a%", "b%");
        builder.queryParam("c", " d ");
        builder.scheme("https");
        URL url = builder.build();
        assertEquals("/foobar?a%25=b%25&c=%20d%20", url.relativeReference());
        assertEquals("https://google.com:8008/foobar?a%25=b%25&c=%20d%20", url.toExternalForm());
        assertEquals("https://google.com:8008/foobar?a%=b%&c= d ", url.toString());
    }

    @Test
    void testNewBuilder() {
        URL url = URL.from("http://google.com:8008/foobar");
        URLBuilder builder = url.newBuilder()
                .scheme(url.getScheme())
                .schemeSpecificPart(url.getSchemeSpecificPart());
        assertEquals("http://google.com:8008/foobar", builder.build().toString());
    }

    @Test
    void testUserInfo(){
        String s = URL.http().userInfo("foo:bar").host("foo.com").toUrlString();
        assertEquals("http://foo:bar@foo.com", s);
        s = URL.http().userInfo("foo:foo:bar").host("foo.com").toUrlString();
        assertEquals("http://foo:foo:bar@foo.com", s);
        s = URL.http().userInfo("foo:foo%3Abar").host("foo.com").toUrlString();
        assertEquals("http://foo:foo%3Abar@foo.com", s);
        s = URL.http().userInfo("foo", "foo:bar").host("foo.com").toUrlString();
        assertEquals("http://foo:foo%3Abar@foo.com", s);
    }

    private void assertUrl(String urlString, String expected) {
        assertEquals(expected, urlString);
        assertEquals(expected, URL.from(urlString).toExternalForm());
    }
}
