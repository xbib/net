package org.xbib.net.path;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.xbib.net.QueryParameters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 */
class PathMatcherTest {

    private PathMatcher pathMatcher = new PathMatcher();

    @Test
    void match() {
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
        assertFalse(pathMatcher.match("test*", "tsttest"));
        assertFalse(pathMatcher.match("test*", "test/"));
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
        assertTrue(pathMatcher.match("/**/*", "/testing/testing"));
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
        assertFalse(pathMatcher.match("/**/*bla", "/bla/bla/bla/bbb"));

        assertTrue(pathMatcher.match("/*bla*/**/bla/**", "/XXXblaXXXX/testing/testing/bla/testing/testing/"));
        assertTrue(pathMatcher.match("/*bla*/**/bla/*", "/XXXblaXXXX/testing/testing/bla/testing"));
        assertTrue(pathMatcher.match("/*bla*/**/bla/**", "/XXXblaXXXX/testing/testing/bla/testing/testing"));
        assertTrue(pathMatcher.match("/*bla*/**/bla/**", "/XXXblaXXXX/testing/testing/bla/testing/testing.jpg"));

        assertTrue(pathMatcher.match("*bla*/**/bla/**", "XXXblaXXXX/testing/testing/bla/testing/testing/"));
        assertTrue(pathMatcher.match("*bla*/**/bla/*", "XXXblaXXXX/testing/testing/bla/testing"));
        assertTrue(pathMatcher.match("*bla*/**/bla/**", "XXXblaXXXX/testing/testing/bla/testing/testing"));
        assertFalse(pathMatcher.match("*bla*/**/bla/*", "XXXblaXXXX/testing/testing/bla/testing/testing"));

        assertFalse(pathMatcher.match("/x/x/**/bla", "/x/x/x/"));

        assertTrue(pathMatcher.match("", ""));

        assertTrue(pathMatcher.match("/{bla}.*", "/testing.html"));
    }

