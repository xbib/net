package org.xbib.net.path.spring;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PathMatcherTest {

    @Test
    void isMatchWithCaseSensitiveWithDefaultPathSeparator() {

        final PathPatternParser pathMatcher = new PathPatternParser();

        // test exact matching
        assertTrue(pathMatcher.match("test", "test"));
        assertTrue(pathMatcher.match("/test", "/test"));
        assertTrue(pathMatcher.match("http://example.org", "http://example.org")); // SPR-14141
        assertFalse(pathMatcher.match("/test.jpg", "test.jpg"));
        assertFalse(pathMatcher.match("test", "/test"));
        assertFalse(pathMatcher.match("/test", "test"));

        // test matching with ?'s
        assertTrue(pathMatcher.match("t?st", "test"));
        assertTrue(pathMatcher.match("??st", "test"));
        assertTrue(pathMatcher.match("tes?", "test"));
        assertTrue(pathMatcher.match("te??", "test"));
        assertTrue(pathMatcher.match("?es?", "test"));
        assertFalse(pathMatcher.match("tes?", "tes"));
        assertFalse(pathMatcher.match("tes?", "testt"));
        assertFalse(pathMatcher.match("tes?", "tsst"));

        // test matching with *'s
        assertTrue(pathMatcher.match("*", "test"));
        assertTrue(pathMatcher.match("test*", "test"));
        assertTrue(pathMatcher.match("test*", "testTest"));
        assertTrue(pathMatcher.match("test/*", "test/Test"));
        assertTrue(pathMatcher.match("test/*", "test/t"));
        assertTrue(pathMatcher.match("test/*", "test/"));
        assertTrue(pathMatcher.match("*test*", "AnothertestTest"));
        assertTrue(pathMatcher.match("*test", "Anothertest"));
        assertTrue(pathMatcher.match("*.*", "test."));
        assertTrue(pathMatcher.match("*.*", "test.test"));
        assertTrue(pathMatcher.match("*.*", "test.test.test"));
        assertTrue(pathMatcher.match("test*aaa", "testblaaaa"));
        assertFalse(pathMatcher.match("test*", "tst"));
        assertFalse(pathMatcher.match("test*", "tsttest"));
        //assertFalse(pathMatcher.match("test*", "test/"));
        assertFalse(pathMatcher.match("test*", "test/t"));
        assertFalse(pathMatcher.match("test/*", "test"));
        assertFalse(pathMatcher.match("*test*", "tsttst"));
        assertFalse(pathMatcher.match("*test", "tsttst"));
        assertFalse(pathMatcher.match("*.*", "tsttst"));
        assertFalse(pathMatcher.match("test*aaa", "test"));
        assertFalse(pathMatcher.match("test*aaa", "testblaaab"));

        // test matching with ?'s and /'s
        assertTrue(pathMatcher.match("/?", "/a"));
        assertTrue(pathMatcher.match("/?/a", "/a/a"));
        assertTrue(pathMatcher.match("/a/?", "/a/b"));
        assertTrue(pathMatcher.match("/??/a", "/aa/a"));
        assertTrue(pathMatcher.match("/a/??", "/a/bb"));
        assertTrue(pathMatcher.match("/?", "/a"));

        // test matching with **'s
        assertTrue(pathMatcher.match("/**", "/testing/testing"));
        assertTrue(pathMatcher.match("/*/**", "/testing/testing"));
        //assertTrue(pathMatcher.match("/**/*", "/testing/testing"));
        //assertTrue(pathMatcher.match("/bla/**/bla", "/bla/testing/testing/bla"));
        //assertTrue(pathMatcher.match("/bla/**/bla", "/bla/testing/testing/bla/bla"));
        //assertTrue(pathMatcher.match("/**/test", "/bla/bla/test"));
        //assertTrue(pathMatcher.match("/bla/**/**/bla", "/bla/bla/bla/bla/bla/bla"));
        assertTrue(pathMatcher.match("/bla*bla/test", "/blaXXXbla/test"));
        assertTrue(pathMatcher.match("/*bla/test", "/XXXbla/test"));
        assertFalse(pathMatcher.match("/bla*bla/test", "/blaXXXbl/test"));
        assertFalse(pathMatcher.match("/*bla/test", "XXXblab/test"));
        assertFalse(pathMatcher.match("/*bla/test", "XXXbl/test"));

        assertFalse(pathMatcher.match("/????", "/bala/bla"));
        //assertFalse(pathMatcher.match("/**/*bla", "/bla/bla/bla/bbb"));

       // assertTrue(pathMatcher.match("/*bla*/**/bla/**", "/XXXblaXXXX/testing/testing/bla/testing/testing/"));
       // assertTrue(pathMatcher.match("/*bla*/**/bla/*", "/XXXblaXXXX/testing/testing/bla/testing"));
       // assertTrue(pathMatcher.match("/*bla*/**/bla/**", "/XXXblaXXXX/testing/testing/bla/testing/testing"));
       // assertTrue(pathMatcher.match("/*bla*/**/bla/**", "/XXXblaXXXX/testing/testing/bla/testing/testing.jpg"));

       // assertTrue(pathMatcher.match("*bla*/**/bla/**", "XXXblaXXXX/testing/testing/bla/testing/testing/"));
       // assertTrue(pathMatcher.match("*bla*/**/bla/*", "XXXblaXXXX/testing/testing/bla/testing"));
      //  assertTrue(pathMatcher.match("*bla*/**/bla/**", "XXXblaXXXX/testing/testing/bla/testing/testing"));
      //  assertFalse(pathMatcher.match("*bla*/**/bla/*", "XXXblaXXXX/testing/testing/bla/testing/testing"));

        //assertFalse(pathMatcher.match("/x/x/**/bla", "/x/x/x/"));

        assertTrue(pathMatcher.match("/foo/bar/**", "/foo/bar"));

        assertTrue(pathMatcher.match("", ""));

        assertTrue(pathMatcher.match("/foo/bar/**", "/foo/bar"));
        assertTrue(pathMatcher.match("/resource/1", "/resource/1"));
        assertTrue(pathMatcher.match("/resource/*", "/resource/1"));
        assertTrue(pathMatcher.match("/resource/*/", "/resource/1/"));
        assertTrue(pathMatcher.match("/top-resource/*/resource/*/sub-resource/*", "/top-resource/1/resource/2/sub-resource/3"));
        assertTrue(pathMatcher.match("/top-resource/*/resource/*/sub-resource/*", "/top-resource/999999/resource/8888888/sub-resource/77777777"));
        assertTrue(pathMatcher.match("/*/*/*/*/secret.html", "/this/is/protected/path/secret.html"));
        assertTrue(pathMatcher.match("/*/*/*/*/*.html", "/this/is/protected/path/secret.html"));
        assertFalse(pathMatcher.match("*.html", "/this/is/protected/path/secret.html"));
        assertTrue(pathMatcher.match("/*/*/*/*", "/this/is/protected/path"));
        assertFalse(pathMatcher.match("/*.txt", "/path/my.txt"));
        //assertTrue(pathMatcher.match("org/springframework/**/*.jsp", "org/springframework/web/views/hello.jsp"));
        //assertTrue(pathMatcher.match("org/springframework/**/*.jsp", "org/springframework/web/default.jsp"));
        //assertTrue(pathMatcher.match("org/springframework/**/*.jsp", "org/springframework/default.jsp"));
        //assertTrue(pathMatcher.match("org/**/servlet/bla.jsp", "org/springframework/servlet/bla.jsp"));
        //assertTrue(pathMatcher.match("org/**/servlet/bla.jsp", "org/springframework/testing/servlet/bla.jsp"));
        //assertTrue(pathMatcher.match("org/**/servlet/bla.jsp", "org/servlet/bla.jsp"));
        //assertTrue(pathMatcher.match("**/hello.jsp", "org/springframework/servlet/web/views/hello.jsp"));
        //assertTrue(pathMatcher.match("**/**/hello.jsp", "org/springframework/servlet/web/views/hello.jsp"));

        assertFalse(pathMatcher.match("/foo/bar/**", "/foo /bar"));
        assertFalse(pathMatcher.match("/foo/bar/**", "/foo          /bar"));
        assertFalse(pathMatcher.match("/foo/bar/**", "/foo          /               bar"));
        assertFalse(pathMatcher.match("/foo/bar/**", "       /      foo          /               bar"));
        //assertFalse(pathMatcher.match("org/**/servlet/bla.jsp", "   org   /      servlet    /   bla   .   jsp"));
    }

    @Disabled("no custom path separator")
    @Test
    public void isMatchWithCustomSeparator() throws Exception {
        final PathPatternParser pathMatcher = new PathPatternParser();
        //pathMatcher.withPathSeparator('.');

        assertTrue(pathMatcher.match(".foo.bar.**", ".foo.bar"));
        assertTrue(pathMatcher.match(".resource.1", ".resource.1"));
        assertTrue(pathMatcher.match(".resource.*", ".resource.1"));
        assertTrue(pathMatcher.match(".resource.*.", ".resource.1."));
        assertTrue(pathMatcher.match("org.springframework.**.*.jsp", "org.springframework.web.views.hello.jsp"));
        assertTrue(pathMatcher.match("org.springframework.**.*.jsp", "org.springframework.web.default.jsp"));
        assertTrue(pathMatcher.match("org.springframework.**.*.jsp", "org.springframework.default.jsp"));
        assertTrue(pathMatcher.match("org.**.servlet.bla.jsp", "org.springframework.servlet.bla.jsp"));
        assertTrue(pathMatcher.match("org.**.servlet.bla.jsp", "org.springframework.testing.servlet.bla.jsp"));
        assertTrue(pathMatcher.match("org.**.servlet.bla.jsp", "org.servlet.bla.jsp"));
        assertTrue(pathMatcher.match("http://example.org", "http://example.org"));
        assertTrue(pathMatcher.match("**.hello.jsp", "org.springframework.servlet.web.views.hello.jsp"));
        assertTrue(pathMatcher.match("**.**.hello.jsp", "org.springframework.servlet.web.views.hello.jsp"));

        // test matching with ?'s and .'s
        assertTrue(pathMatcher.match(".?", ".a"));
        assertTrue(pathMatcher.match(".?.a", ".a.a"));
        assertTrue(pathMatcher.match(".a.?", ".a.b"));
        assertTrue(pathMatcher.match(".??.a", ".aa.a"));
        assertTrue(pathMatcher.match(".a.??", ".a.bb"));
        assertTrue(pathMatcher.match(".?", ".a"));

        // test matching with **'s
        assertTrue(pathMatcher.match(".**", ".testing.testing"));
        assertTrue(pathMatcher.match(".*.**", ".testing.testing"));
        assertTrue(pathMatcher.match(".**.*", ".testing.testing"));
        assertTrue(pathMatcher.match(".bla.**.bla", ".bla.testing.testing.bla"));
        assertTrue(pathMatcher.match(".bla.**.bla", ".bla.testing.testing.bla.bla"));
        assertTrue(pathMatcher.match(".**.test", ".bla.bla.test"));
        assertTrue(pathMatcher.match(".bla.**.**.bla", ".bla.bla.bla.bla.bla.bla"));
        assertFalse(pathMatcher.match(".bla*bla.test", ".blaXXXbl.test"));
        assertFalse(pathMatcher.match(".*bla.test", "XXXblab.test"));
        assertFalse(pathMatcher.match(".*bla.test", "XXXbl.test"));
    }

    @Disabled("no ignore case")
    @Test
    public void isMatchWithIgnoreCase() throws Exception {
        final PathPatternParser pathMatcher = new PathPatternParser();
        // withIgnoreCase();

        assertTrue(pathMatcher.match("/foo/bar/**", "/FoO/baR"));
        assertTrue(pathMatcher.match("org/springframework/**/*.jsp", "ORG/SpringFramework/web/views/hello.JSP"));
        assertTrue(pathMatcher.match("org/**/servlet/bla.jsp", "Org/SERVLET/bla.jsp"));
        assertTrue(pathMatcher.match("/?", "/A"));
        assertTrue(pathMatcher.match("/?/a", "/a/A"));
        assertTrue(pathMatcher.match("/a/??", "/a/Bb"));
        assertTrue(pathMatcher.match("/?", "/a"));
        assertTrue(pathMatcher.match("/**", "/testing/teSting"));
        assertTrue(pathMatcher.match("/*/**", "/testing/testing"));
        assertTrue(pathMatcher.match("/**/*", "/tEsting/testinG"));
        assertTrue(pathMatcher.match("http://example.org", "HtTp://exAmple.org"));
        assertTrue(pathMatcher.match("HTTP://EXAMPLE.ORG", "HtTp://exAmple.org"));
    }

    @Disabled("no ignore case, no custom separator")
    @Test
    public void isMatchWithIgnoreCaseWithCustomPathSeparator() throws Exception {
        final PathPatternParser pathMatcher = new PathPatternParser();
        //final AntPathMatcher pathMatcher = AntPathMatcher.builder()
        //        .withIgnoreCase()
        //        .withPathSeparator('.').build();

        assertTrue(pathMatcher.match(".foo.bar.**", ".FoO.baR"));
        assertTrue(pathMatcher.match("org.springframework.**.*.jsp", "ORG.SpringFramework.web.views.hello.JSP"));
        assertTrue(pathMatcher.match("org.**.servlet.bla.jsp", "Org.SERVLET.bla.jsp"));
        assertTrue(pathMatcher.match(".?", ".A"));
        assertTrue(pathMatcher.match(".?.a", ".a.A"));
        assertTrue(pathMatcher.match(".a.??", ".a.Bb"));
        assertTrue(pathMatcher.match(".?", ".a"));
        assertTrue(pathMatcher.match(".**", ".testing.teSting"));
        assertTrue(pathMatcher.match(".*.**", ".testing.testing"));
        assertTrue(pathMatcher.match(".**.*", ".tEsting.testinG"));
        assertTrue(pathMatcher.match("http:..example.org", "HtTp:..exAmple.org"));
        assertTrue(pathMatcher.match("HTTP:..EXAMPLE.ORG", "HtTp:..exAmple.org"));
    }

    @Disabled
    @Test
    public void isMatchWithMatchStart() {
        final PathPatternParser pathMatcher = new PathPatternParser();
        //final AntPathMatcher pathMatcher = AntPathMatcher.builder().withMatchStart().build();

        // test exact matching
        assertTrue(pathMatcher.match("test", "test"));
        assertTrue(pathMatcher.match("/test", "/test"));
        assertFalse(pathMatcher.match("/test.jpg", "test.jpg"));
        assertFalse(pathMatcher.match("test", "/test"));
        assertFalse(pathMatcher.match("/test", "test"));

        // test matching with ?'s
        assertTrue(pathMatcher.match("t?st", "test"));
        assertTrue(pathMatcher.match("??st", "test"));
        assertTrue(pathMatcher.match("tes?", "test"));
        assertTrue(pathMatcher.match("te??", "test"));
        assertTrue(pathMatcher.match("?es?", "test"));
        assertFalse(pathMatcher.match("tes?", "tes"));
        assertFalse(pathMatcher.match("tes?", "testt"));
        assertFalse(pathMatcher.match("tes?", "tsst"));

        // test matching with *'s
        assertTrue(pathMatcher.match("*", "test"));
        assertTrue(pathMatcher.match("test*", "test"));
        assertTrue(pathMatcher.match("test*", "testTest"));
        assertTrue(pathMatcher.match("test/*", "test/Test"));
        assertTrue(pathMatcher.match("test/*", "test/t"));
        assertTrue(pathMatcher.match("test/*", "test/"));
        assertTrue(pathMatcher.match("*test*", "AnothertestTest"));
        assertTrue(pathMatcher.match("*test", "Anothertest"));
        assertTrue(pathMatcher.match("*.*", "test."));
        assertTrue(pathMatcher.match("*.*", "test.test"));
        assertTrue(pathMatcher.match("*.*", "test.test.test"));
        assertTrue(pathMatcher.match("test*aaa", "testblaaaa"));
        assertFalse(pathMatcher.match("test*", "tst"));
        assertFalse(pathMatcher.match("test*", "test/"));
        assertFalse(pathMatcher.match("test*", "tsttest"));
        assertFalse(pathMatcher.match("test*", "test/"));
        assertFalse(pathMatcher.match("test*", "test/t"));
        assertTrue(pathMatcher.match("test/*", "test"));
        assertTrue(pathMatcher.match("test/t*.txt", "test"));
        assertFalse(pathMatcher.match("*test*", "tsttst"));
        assertFalse(pathMatcher.match("*test", "tsttst"));
        assertFalse(pathMatcher.match("*.*", "tsttst"));
        assertFalse(pathMatcher.match("test*aaa", "test"));
        assertFalse(pathMatcher.match("test*aaa", "testblaaab"));

        // test matching with ?'s and /'s
        assertTrue(pathMatcher.match("/?", "/a"));
        assertTrue(pathMatcher.match("/?/a", "/a/a"));
        assertTrue(pathMatcher.match("/a/?", "/a/b"));
        assertTrue(pathMatcher.match("/??/a", "/aa/a"));
        assertTrue(pathMatcher.match("/a/??", "/a/bb"));
        assertTrue(pathMatcher.match("/?", "/a"));

        // test matching with **'s
        assertTrue(pathMatcher.match("/**", "/testing/testing"));
        assertTrue(pathMatcher.match("/*/**", "/testing/testing"));
        assertTrue(pathMatcher.match("/**/*", "/testing/testing"));
        assertTrue(pathMatcher.match("test*/**", "test/"));
        assertTrue(pathMatcher.match("test*/**", "test/t"));
        assertTrue(pathMatcher.match("/bla/**/bla", "/bla/testing/testing/bla"));
        assertTrue(pathMatcher.match("/bla/**/bla", "/bla/testing/testing/bla/bla"));
        assertTrue(pathMatcher.match("/**/test", "/bla/bla/test"));
        assertTrue(pathMatcher.match("/bla/**/**/bla", "/bla/bla/bla/bla/bla/bla"));
        assertTrue(pathMatcher.match("/bla*bla/test", "/blaXXXbla/test"));
        assertTrue(pathMatcher.match("/*bla/test", "/XXXbla/test"));
        assertFalse(pathMatcher.match("/bla*bla/test", "/blaXXXbl/test"));
        assertFalse(pathMatcher.match("/*bla/test", "XXXblab/test"));
        assertFalse(pathMatcher.match("/*bla/test", "XXXbl/test"));

        assertFalse(pathMatcher.match("/????", "/bala/bla"));
        assertTrue(pathMatcher.match("/**/*bla", "/bla/bla/bla/bbb"));

        assertTrue(pathMatcher.match("/*bla*/**/bla/**", "/XXXblaXXXX/testing/testing/bla/testing/testing/"));
        assertTrue(pathMatcher.match("/*bla*/**/bla/*", "/XXXblaXXXX/testing/testing/bla/testing"));
        assertTrue(pathMatcher.match("/*bla*/**/bla/**", "/XXXblaXXXX/testing/testing/bla/testing/testing"));
        assertTrue(pathMatcher.match("/*bla*/**/bla/**", "/XXXblaXXXX/testing/testing/bla/testing/testing.jpg"));

        assertTrue(pathMatcher.match("*bla*/**/bla/**", "XXXblaXXXX/testing/testing/bla/testing/testing/"));
        assertTrue(pathMatcher.match("*bla*/**/bla/*", "XXXblaXXXX/testing/testing/bla/testing"));
        assertTrue(pathMatcher.match("*bla*/**/bla/**", "XXXblaXXXX/testing/testing/bla/testing/testing"));
        assertTrue(pathMatcher.match("*bla*/**/bla/*", "XXXblaXXXX/testing/testing/bla/testing/testing"));

        assertTrue(pathMatcher.match("/x/x/**/bla", "/x/x/x/"));

        assertTrue(pathMatcher.match("", ""));
    }

    @Disabled
    @Test
    public void isMatchWithTrimTokens() {
        final PathPatternParser pathMatcher = new PathPatternParser();
        //final AntPathMatcher pathMatcher = AntPathMatcher.builder().withTrimTokens().build();

        assertTrue(pathMatcher.match("/foo/bar/**", "/foo /bar"));
        assertTrue(pathMatcher.match("/foo/bar/**", "/foo          /bar"));
        assertTrue(pathMatcher.match("/foo/bar/**", "/foo          /               bar"));
        assertTrue(pathMatcher.match("/foo/bar/**", "       /      foo          /               bar"));
        assertTrue(pathMatcher.match("/**/*", "/      testing     /     testing   "));
        assertTrue(pathMatcher.match("org/**/servlet/bla.jsp", "   org   /      servlet    /   bla   .   jsp"));
    }

    @Disabled
    @Test
    public void isMatchWithIgnoreCaseWithCustomPathSeparatorWithTrimTokens() throws Exception {
        final PathPatternParser pathMatcher = new PathPatternParser();
        //final AntPathMatcher pathMatcher = AntPathMatcher.builder()
        //        .withIgnoreCase()
        //        .withTrimTokens()
        //        .withPathSeparator('.').build();

        assertTrue(pathMatcher.match(".foo.bar.**", ".FoO.baR"));
        assertTrue(pathMatcher.match("org.springframework.**.*.jsp", "ORG.  SpringFramework.web.views.hello . JSP"));
        assertTrue(pathMatcher.match("org.**.servlet.bla.jsp", "Org        .SERVLET     .     bla.jsp"));
        assertTrue(pathMatcher.match(".?", ".       A"));
        assertTrue(pathMatcher.match(".?.a", ".a.A"));
        assertTrue(pathMatcher.match(".a.??", ".       a      .     B       b"));
        assertTrue(pathMatcher.match(".?", ".a"));
        assertTrue(pathMatcher.match(".**", ".testing       .   teSting"));
        assertTrue(pathMatcher.match(".*.**", ".testing.testing"));
        assertTrue(pathMatcher.match(".**.*", "    .         tEsting .    testinG"));
        assertTrue(pathMatcher.match("http:..example.org", " H t T p : . . exAmple . org"));
        assertTrue(pathMatcher.match("HTTP:..EXAMPLE.ORG", "HtTp:..exAmple.org"));
    }
}
