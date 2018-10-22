package org.xbib.net;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

import java.net.URI;
import java.nio.charset.MalformedInputException;
import java.nio.charset.UnmappableCharacterException;

/**
 */
public class URLResolverTest {

    @Test
    public void testResolveURI() throws Exception {
        URI base = URI.create("http://example.org/foo");
        assertEquals("http://example.org/", base.resolve("/").toString());
        assertEquals("http://example.org/foobar", base.resolve("/foobar").toString());
        assertEquals("http://example.org/foobar", base.resolve("foobar").toString());
        base = URI.create("http://example.org/foo/");
        assertEquals("http://example.org/", base.resolve("/").toString());
        assertEquals("http://example.org/foobar", base.resolve("/foobar").toString());
        assertEquals("http://example.org/foo/foobar", base.resolve("foobar").toString());
    }

    @Test
    public void testResolveURL() throws Exception {
        URL base = URL.create("http://example.org/foo");
        assertEquals("http://example.org/", base.resolve("/").toString());
        assertEquals("http://example.org/foobar", base.resolve("/foobar").toString());
        assertEquals("http://example.org/foobar", base.resolve("foobar").toString());
        base = URL.create("http://example.org/foo/");
        assertEquals("http://example.org/", base.resolve("/").toString());
        assertEquals("http://example.org/foobar", base.resolve("/foobar").toString());
        assertEquals("http://example.org/foo/foobar", base.resolve("foobar").toString());
    }

    @Test
    public void testMultiResolve() throws Exception {
        URL base = URL.create("http://example:8080");
        String pathSpec = "foobar/";
        String index = "index.html";
        String queryString = "a=b";
        URL url = base.resolve(pathSpec).resolve(index).newBuilder().query(queryString).build().normalize();
        assertEquals("http://example:8080/foobar/index.html?a=b", url.toString());
    }

    @Test
    public void testFielding() throws Exception {
        // http://www.ics.uci.edu/~fielding/url/test1.html
        resolve("http://a/b/c/d;p?q", "g:h", "g:h");
        resolve("http://a/b/c/d;p?q", "g", "http://a/b/c/g");
        resolve("http://a/b/c/d;p?q", "./g", "http://a/b/c/g");
        resolve("http://a/b/c/d;p?q", "g/", "http://a/b/c/g/");
        resolve("http://a/b/c/d;p?q", "/g", "http://a/g");
        resolve("http://a/b/c/d;p?q", "//g", "http://g");
        resolve("http://a/b/c/d;p?q", "?y", "http://a/b/c/d;p?y");
        resolve("http://a/b/c/d;p?q", "g?y", "http://a/b/c/g?y");
        resolve("http://a/b/c/d;p?q", "#s", "http://a/b/c/d;p?q#s");
        resolve("http://a/b/c/d;p?q", "g#s", "http://a/b/c/g#s");
        resolve("http://a/b/c/d;p?q", "g?y#s", "http://a/b/c/g?y#s");
        resolve("http://a/b/c/d;p?q", ";x", "http://a/b/c/;x");
        resolve("http://a/b/c/d;p?q", "g;x", "http://a/b/c/g;x");
        resolve("http://a/b/c/d;p?q", "g;x?y#s", "http://a/b/c/g;x?y#s");
        resolve("http://a/b/c/d;p?q", ".", "http://a/b/c/");
        resolve("http://a/b/c/d;p?q", "./", "http://a/b/c/");
        resolve("http://a/b/c/d;p?q", "..", "http://a/b/");
        resolve("http://a/b/c/d;p?q", "../", "http://a/b/");
        resolve("http://a/b/c/d;p?q", "../g", "http://a/b/g");
        resolve("http://a/b/c/d;p?q", "../..", "http://a/");
        resolve("http://a/b/c/d;p?q", "../../", "http://a/");
        resolve("http://a/b/c/d;p?q", "../../g", "http://a/g");
        // abnormal cases
        resolve("http://a/b/c/d;p?q", "../../../g", "http://a/g");
        resolve("http://a/b/c/d;p?q", "../../../../g", "http://a/g");
        resolve("http://a/b/c/d;p?q", "/./g", "http://a/g");
        resolve("http://a/b/c/d;p?q", "/../g", "http://a/g");
        resolve("http://a/b/c/d;p?q", "g.", "http://a/b/c/g.");
        resolve("http://a/b/c/d;p?q", ".g", "http://a/b/c/.g");
        resolve("http://a/b/c/d;p?q", "g..", "http://a/b/c/g..");
        resolve("http://a/b/c/d;p?q", "..g", "http://a/b/c/..g");
        // less likely
        resolve("http://a/b/c/d;p?q", "./../g", "http://a/b/g");
        resolve("http://a/b/c/d;p?q", "./g/.", "http://a/b/c/g/");
        resolve("http://a/b/c/d;p?q", "g/./h", "http://a/b/c/g/h");
        resolve("http://a/b/c/d;p?q", "g/../h", "http://a/b/c/h");
        resolve("http://a/b/c/d;p?q", "g;x=1/./y", "http://a/b/c/g;x=1/y");
        resolve("http://a/b/c/d;p?q", "g;x=1/../y", "http://a/b/c/y");
        // query component
        resolve("http://a/b/c/d;p?q", "g?y/./x", "http://a/b/c/g?y/./x");
        resolve("http://a/b/c/d;p?q", "g?y/../x", "http://a/b/c/g?y/../x");
        // fragment component
        resolve("http://a/b/c/d;p?q", "g#s/./x", "http://a/b/c/g#s/./x");
        resolve("http://a/b/c/d;p?q", "g#s/../x", "http://a/b/c/g#s/../x");
        // scheme
        resolve("http://a/b/c/d;p?q", "http:g", "http:g");
        resolve("http://a/b/c/d;p?q", "http:", "http:");
        // absolute
        resolve("http://a/b/c/d;p?q", "http://e/f/g/h", "http://e/f/g/h");
    }

    private void resolve(String inputBase, String spec, String expected)
            throws URLSyntaxException, MalformedInputException, UnmappableCharacterException {
        assertEquals(expected, URL.base(inputBase).resolve(spec).toExternalForm());
    }
}