    @Test
    void withMatchStart() {
        // test exact matching
        assertTrue(pathMatcher.matchStart("test", "test"));
        assertTrue(pathMatcher.matchStart("/test", "/test"));
        assertFalse(pathMatcher.matchStart("/test.jpg", "test.jpg"));
        assertFalse(pathMatcher.matchStart("test", "/test"));
        assertFalse(pathMatcher.matchStart("/test", "test"));

        // test matching with ?'s
        assertTrue(pathMatcher.matchStart("t?st", "test"));
        assertTrue(pathMatcher.matchStart("??st", "test"));
        assertTrue(pathMatcher.matchStart("tes?", "test"));
        assertTrue(pathMatcher.matchStart("te??", "test"));
        assertTrue(pathMatcher.matchStart("?es?", "test"));
        assertFalse(pathMatcher.matchStart("tes?", "tes"));
        assertFalse(pathMatcher.matchStart("tes?", "testt"));
        assertFalse(pathMatcher.matchStart("tes?", "tsst"));

        // test matching with *'s
        assertTrue(pathMatcher.matchStart("*", "test"));
        assertTrue(pathMatcher.matchStart("test*", "test"));
        assertTrue(pathMatcher.matchStart("test*", "testTest"));
        assertTrue(pathMatcher.matchStart("test/*", "test/Test"));
        assertTrue(pathMatcher.matchStart("test/*", "test/t"));
        assertTrue(pathMatcher.matchStart("test/*", "test/"));
        assertTrue(pathMatcher.matchStart("*test*", "AnothertestTest"));
        assertTrue(pathMatcher.matchStart("*test", "Anothertest"));
        assertTrue(pathMatcher.matchStart("*.*", "test."));
        assertTrue(pathMatcher.matchStart("*.*", "test.test"));
        assertTrue(pathMatcher.matchStart("*.*", "test.test.test"));
        assertTrue(pathMatcher.matchStart("test*aaa", "testblaaaa"));
        assertFalse(pathMatcher.matchStart("test*", "tst"));
        assertFalse(pathMatcher.matchStart("test*", "test/"));
        assertFalse(pathMatcher.matchStart("test*", "tsttest"));
        assertFalse(pathMatcher.matchStart("test*", "test/"));
        assertFalse(pathMatcher.matchStart("test*", "test/t"));
        assertTrue(pathMatcher.matchStart("test/*", "test"));
        assertTrue(pathMatcher.matchStart("test/t*.txt", "test"));
        assertFalse(pathMatcher.matchStart("*test*", "tsttst"));
        assertFalse(pathMatcher.matchStart("*test", "tsttst"));
        assertFalse(pathMatcher.matchStart("*.*", "tsttst"));
        assertFalse(pathMatcher.matchStart("test*aaa", "test"));
        assertFalse(pathMatcher.matchStart("test*aaa", "testblaaab"));

        // test matching with ?'s and /'s
        assertTrue(pathMatcher.matchStart("/?", "/a"));
        assertTrue(pathMatcher.matchStart("/?/a", "/a/a"));
        assertTrue(pathMatcher.matchStart("/a/?", "/a/b"));
        assertTrue(pathMatcher.matchStart("/??/a", "/aa/a"));
        assertTrue(pathMatcher.matchStart("/a/??", "/a/bb"));
        assertTrue(pathMatcher.matchStart("/?", "/a"));

        // test matching with **'s
        assertTrue(pathMatcher.matchStart("/**", "/testing/testing"));
        assertTrue(pathMatcher.matchStart("/*/**", "/testing/testing"));
        assertTrue(pathMatcher.matchStart("/**/*", "/testing/testing"));
        assertTrue(pathMatcher.matchStart("test*/**", "test/"));
        assertTrue(pathMatcher.matchStart("test*/**", "test/t"));
        assertTrue(pathMatcher.matchStart("/bla/**/bla", "/bla/testing/testing/bla"));
        assertTrue(pathMatcher.matchStart("/bla/**/bla", "/bla/testing/testing/bla/bla"));
        assertTrue(pathMatcher.matchStart("/**/test", "/bla/bla/test"));
        assertTrue(pathMatcher.matchStart("/bla/**/**/bla", "/bla/bla/bla/bla/bla/bla"));
        assertTrue(pathMatcher.matchStart("/bla*bla/test", "/blaXXXbla/test"));
        assertTrue(pathMatcher.matchStart("/*bla/test", "/XXXbla/test"));
        assertFalse(pathMatcher.matchStart("/bla*bla/test", "/blaXXXbl/test"));
        assertFalse(pathMatcher.matchStart("/*bla/test", "XXXblab/test"));
        assertFalse(pathMatcher.matchStart("/*bla/test", "XXXbl/test"));

        assertFalse(pathMatcher.matchStart("/????", "/bala/bla"));
        assertTrue(pathMatcher.matchStart("/**/*bla", "/bla/bla/bla/bbb"));

        assertTrue(pathMatcher.matchStart("/*bla*/**/bla/**", "/XXXblaXXXX/testing/testing/bla/testing/testing/"));
        assertTrue(pathMatcher.matchStart("/*bla*/**/bla/*", "/XXXblaXXXX/testing/testing/bla/testing"));
        assertTrue(pathMatcher.matchStart("/*bla*/**/bla/**", "/XXXblaXXXX/testing/testing/bla/testing/testing"));
        assertTrue(pathMatcher.matchStart("/*bla*/**/bla/**", "/XXXblaXXXX/testing/testing/bla/testing/testing.jpg"));

        assertTrue(pathMatcher.matchStart("*bla*/**/bla/**", "XXXblaXXXX/testing/testing/bla/testing/testing/"));
        assertTrue(pathMatcher.matchStart("*bla*/**/bla/*", "XXXblaXXXX/testing/testing/bla/testing"));
        assertTrue(pathMatcher.matchStart("*bla*/**/bla/**", "XXXblaXXXX/testing/testing/bla/testing/testing"));
        assertTrue(pathMatcher.matchStart("*bla*/**/bla/*", "XXXblaXXXX/testing/testing/bla/testing/testing"));

        assertTrue(pathMatcher.matchStart("/x/x/**/bla", "/x/x/x/"));

        assertTrue(pathMatcher.matchStart("", ""));
    }

