package org.xbib.net;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import org.junit.jupiter.api.Test;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.fail;

class URLTest {

    @Test
    void test() throws Exception {
       List<JsonTest> tests = readTests(fromResource("/urltestdata.json"));
       for (JsonTest test : tests) {
           String base = test.base;
           String input = test.input;
           if (test.skip) {
               continue;
           }
           if (test.failure) {
               try {
                   URL.base(base).resolve(input);
                   fail("base = " + base + " input = " + input);
               } catch (Exception e) {
                   // pass
               }
           } else {
               if (base != null && input != null) {
                   try {
                       URL url = URL.base(base).resolve(input);
                       if (test.protocol != null) {
                           assertEquals(test.protocol, url.getScheme() + ":");
                       }
                       if (test.hostname != null) {
                           // default in Mac OS
                           String host = url.getHost();
                           if ("broadcasthost".equals(host)) {
                               host = "255.255.255.255";
                           }
                           assertEquals(test.hostname, host);
                       }
                       if (test.port != null && !test.port.isEmpty() && url.getPort() != null) {
                           assertEquals(Integer.parseInt(test.port), (int) url.getPort());
                       }
                       // TODO(jprante)
                       //if (test.pathname != null && !test.pathname.isEmpty() && url.getPath() != null) {
                       //    assertEquals(test.pathname, url.getPath());
                       //}
                       //System.err.println("passed: " + base + " " + input);
                   } catch (URLSyntaxException e) {
                       //System.err.println("unable to resolve: " + base + " " + input + " reason: " + e.getMessage());
                   }
               }
           }
       }
    }

    private JsonNode fromResource(String path) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ObjectReader reader = mapper.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, true)
                .readerFor(JsonNode.class);
        return reader.readValue(getClass().getResourceAsStream(path));
    }

    private List<JsonTest> readTests(JsonNode jsonNode) {
        List<JsonTest> list = new ArrayList<>();
        for (JsonNode n : jsonNode) {
            if (n.isObject()) {
                JsonTest jsontest = new JsonTest();
                jsontest.input = get(n, "input");
                jsontest.base = get(n, "base");
                jsontest.href = get(n, "href");
                jsontest.origin = get(n, "origin");
                jsontest.protocol = get(n, "protocol");
                jsontest.username = get(n, "username");
                jsontest.password = get(n, "password");
                jsontest.host = get(n, "host");
                jsontest.hostname = get(n, "hostname");
                jsontest.port = get(n, "port");
                jsontest.pathname = get(n, "pathname");
                jsontest.search = get(n, "search");
                jsontest.hash = get(n, "hash");
                jsontest.failure = n.has("failure");
                jsontest.skip = n.has("skip");
                list.add(jsontest);
            }
        }
        return list;
    }

    private String get(JsonNode n, String key) {
        return n.has(key) ? n.get(key).textValue() : null;
    }

    static class JsonTest {
        String input;
        String base;
        String href;
        String origin;
        String protocol;
        String username;
        String password;
        String host;
        String hostname;
        String port;
        String pathname;
        String search;
        String hash;
        boolean failure;
        boolean skip;
    }

    @Test
    void testIpv4() {
        URL iri = URL.create("http://127.0.0.1");
        assertEquals("http://127.0.0.1", iri.toExternalForm());
    }

    @Test
    void testIpv6() {
        URL iri = URL.from("http://[2001:0db8:85a3:08d3:1319:8a2e:0370:7344]");
        assertEquals(iri.getProtocolVersion(), ProtocolVersion.IPV6);
        assertEquals("http://[2001:db8:85a3:8d3:1319:8a2e:370:7344]", iri.toString());
    }

    @Test
    void testIpv6Invalid() {
        URL iri = URL.from("http://[2001:0db8:85a3:08d3:1319:8a2e:0370:734o]");
        assertEquals("http://2001:0db8:85a3:08d3:1319:8a2e:0370:734o", iri.toString());
    }

    @Test
    void testSimple() {
        URL iri = URL.create("http://validator.w3.org/check?uri=http%3A%2F%2Fr\u00E9sum\u00E9.example.org");
        assertEquals("http://validator.w3.org/check?uri=http%3A%2F%2Fr\u00E9sum\u00E9.example.org", iri.toString());
    }

    @Test
    void testFile() throws Exception {
        URL iri = URL.create("file:///tmp/test/foo");
        assertEquals("", iri.getHost());
        assertEquals("/tmp/test/foo", iri.getPath());
        assertEquals("file:///tmp/test/foo", iri.toExternalForm());
        assertEquals("file:///tmp/test/foo", iri.toString());
    }

    @Test
    void testSimple2() throws Exception {
        URL iri = URL.create("http://www.example.org/red%09ros\u00E9#red");
        assertEquals("http://www.example.org/red%09ros%C3%A9#red", iri.toExternalForm());
    }

    @Test
    void testNotSoSimple() throws Exception {
        URL iri = URL.create("http://example.com/\uD800\uDF00\uD800\uDF01\uD800\uDF02");
        assertEquals("http://example.com/%F0%90%8C%80%F0%90%8C%81%F0%90%8C%82", iri.toExternalForm());
    }

    @Test
    void testIRItoURI() throws Exception {
        URL iri = URL.from("http://\u7D0D\u8C46.example.org/%E2%80%AE");
        assertEquals("http://xn--99zt52a.example.org/%E2%80%AE", iri.toExternalForm());
    }

    @Test
    void testComparison() {
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
    void testUCN() {
        URL iri1 = URL.create("http://www.example.org/r\u00E9sum\u00E9.html");
        String s = Normalizer.normalize("http://www.example.org/re\u0301sume\u0301.html", Normalizer.Form.NFC);
        URL iri2 = URL.create(s);
        assertEquals(iri2, iri1);
    }

    @Test
    void testNormalizePath() {
        URL iri1 = URL.create("http://example.org/%7e%2Fuser%2f");
        URL iri2 = URL.create("http://example.org/%7E%2fuser/");
        assertEquals(iri1.normalize(), iri2.normalize());
    }

    @Test
    void testIDN() {
        URL iri1 = URL.from("http://r\u00E9sum\u00E9.example.org");
        assertEquals("xn--rsum-bpad.example.org", iri1.getHost());
    }

    @Test
    void testResolveRelative() {
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
    void testSchemes() {
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
