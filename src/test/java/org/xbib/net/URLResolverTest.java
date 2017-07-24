package org.xbib.net;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 */
public class URLResolverTest {

    @Test
    public void testResolve() throws Exception {
        URL base = URL.create("http://example.org/foo/");
        assertEquals("http://example.org/", base.resolve("/").toString());
        resolve("http://foo.bar", "foobar", "http://foo.bar/foobar");
        resolve("http://foo.bar/", "foobar", "http://foo.bar/foobar");
        resolve("http://foo.bar/foobar", "foobar", "http://foo.bar/foobar");
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
    }

    private void resolve(String inputBase, String relative, String expected) {
        assertEquals(expected, URL.base(inputBase).resolve(relative).toExternalForm());
    }
}