    @Test
    void uniqueDeliminator() {
        pathMatcher.setPathSeparator(".");

        // test exact matching
        assertTrue(pathMatcher.match("test", "test"));
        assertTrue(pathMatcher.match(".test", ".test"));
        assertFalse(pathMatcher.match(".test/jpg", "test/jpg"));
        assertFalse(pathMatcher.match("test", ".test"));
        assertFalse(pathMatcher.match(".test", "test"));

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
        assertTrue(pathMatcher.match("*test*", "AnothertestTest"));
        assertTrue(pathMatcher.match("*test", "Anothertest"));
        assertTrue(pathMatcher.match("*/*", "test/"));
        assertTrue(pathMatcher.match("*/*", "test/test"));
        assertTrue(pathMatcher.match("*/*", "test/test/test"));
        assertTrue(pathMatcher.match("test*aaa", "testblaaaa"));
        assertFalse(pathMatcher.match("test*", "tst"));
        assertFalse(pathMatcher.match("test*", "tsttest"));
        assertFalse(pathMatcher.match("*test*", "tsttst"));
        assertFalse(pathMatcher.match("*test", "tsttst"));
        assertFalse(pathMatcher.match("*/*", "tsttst"));
        assertFalse(pathMatcher.match("test*aaa", "test"));
        assertFalse(pathMatcher.match("test*aaa", "testblaaab"));

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
        assertTrue(pathMatcher.match(".bla*bla.test", ".blaXXXbla.test"));
        assertTrue(pathMatcher.match(".*bla.test", ".XXXbla.test"));
        assertFalse(pathMatcher.match(".bla*bla.test", ".blaXXXbl.test"));
        assertFalse(pathMatcher.match(".*bla.test", "XXXblab.test"));
        assertFalse(pathMatcher.match(".*bla.test", "XXXbl.test"));
    }

    @Test
    void extractPathWithinPattern() throws Exception {
        assertEquals("",
                pathMatcher.extractPathWithinPattern("/docs/commit.html", "/docs/commit.html"));

        assertEquals("cvs/commit",
                pathMatcher.extractPathWithinPattern("/docs/*", "/docs/cvs/commit"));
        assertEquals("commit.html",
                pathMatcher.extractPathWithinPattern("/docs/cvs/*.html", "/docs/cvs/commit.html"));
        assertEquals("cvs/commit",
                pathMatcher.extractPathWithinPattern("/docs/**", "/docs/cvs/commit"));
        assertEquals("cvs/commit.html",
                pathMatcher.extractPathWithinPattern("/docs/**/*.html", "/docs/cvs/commit.html"));
        assertEquals("commit.html",
                pathMatcher.extractPathWithinPattern("/docs/**/*.html", "/docs/commit.html"));
        assertEquals("commit.html",
                pathMatcher.extractPathWithinPattern("/*.html", "/commit.html"));
        assertEquals("docs/commit.html",
                pathMatcher.extractPathWithinPattern("/*.html", "/docs/commit.html"));
        assertEquals("/commit.html",
                pathMatcher.extractPathWithinPattern("*.html", "/commit.html"));
        assertEquals("/docs/commit.html",
                pathMatcher.extractPathWithinPattern("*.html", "/docs/commit.html"));
        assertEquals("/docs/commit.html",
                pathMatcher.extractPathWithinPattern("**/*.*", "/docs/commit.html"));
        assertEquals("/docs/commit.html",
                pathMatcher.extractPathWithinPattern("*", "/docs/commit.html"));
        assertEquals("/docs/cvs/other/commit.html",
                pathMatcher.extractPathWithinPattern("**/commit.html", "/docs/cvs/other/commit.html"));
        assertEquals("cvs/other/commit.html",
                pathMatcher.extractPathWithinPattern("/docs/**/commit.html", "/docs/cvs/other/commit.html"));
        assertEquals("cvs/other/commit.html",
                pathMatcher.extractPathWithinPattern("/docs/**/**/**/**", "/docs/cvs/other/commit.html"));

        assertEquals("docs/cvs/commit",
                pathMatcher.extractPathWithinPattern("/d?cs/*", "/docs/cvs/commit"));
        assertEquals("cvs/commit.html",
                pathMatcher.extractPathWithinPattern("/docs/c?s/*.html", "/docs/cvs/commit.html"));
        assertEquals("docs/cvs/commit",
                pathMatcher.extractPathWithinPattern("/d?cs/**", "/docs/cvs/commit"));
        assertEquals("docs/cvs/commit.html",
                pathMatcher.extractPathWithinPattern("/d?cs/**/*.html", "/docs/cvs/commit.html"));
    }

