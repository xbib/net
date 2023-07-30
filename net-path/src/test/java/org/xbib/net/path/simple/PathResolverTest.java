package org.xbib.net.path.simple;

import org.junit.jupiter.api.Test;
import org.xbib.net.Parameter;
import org.xbib.net.ParameterException;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class PathResolverTest {

    @Test
    void simple() throws ParameterException {
        PathResolver<Integer> pathResolver = PathResolver.<Integer>builder()
                .add("GET", "explorer", 1234)
                .build();
        assertSuccessfulGetResolution(pathResolver, "explorer", 1234);
        assertFailedGetResolution(pathResolver, "PUT", "explorer");
        assertFailedGetResolution(pathResolver, "");
        assertFailedGetResolution(pathResolver, "test");
    }

    @Test
    void name() throws ParameterException {
        PathResolver<Integer> pathResolver = PathResolver.<Integer>builder()
                .add("GET", "/static/{file}", 1234)
                .add("HEAD", "/static/{file}", 1234)
                .build();
        assertSuccessfulResolution(pathResolver, "GET", "/static/test.txt",
                Set.of(1234), Map.of("file", "test.txt"));
    }

    @Test
    void glob() throws ParameterException {
        PathResolver<Integer> trie = PathResolver.<Integer>builder()
                .add("GET", "/static/**", 1234)
                .build();
        assertSuccessfulResolution(trie, "GET", "/static/test.txt",
                Set.of(1234), Map.of());
    }

    @Test
    void sharedPrefix() throws ParameterException {
        PathResolver<Integer> trie = PathResolver.<Integer>builder()
                .add("GET", "discovery/v1/rest", 1234)
                .add("GET", "discovery/v2/rest", 4321)
                .build();
        assertSuccessfulGetResolution(trie, "discovery/v1/rest", 1234);
        assertSuccessfulGetResolution(trie, "discovery/v2/rest", 4321);
        assertFailedGetResolution(trie, "");
        assertFailedGetResolution(trie, "discovery");
        assertFailedGetResolution(trie, "discovery/v1");
    }

    @Test
    void prefix() throws ParameterException {
        PathResolver<Integer> trie = PathResolver.<Integer>builder()
                .add("GET", "discovery", 1234)
                .add("GET", "discovery/v1", 4321)
                .build();
        assertSuccessfulGetResolution(trie, "discovery", 1234);
        assertSuccessfulGetResolution(trie, "discovery/v1", 4321);
        assertFailedGetResolution(trie, "");
    }

    @Test
    void parameter() throws ParameterException {
        PathResolver<Integer> trie = PathResolver.<Integer>builder()
                .add("GET", "discovery/{version}/rest", 1234)
                .build();
        assertSuccessfulGetResolution(
                trie, "discovery/v1/rest", 1234, Map.of("version", "v1"));
    }

    @Test
    void multipleParameters() throws ParameterException {
        PathResolver<Integer> trie = PathResolver.<Integer>builder()
                .add("GET", "discovery/{discovery_version}/apis/{api}/{format}", 1234)
                .build();
        assertSuccessfulGetResolution(trie, "discovery/v1/apis/test/rest", 1234,
                Map.of("discovery_version", "v1", "api", "test", "format", "rest"));
    }

    @Test
    void sharedParameterPrefix() throws ParameterException {
        PathResolver<Integer> trie = PathResolver.<Integer>builder()
                .add("GET", "discovery/{version}/rest", 1234)
                .add("GET", "discovery/{version}/rpc", 4321)
                .build();
        assertSuccessfulGetResolution(
                trie, "discovery/v1/rest", 1234, Map.of("version", "v1"));
        assertSuccessfulGetResolution(
                trie, "discovery/v1/rpc", 4321, Map.of("version", "v1"));
    }

    @Test
    void testResolveParameterAfterLiteral() throws ParameterException {
        PathResolver<Integer> trie = PathResolver.<Integer>builder()
                .add("GET", "{one}/three", 1234)
                .add("GET", "one/two", 4321)
                .build();
        assertSuccessfulGetResolution(trie, "one/three", 1234, Map.of("one", "one"));
        assertSuccessfulGetResolution(trie, "one/two", 4321);
    }

    @Test
    void testResolveBacktrack() throws ParameterException {
        PathResolver<Integer> trie = PathResolver.<Integer>builder()
                .add("GET", "{one}/{two}/three/{four}", 1234)
                .add("GET", "one/two/{three}/four", 4321)
                .build();
        assertSuccessfulResolution(trie, "GET", "one/two/three/five", Set.of(1234, 4321),
                Map.of("one", "one", "two", "two", "four", "five"));
    }

    @Test
    void pathMethodsWithDifferentParameterNames() throws ParameterException {
        PathResolver<Integer> trie = PathResolver.<Integer>builder()
                .add("GET", "test/{one}", 1234)
                .add("PUT", "test/{two}", 4321)
                .build();
        assertSuccessfulResolution(trie, "GET", "test/foo", Set.of(1234, 4321),
                Map.of("one", "foo"));
    }

    @Test
    void duplicatePath() {
        doStrictDuplicateTest("test/path", "test/path");
    }

    @Test
    void duplicateParameterizedPath() {
        doStrictDuplicateTest("test/{param}/path", "test/{parameterized}/path");
    }

    @Test
    void laxDuplicatePath() throws ParameterException {
        PathResolver<Integer> pathResolver = PathResolver.<Integer>builder(false)
                .add("GET", "test/{one}", 1234)
                .add("GET", "test/{two}", 4321)
                .build();
        pathResolver.resolve("GET", "test/foo", result -> {
            if (result.getParameter().containsKey("one", Parameter.Domain.PATH)) {
                assertThat(result.getValue(), is(1234));
                assertThat(result.getParameter().get("one", Parameter.Domain.PATH), is("foo"));
            } else {
                assertThat(result.getValue(), is(4321));
                assertThat(result.getParameter().get("two", Parameter.Domain.PATH), is("foo"));
            }
        });
    }

    @Test
    void builderNullPath() {
        try {
            PathResolver.builder().add("GET", null, 1234);
            fail("expected NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }
    }

    @Test
    void builderNullValue() {
        try {
            PathResolver.builder().add("GET", "throws/an/exception", null);
            fail("expected NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }
    }

    @Test
    void resolveNullPath() {
        try {
            PathResolver<Integer> trie = PathResolver.<Integer>builder().build();
            trie.resolve("GET", null, r -> {});
            fail("expected NullPointerException");
        } catch (NullPointerException e) {
            // expected
        } catch (ParameterException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void invalidParameterName() {
        try {
            PathResolver.builder().add("GET", "bad/{[test}", 1234);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    void invalidPathParameterSyntax() {
        try {
            PathResolver.builder().add("GET", "bad/{test", 1234);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    void invalidParameterSegment() {
        String invalids = "?#[]{}";
        for (char c : invalids.toCharArray()) {
            try {
                PathResolver.builder().add("GET", "bad/" + c, 1234);
                fail("expected IllegalArgumentException");
            } catch (IllegalArgumentException e) {
                // expected
            }
        }
    }

    @Test
    void testFallback() throws ParameterException {
        AtomicInteger counter = new AtomicInteger(0);
        org.xbib.net.path.PathResolver<Integer> trie = PathResolver.<Integer>builder()
                .add("GET", "/test/{one}", 1)
                .add("GET", "/**", 2)
                .build();
        trie.resolve("GET", "/test/foo", r-> {
            assertEquals(1, (int) r.getValue());
            counter.incrementAndGet();
        });
        assertThat(counter.get(), equalTo(1));
    }

    private void doStrictDuplicateTest(String path, String duplicatePath) {
        try {
            PathResolver.builder()
                    .add("GET", path, 1234)
                    .add("GET", duplicatePath, 4321);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    private void assertSuccessfulGetResolution(PathResolver<Integer> trie, String path, Integer value) throws ParameterException {
        assertSuccessfulResolution(trie, "GET", path, value);
    }

    private void assertSuccessfulResolution(PathResolver<Integer> trie, String method, String path, Integer value) throws ParameterException {
        assertSuccessfulResolution(trie, method, path, Set.of(value), Collections.emptyMap());
    }

    private void assertSuccessfulGetResolution(PathResolver<Integer> trie, String path, Integer value,
                                               Map<String, String> rawParameters) throws ParameterException {
        assertSuccessfulResolution(trie, "GET", path, Set.of(value), rawParameters);
    }

    private void assertSuccessfulResolution(PathResolver<Integer> trie,
                                            String method,
                                            String path,
                                            Set<Integer> values,
                                            Map<String, String> rawParameters) throws ParameterException {
        trie.resolve(method, path, result -> {
            assertTrue(values.contains(result.getValue()));
            //assertThat(result.getRawParameters(), is(rawParameters));
        });
    }

    private void assertFailedGetResolution(PathResolver<Integer> trie, String path) throws ParameterException {
        assertFailedGetResolution(trie, "GET", path);
    }

    private void assertFailedGetResolution(PathResolver<Integer> trie, String method, String path) throws ParameterException {
        trie.resolve(method, path, r-> { fail(); });
    }
}
