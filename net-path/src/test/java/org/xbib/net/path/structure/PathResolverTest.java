package org.xbib.net.path.structure;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.xbib.net.Parameter;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class PathResolverTest {

    @Test
    void example() {
        PathResolver<Integer> pathResolver = PathResolver.<Integer>builder()
                .add( "GET", "/static/{file}", 1234)
                .build();
        assertSuccessfulResolution(pathResolver, "GET", "/static/test.txt", 1234,
                Parameter.of("PATH", Map.of("file", "test.txt")));
    }

    @Test
    void simple() {
        PathResolver<Integer> pathResolver = PathResolver.<Integer>builder()
                .add("GET", "explorer", 1234)
                .build();
        assertSuccessfulResolution(pathResolver, "explorer", 1234);
        assertFailedGetResolution(pathResolver, "");
        assertFailedGetResolution(pathResolver, "test");
    }

    @Test
    void sharedPrefix() {
        PathResolver<Integer> pathResolver = PathResolver.<Integer>builder()
                .add("GET", "discovery/v1/rest", 1234)
                .add("GET", "discovery/v2/rest", 4321)
                .build();
        assertSuccessfulResolution(pathResolver, "discovery/v1/rest", 1234);
        assertSuccessfulResolution(pathResolver, "discovery/v2/rest", 4321);
        assertFailedGetResolution(pathResolver, "");
        assertFailedGetResolution(pathResolver, "discovery");
        assertFailedGetResolution(pathResolver, "discovery/v1");
    }

    @Test
    void prefix() {
        PathResolver<Integer> pathResolver = PathResolver.<Integer>builder()
                .add("GET", "discovery", 1234)
                .add("GET", "discovery/v1", 4321)
                .build();
        assertSuccessfulResolution(pathResolver, "discovery", 1234);
        assertSuccessfulResolution(pathResolver, "discovery/v1", 4321);
        assertFailedGetResolution(pathResolver, "");
    }

    @Test
    void parameter() {
        PathResolver<Integer> pathResolver = PathResolver.<Integer>builder()
                .add("GET", "discovery/{version}/rest", 1234)
                .build();
        assertSuccessfulResolution(pathResolver, "GET", "discovery/v1/rest", 1234,
                Parameter.of("PATH", Map.of("version", "v1")));
    }

    @Test
    void multipleParameters() {
        PathResolver<Integer> pathResolver = PathResolver.<Integer>builder()
                .add("GET", "discovery/{discovery_version}/apis/{api}/{format}", 1234)
                .build();
        assertSuccessfulResolution(pathResolver, "GET", "discovery/v1/apis/test/rest", 1234,
                Parameter.of("PATH", Map.of("discovery_version", "v1", "api", "test", "format", "rest")));
    }

    @Test
    void sharedParameterPrefix() {
        PathResolver<Integer> pathResolver = PathResolver.<Integer>builder()
                .add("GET", "discovery/{version}/rest", 1234)
                .add("GET", "discovery/{version}/rpc", 4321)
                .build();
        assertSuccessfulResolution(pathResolver, "GET", "discovery/v1/rest", 1234,
                Parameter.of("PATH", Map.of("version", "v1")));
        assertSuccessfulResolution(pathResolver, "GET", "discovery/v2/rest", 1234,
                Parameter.of("PATH", Map.of("version", "v2")));
        assertSuccessfulResolution(pathResolver, "GET", "discovery/v1/rpc", 4321,
                Parameter.of("PATH", Map.of("version", "v1")));
        assertSuccessfulResolution(pathResolver, "GET", "discovery/v2/rpc", 4321,
                Parameter.of("PATH", Map.of("version", "v2")));
    }

    @Test
    void testResolveParameterAfterLiteral() {
        PathResolver<Integer> pathResolver = PathResolver.<Integer>builder()
                .add("GET", "{one}/three", 1234)
                .add("GET", "one/two", 4321)
                .build();
        assertSuccessfulResolution(pathResolver, "GET", "one/three", 1234,
                Parameter.of("PATH", Map.of("one", "one")));
        assertSuccessfulResolution(pathResolver, "one/two", 4321);
    }

    @Test
    void testResolveBacktrack() {
        PathResolver<Integer> pathResolver = PathResolver.<Integer>builder()
                .add("GET", "{one}/{two}/three/{four}", 1234)
                .add("GET", "one/two/{three}/four", 4321)
                .build();
        pathResolver.resolve("GET", "one/two/three/four", result -> {
            assertThat(result.getValue(), anyOf(equalTo(1234), equalTo(4321)));
            if (result.getParameter().containsKey("PATH", "three")) {
                assertThat(result.getParameter(), is(Parameter.of("PATH", Map.of("three", "three"))));
            } else {
                assertThat(result.getParameter(), is(Parameter.of("PATH", Map.of("one", "one", "two", "two", "four", "four"))));
            }
        });
        pathResolver.resolve("GET", "one/two/three/five", result -> {
            assertThat(result.getValue(), equalTo(1234));
            assertThat(result.getParameter(), is(Parameter.of("PATH", Map.of("one", "one", "two", "two", "four", "five"))));
        });
    }

    @Test
    void pathMethodsWithDifferentParameterNames() {
        PathResolver<Integer> pathResolver = PathResolver.<Integer>builder()
                .add("GET", "test/{one}", 1234)
                .add("GET", "test/{two}", 4321)
                .build();
        AtomicInteger count = new AtomicInteger();
        pathResolver.resolve("GET", "test/foo", result -> {
            assertThat(result.getValue(), anyOf(equalTo(1234), equalTo(4321)));
            if (result.getValue() == 1234) {
                assertThat(result.getParameter(), is(Parameter.of("PATH", Map.of("one", "foo"))));
            }
            if (result.getValue() == 4321) {
                assertThat(result.getParameter(), is(Parameter.of("PATH", Map.of("two", "foo"))));
            }
            count.incrementAndGet();
        });
        assertThat(count.get(), is(2));
    }

    @Test
    void duplicatePathParams() {
        PathResolver<Integer> pathResolver = PathResolver.<Integer>builder()
                .add("GET", "test/{one}", 1234)
                .add("GET", "test/{two}", 4321)
                .build();
        AtomicInteger count = new AtomicInteger();
        pathResolver.resolve("GET", "test/foo", result -> {
            assertThat(result.getValue(), anyOf(equalTo(1234), equalTo(4321)));
            if (result.getParameter().containsKey("PATH", "one")) {
                assertThat(result.getParameter().get("PATH", "one"), is("foo"));
            }
            if (result.getParameter().containsKey("PATH", "two")) {
                assertThat(result.getParameter().get("PATH", "two"), is("foo"));
            }
            count.incrementAndGet();
        });
        assertThat(count.get(), is(2));
    }

    @Test
    void builderNullPath() {
        try {
            PathResolver.builder()
                    .add("GET", null, 1234);
            fail("expected NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }
    }

    @Test
    void builderNullValue() {
        try {
            PathResolver.builder()
                    .add("GET", "throws/an/exception", null);
            fail("expected NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }
    }

    @Test
    void resolveNullPath() {
        try {
            PathResolver<Integer> trie = PathResolver.<Integer>builder().build();
            trie.resolve(null, null, r -> {});
            fail("expected NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }
    }

    @Test
    void testFallback() {
        AtomicInteger counter = new AtomicInteger(0);
        PathResolver<Integer> trie = PathResolver.<Integer>builder()
                .add( "GET", "/test/{one}", 1)
                .add( "GET", "/**", 2)
                .build();
        trie.resolve("GET", "/test/foo", result -> {
            assertTrue( result.getValue() == 1 || result.getValue() == 2);
            counter.incrementAndGet();
        });
        assertThat(counter.get(), equalTo(1));
    }

    @Disabled
    @Test
    void testSuffixCatchAll() {
        AtomicInteger counter = new AtomicInteger(0);
        PathResolver<Integer> trie = PathResolver.<Integer>builder()
                .add( "GET", "/**/*.test", 1)
                .add( "GET", "/**", 2)
                .build();
        trie.resolve("GET", "/test/test.test", result -> {
            assertTrue( result.getValue() == 1 || result.getValue() == 2);
            counter.incrementAndGet();
        });
        assertThat(counter.get(), equalTo(2));
    }

    private void assertSuccessfulResolution(PathResolver<Integer> pathResolver, String path, Integer value) {
        assertSuccessfulResolution(pathResolver, "GET", path, value, Parameter.of("PATH"));
    }

    private void assertSuccessfulResolution(PathResolver<Integer> pathResolver, String method, String path, Integer value,
            Parameter parameter) {
        AtomicBoolean found = new AtomicBoolean(false);
        pathResolver.resolve(method, path, result -> {
            assertThat(result, notNullValue());
            assertThat(result.getMethod(), is(method));
            assertThat(result.getValue(), is(value));
            assertThat(result.getParameter(), is(parameter));
            found.set(true);
        });
        assertTrue(found.get());
    }

    private void assertFailedGetResolution(PathResolver<Integer> pathResolver, String path) {
        pathResolver.resolve("GET", path, r -> assertThat(r, nullValue()));
    }
}