    @Test
    void extractUriTemplateVariables() throws Exception {
        QueryParameters result = pathMatcher.extractUriTemplateVariables("/hotels/{hotel}", "/hotels/1");
        assertEquals("[hotel=1]", result.toString());
        result = pathMatcher.extractUriTemplateVariables("/h?tels/{hotel}", "/hotels/1");
        assertEquals("[hotel=1]", result.toString());
        result = pathMatcher.extractUriTemplateVariables("/hotels/{hotel}/bookings/{booking}", "/hotels/1/bookings/2");
        assertEquals("[hotel=1, booking=2]", result.toString());
        result = pathMatcher.extractUriTemplateVariables("/**/hotels/**/{hotel}", "/foo/hotels/bar/1");
        assertEquals("[hotel=1]", result.toString());
        result = pathMatcher.extractUriTemplateVariables("/{page}.html", "/42.html");
        assertEquals("[page=42]", result.toString());
        result = pathMatcher.extractUriTemplateVariables("/{page}.*", "/42.html");
        assertEquals("[page=42]", result.toString());
        result = pathMatcher.extractUriTemplateVariables("/A-{B}-C", "/A-b-C");
        assertEquals("[B=b]", result.toString());
        result = pathMatcher.extractUriTemplateVariables("/{name}.{extension}", "/test.html");
        assertEquals("[name=test, extension=html]", result.toString());
    }

    @Test
    void extractUriTemplateVariablesRegex() {
        QueryParameters result = pathMatcher
                .extractUriTemplateVariables("{symbolicName:[\\w\\.]+}-{version:[\\w\\.]+}.jar",
                        "com.example-1.0.0.jar");
        assertEquals("com.example", result.get("symbolicName").get(0));
        assertEquals("1.0.0", result.get("version").get(0));

        result = pathMatcher.extractUriTemplateVariables("{symbolicName:[\\w\\.]+}-sources-{version:[\\w\\.]+}.jar",
                "com.example-sources-1.0.0.jar");
        assertEquals("com.example", result.get("symbolicName").get(0));
        assertEquals("1.0.0", result.get("version").get(0));
    }

    @Test
    void extractUriTemplateVarsRegexQualifiers() {
        QueryParameters result = pathMatcher.extractUriTemplateVariables(
                "{symbolicName:[\\p{L}\\.]+}-sources-{version:[\\p{N}\\.]+}.jar",
                "com.example-sources-1.0.0.jar");
        assertEquals("com.example", result.get("symbolicName").get(0));
        assertEquals("1.0.0", result.get("version").get(0));
        result = pathMatcher.extractUriTemplateVariables(
                "{symbolicName:[\\w\\.]+}-sources-{version:[\\d\\.]+}-{year:\\d{4}}{month:\\d{2}}{day:\\d{2}}.jar",
                "com.example-sources-1.0.0-20100220.jar");
        assertEquals("com.example", result.get("symbolicName").get(0));
        assertEquals("1.0.0", result.get("version").get(0));
        assertEquals("2010", result.get("year").get(0));
        assertEquals("02", result.get("month").get(0));
        assertEquals("20", result.get("day").get(0));
        result = pathMatcher.extractUriTemplateVariables(
                "{symbolicName:[\\p{L}\\.]+}-sources-{version:[\\p{N}\\.\\{\\}]+}.jar",
                "com.example-sources-1.0.0.{12}.jar");
        assertEquals("com.example", result.get("symbolicName").get(0));
        assertEquals("1.0.0.{12}", result.get("version").get(0));
    }

