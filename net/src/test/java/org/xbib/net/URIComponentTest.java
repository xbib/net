package org.xbib.net;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;

class URIComponentTest {

    @Test
    void testURI() {
        URI uri = URI.create("ftp://user:pass@host:1234/path/to/filename.txt");
        assertEquals("ftp", scheme(uri));
        assertEquals("user", user(uri));
        assertEquals("pass", pass(uri));
        assertEquals("host", host(uri));
        assertEquals(1234, port(uri));
        assertEquals("/path/to/", parent(uri));
        assertEquals("filename.txt", filename(uri));
    }

    @Test
    void testURI2() {
        URI uri = URI.create("sftp://user:pass@host:1234/filename.txt");
        assertEquals("sftp", scheme(uri));
        assertEquals("user", user(uri));
        assertEquals("pass", pass(uri));
        assertEquals("host", host(uri));
        assertEquals(1234, port(uri));
        assertEquals("/", parent(uri));
        assertEquals("filename.txt", filename(uri));
    }

    private static String scheme(URI uri) {
        return uri.getScheme();
    }

    private static String user(URI uri) {
        String auth = uri.getAuthority();
        return auth != null ? auth.split(":")[0] : null;
    }

    private static String pass(URI uri) {
        String auth = uri.getAuthority();
        return auth != null ? auth.split("@")[0].split(":")[1] : null;
    }

    private static String host(URI uri) {
        return uri.getHost();
    }

    private static int port(URI uri) {
        return uri.getPort();
    }

    private static String parent(URI uri) {
       return uri.resolve(".").getPath();
    }

    private static String filename(URI uri) {
        String path = uri.getPath();
        int pos = path.lastIndexOf('/');
        return pos >= 0 ? path.substring(pos + 1) : path;
    }
}
