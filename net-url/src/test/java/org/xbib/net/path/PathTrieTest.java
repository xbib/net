package org.xbib.net.path;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.fail;

class PathTrieTest {

    @Test
    void example() {
        PathTrie<Integer> trie = PathTrie.<Integer>builder()
                .add("GET", "/static/{file}", 1234)
                .add("HEAD", "/static/{file}", 1234)
                .build();

        assertSuccessfulResolution(trie, "GET", "/static/test.txt", 1234, Map.of("file", "test.txt"));
    }

    @Test
    void simple() {
        PathTrie<Integer> trie = PathTrie.<Integer>builder()
                .add("GET", "explorer", 1234)
                .build();

        assertSuccessfulGetResolution(trie, "explorer", 1234);
        assertFailedGetResolution(trie, "PUT", "explorer");
        assertFailedGetResolution(trie, "");
        assertFailedGetResolution(trie, "test");
    }

    @Test
    void sharedPrefix() {
        PathTrie<Integer> trie = PathTrie.<Integer>builder()
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
    void prefix() {
        PathTrie<Integer> trie = PathTrie.<Integer>builder()
                .add("GET", "discovery", 1234)
                .add("GET", "discovery/v1", 4321)
                .build();

        assertSuccessfulGetResolution(trie, "discovery", 1234);
        assertSuccessfulGetResolution(trie, "discovery/v1", 4321);
        assertFailedGetResolution(trie, "");
    }

    @Test
    void parameter() {
        PathTrie<Integer> trie = PathTrie.<Integer>builder()
                .add("GET", "discovery/{version}/rest", 1234)
                .build();

        assertSuccessfulGetResolution(
                trie, "discovery/v1/rest", 1234, Map.of("version", "v1"));
    }

    @Test
    void multipleParameters() {
        PathTrie<Integer> trie = PathTrie.<Integer>builder()
                .add("GET", "discovery/{discovery_version}/apis/{api}/{format}", 1234)
                .build();

        assertSuccessfulGetResolution(trie, "discovery/v1/apis/test/rest", 1234,
                Map.of("discovery_version", "v1", "api", "test", "format", "rest"));
    }

    @Test
    void sharedParameterPrefix() {
        PathTrie<Integer> trie = PathTrie.<Integer>builder()
                .add("GET", "discovery/{version}/rest", 1234)
                .add("GET", "discovery/{version}/rpc", 4321)
                .build();

        assertSuccessfulGetResolution(
                trie, "discovery/v1/rest", 1234, Map.of("version", "v1"));
        assertSuccessfulGetResolution(
                trie, "discovery/v1/rpc", 4321, Map.of("version", "v1"));
    }

    @Disabled
    @Test
    void encodedParameter() {
        PathTrie<Integer> trie = PathTrie.<Integer>builder()
                .add("GET", "{value}", 1234)
                .build();

        assertSuccessfulGetResolution(
                trie, "%E4%B8%AD%E6%96%87", 1234, Map.of("value", "中文"));
    }

    @Test
    void testResolveParameterAfterLiteral() {
        PathTrie<Integer> trie = PathTrie.<Integer>builder()
                .add("GET", "{one}/three", 1234)
                .add("GET", "one/two", 4321)
                .build();

        assertSuccessfulGetResolution(trie, "one/three", 1234, Map.of("one", "one"));
        assertSuccessfulGetResolution(trie, "one/two", 4321);
    }

    @Test
    void testResolveBacktrack() {
        PathTrie<Integer> trie = PathTrie.<Integer>builder()
                .add("GET", "{one}/{two}/three/{four}", 1234)
                .add("GET", "one/two/{three}/four", 4321)
                .build();

        assertSuccessfulGetResolution(trie, "one/two/three/five", 1234,
                Map.of("one", "one", "two", "two", "four", "five"));
        assertSuccessfulGetResolution(
                trie, "one/two/three/four", 4321, Map.of("three", "three"));
    }

    @Test
    void pathMethodsWithDifferentParameterNames() {
        PathTrie<Integer> trie = PathTrie.<Integer>builder()
                .add("GET", "test/{one}", 1234)
                .add("PUT", "test/{two}", 4321)
                .build();

        assertSuccessfulResolution(
                trie, "GET", "test/foo", 1234, Map.of("one", "foo"));
        assertSuccessfulResolution(
                trie, "PUT", "test/foo", 4321, Map.of("two", "foo"));
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
    void laxDuplicatePath() {
        PathTrie<Integer> trie = PathTrie.<Integer>builder(false)
                .add("GET", "test/{one}", 1234)
                .add("GET", "test/{two}", 4321)
                .build();

        PathTrie.Result<Integer> result = trie.resolve("GET", "test/foo");
        // We don't care which result is returned as long as it is a valid one.
        if (result.getRawParameters().containsKey("one")) {
            assertThat(result.getResult(), is(1234));
            assertThat(result.getRawParameters().get("one"),  is("foo"));
        } else {
            assertThat(result.getResult(), is(4321));
            assertThat(result.getRawParameters().get("two"), is("foo"));
        }
    }

    @Test
    void builderNullPath() {
        try {
            PathTrie.builder().add("GET", null, 1234);
            fail("expected NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }
    }

    @Test
    void builderNullValue() {
        try {
            PathTrie.builder().add("GET", "throws/an/exception", null);
            fail("expected NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }
    }

    @Test
    void resolveNullPath() {
        try {
            PathTrie<Integer> trie = PathTrie.<Integer>builder().build();
            trie.resolve("GET", null);
            fail("expected NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }
    }

    @Test
    void invalidParameterName() {
        try {
            PathTrie.builder().add("GET", "bad/{[test}", 1234);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    void invalidPathParameterSyntax() {
        try {
            PathTrie.builder().add("GET", "bad/{test", 1234);
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
                PathTrie.builder().add("GET", "bad/" + c, 1234);
                fail("expected IllegalArgumentException");
            } catch (IllegalArgumentException e) {
                // expected
            }
        }
    }

    private void doStrictDuplicateTest(String path, String duplicatePath) {
        try {
            PathTrie.builder()
                    .add("GET", path, 1234)
                    .add("GET", duplicatePath, 4321);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    private void assertSuccessfulGetResolution(PathTrie<Integer> trie, String path, Integer value) {
        assertSuccessfulResolution(trie, "GET", path, value);
    }

    private void assertSuccessfulResolution(PathTrie<Integer> trie, String method, String path, Integer value) {
        assertSuccessfulResolution(trie, method, path, value, Collections.emptyMap());
    }

    private void assertSuccessfulGetResolution(PathTrie<Integer> trie, String path, Integer value,
                                               Map<String, String> rawParameters) {
        assertSuccessfulResolution(trie, "GET", path, value, rawParameters);
    }

    private void assertSuccessfulResolution(PathTrie<Integer> trie, String method, String path, Integer value,
            Map<String, String> rawParameters) {
        PathTrie.Result<Integer> result = trie.resolve(method, path);
        assertThat(result, notNullValue());
        assertThat(result.getResult(), is(value));
        assertThat(result.getRawParameters(), is(rawParameters));
    }

    private void assertFailedGetResolution(PathTrie<Integer> trie, String path) {
        assertFailedGetResolution(trie, "GET", path);
    }

    private void assertFailedGetResolution(PathTrie<Integer> trie, String method, String path) {
        assertThat(trie.resolve(method, path), nullValue());
    }
}