    @Test
    void extractUriTemplateVarsRegexCapturingGroups() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            pathMatcher.extractUriTemplateVariables("/web/{id:foo(bar)?}", "/web/foobar");
        });
    }

    @Test
    void combine() {
        assertEquals("", pathMatcher.combine(null, null));
        assertEquals("/hotels", pathMatcher.combine("/hotels", null));
        assertEquals("/hotels", pathMatcher.combine(null, "/hotels"));
        assertEquals("/hotels/booking", pathMatcher.combine("/hotels/*", "booking"));
        assertEquals("/hotels/booking", pathMatcher.combine("/hotels/*", "/booking"));
        assertEquals("/hotels/**/booking", pathMatcher.combine("/hotels/**", "booking"));
        assertEquals("/hotels/**/booking", pathMatcher.combine("/hotels/**", "/booking"));
        assertEquals("/hotels/booking", pathMatcher.combine("/hotels", "/booking"));
        assertEquals("/hotels/booking", pathMatcher.combine("/hotels", "booking"));
        assertEquals("/hotels/booking", pathMatcher.combine("/hotels/", "booking"));
        assertEquals("/hotels/{hotel}", pathMatcher.combine("/hotels/*", "{hotel}"));
        assertEquals("/hotels/**/{hotel}", pathMatcher.combine("/hotels/**", "{hotel}"));
        assertEquals("/hotels/{hotel}", pathMatcher.combine("/hotels", "{hotel}"));
        assertEquals("/hotels/{hotel}.*", pathMatcher.combine("/hotels", "{hotel}.*"));
        assertEquals("/hotels/*/booking/{booking}", pathMatcher.combine("/hotels/*/booking", "{booking}"));
        assertEquals("/hotel.html", pathMatcher.combine("/*.html", "/hotel.html"));
        assertEquals("/hotel.html", pathMatcher.combine("/*.html", "/hotel"));
        assertEquals("/hotel.html", pathMatcher.combine("/*.html", "/hotel.*"));
        assertEquals("/*.html", pathMatcher.combine("/**", "/*.html"));
        assertEquals("/*.html", pathMatcher.combine("/*", "/*.html"));
        assertEquals("/*.html", pathMatcher.combine("/*.*", "/*.html"));
        assertEquals("/{foo}/bar", pathMatcher.combine("/{foo}", "/bar"));
        assertEquals("/user/user", pathMatcher.combine("/user", "/user"));
        assertEquals("/{foo:.*[^0-9].*}/edit/", pathMatcher.combine("/{foo:.*[^0-9].*}", "/edit/"));
        assertEquals("/1.0/foo/test", pathMatcher.combine("/1.0", "/foo/test"));
        assertEquals("/hotel", pathMatcher.combine("/", "/hotel"));
        assertEquals("/hotel/booking", pathMatcher.combine("/hotel/", "/booking"));
    }

    @Test
    void combineWithTwoFileExtensionPatterns() {
        Assertions.assertThrows(IllegalArgumentException.class, () ->{
            pathMatcher.combine("/*.html", "/*.txt");
        });
    }

    @Test
    void patternComparator() {
        Comparator<String> comparator = pathMatcher.getPatternComparator("/hotels/new");

        assertEquals(0, comparator.compare(null, null));
        assertEquals(1, comparator.compare(null, "/hotels/new"));
        assertEquals(-1, comparator.compare("/hotels/new", null));

        assertEquals(0, comparator.compare("/hotels/new", "/hotels/new"));

        assertEquals(-1, comparator.compare("/hotels/new", "/hotels/*"));
        assertEquals(1, comparator.compare("/hotels/*", "/hotels/new"));
        assertEquals(0, comparator.compare("/hotels/*", "/hotels/*"));

        assertEquals(-1, comparator.compare("/hotels/new", "/hotels/{hotel}"));
        assertEquals(1, comparator.compare("/hotels/{hotel}", "/hotels/new"));
        assertEquals(0, comparator.compare("/hotels/{hotel}", "/hotels/{hotel}"));
        assertEquals(-1, comparator.compare("/hotels/{hotel}/booking", "/hotels/{hotel}/bookings/{booking}"));
        assertEquals(1, comparator.compare("/hotels/{hotel}/bookings/{booking}", "/hotels/{hotel}/booking"));

        assertEquals(-1, comparator.compare("/hotels/{hotel}/bookings/{booking}/cutomers/{customer}", "/**"));
        assertEquals(1, comparator.compare("/**", "/hotels/{hotel}/bookings/{booking}/cutomers/{customer}"));
        assertEquals(0, comparator.compare("/**", "/**"));

        assertEquals(-1, comparator.compare("/hotels/{hotel}", "/hotels/*"));
        assertEquals(1, comparator.compare("/hotels/*", "/hotels/{hotel}"));

        assertEquals(-1, comparator.compare("/hotels/*", "/hotels/*/**"));
        assertEquals(1, comparator.compare("/hotels/*/**", "/hotels/*"));

        assertEquals(-1, comparator.compare("/hotels/new", "/hotels/new.*"));
        assertEquals(2, comparator.compare("/hotels/{hotel}", "/hotels/{hotel}.*"));

        assertEquals(-1, comparator.compare("/hotels/{hotel}/bookings/{booking}/cutomers/{customer}", "/hotels/**"));
        assertEquals(1, comparator.compare("/hotels/**", "/hotels/{hotel}/bookings/{booking}/cutomers/{customer}"));
        assertEquals(1, comparator.compare("/hotels/foo/bar/**", "/hotels/{hotel}"));
        assertEquals(-1, comparator.compare("/hotels/{hotel}", "/hotels/foo/bar/**"));
        assertEquals(2, comparator.compare("/hotels/**/bookings/**", "/hotels/**"));
        assertEquals(-2, comparator.compare("/hotels/**", "/hotels/**/bookings/**"));

        assertEquals(1, comparator.compare("/**", "/hotels/{hotel}"));

        assertEquals(1, comparator.compare("/hotels", "/hotels2"));

        assertEquals(-1, comparator.compare("*", "*/**"));
        assertEquals(1, comparator.compare("*/**", "*"));
    }

    @Test
    void patternComparatorSort() {
        Comparator<String> comparator = pathMatcher.getPatternComparator("/hotels/new");
        List<String> paths = new ArrayList<>(3);
        paths.add(null);
        paths.add("/hotels/new");
        paths.sort(comparator);
        assertEquals("/hotels/new", paths.get(0));
        assertNull(paths.get(1));
        paths.clear();

        paths.add("/hotels/new");
        paths.add(null);
        paths.sort(comparator);
        assertEquals("/hotels/new", paths.get(0));
        assertNull(paths.get(1));
        paths.clear();

        paths.add("/hotels/*");
        paths.add("/hotels/new");
        paths.sort(comparator);
        assertEquals("/hotels/new", paths.get(0));
        assertEquals("/hotels/*", paths.get(1));
        paths.clear();

        paths.add("/hotels/new");
        paths.add("/hotels/*");
        paths.sort(comparator);
        assertEquals("/hotels/new", paths.get(0));
        assertEquals("/hotels/*", paths.get(1));
        paths.clear();

        paths.add("/hotels/**");
        paths.add("/hotels/*");
        paths.sort(comparator);
        assertEquals("/hotels/*", paths.get(0));
        assertEquals("/hotels/**", paths.get(1));
        paths.clear();

        paths.add("/hotels/*");
        paths.add("/hotels/**");
        paths.sort(comparator);
        assertEquals("/hotels/*", paths.get(0));
        assertEquals("/hotels/**", paths.get(1));
        paths.clear();

        paths.add("/hotels/{hotel}");
        paths.add("/hotels/new");
        paths.sort(comparator);
        assertEquals("/hotels/new", paths.get(0));
        assertEquals("/hotels/{hotel}", paths.get(1));
        paths.clear();

        paths.add("/hotels/new");
        paths.add("/hotels/{hotel}");
        paths.sort(comparator);
        assertEquals("/hotels/new", paths.get(0));
        assertEquals("/hotels/{hotel}", paths.get(1));
        paths.clear();

        paths.add("/hotels/*");
        paths.add("/hotels/{hotel}");
        paths.add("/hotels/new");
        paths.sort(comparator);
        assertEquals("/hotels/new", paths.get(0));
        assertEquals("/hotels/{hotel}", paths.get(1));
        assertEquals("/hotels/*", paths.get(2));
        paths.clear();

        paths.add("/hotels/ne*");
        paths.add("/hotels/n*");
        Collections.shuffle(paths);
        paths.sort(comparator);
        assertEquals("/hotels/ne*", paths.get(0));
        assertEquals("/hotels/n*", paths.get(1));
        paths.clear();

        comparator = pathMatcher.getPatternComparator("/hotels/new.html");
        paths.add("/hotels/new.*");
        paths.add("/hotels/{hotel}");
        Collections.shuffle(paths);
        paths.sort(comparator);
        assertEquals("/hotels/new.*", paths.get(0));
        assertEquals("/hotels/{hotel}", paths.get(1));
        paths.clear();

        comparator = pathMatcher.getPatternComparator("/web/endUser/action/login.html");
        paths.add("/**/login.*");
        paths.add("/**/endUser/action/login.*");
        paths.sort(comparator);
        assertEquals("/**/endUser/action/login.*", paths.get(0));
        assertEquals("/**/login.*", paths.get(1));
        paths.clear();
    }

    @Test
    void trimTokensOff() {
        pathMatcher.setTrimTokens(false);
        assertTrue(pathMatcher.match("/group/{groupName}/members", "/group/sales/members"));
        assertTrue(pathMatcher.match("/group/{groupName}/members", "/group/  sales/members"));
        assertFalse(pathMatcher.match("/group/{groupName}/members", "/Group/  Sales/Members"));
    }

    @Test
    void caseInsensitive() {
        pathMatcher.setCaseSensitive(false);
        assertTrue(pathMatcher.match("/group/{groupName}/members", "/group/sales/members"));
        assertTrue(pathMatcher.match("/group/{groupName}/members", "/Group/Sales/Members"));
        assertTrue(pathMatcher.match("/Group/{groupName}/Members", "/group/Sales/members"));
    }

    @Test
    void extensionMappingWithDotPathSeparator() {
        pathMatcher.setPathSeparator(".");
        assertEquals("/*.html.hotel.*", pathMatcher.combine("/*.html", "hotel.*"));
    }
}

