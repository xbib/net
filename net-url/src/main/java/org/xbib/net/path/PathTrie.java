package org.xbib.net.path;

import org.xbib.net.matcher.CharMatcher;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A simple trie that maps pairs of HTTP methods and parameterized paths to arbitrary data. Each
 * node in the tree is a path segment. For example, given a path "discovery/v1/apis", the data would
 * be stored in the node path represented by "discovery" -&gt; "v1" -&gt; "apis". A path is
 * considered parameterized if one or more segments is of the form "{name}". When a parameterized
 * path is resolved, a map from parameter names to raw String values is returned as part of the
 * result. Null values are not acceptable values in this trie. Parameter names can only contain
 * alphanumeric characters or underscores, and cannot start with a numeric.
 */
public class PathTrie<T> {

    private static final String PARAMETER_PATH_SEGMENT = "{}";

    private static final Pattern PARAMETER_NAME_PATTERN = Pattern.compile("[a-zA-Z_][a-zA-Z_\\d]*");

    // General delimiters that must be URL encoded, as defined by RFC 3986.
    private static final CharMatcher RESERVED_URL_CHARS = CharMatcher.anyOf(":/?#[]{}");

    private final Map<String, PathTrie<T>> subTries;

    private final Map<String, MethodInfo<T>> httpMethodMap;

    private final PathMatcher pathMatcher;

    private PathTrie(Builder<T> builder) {
        this.httpMethodMap = builder.httpMethodMap;
        Map<String, PathTrie<T>> subTriesBuilder = new LinkedHashMap<>();
        for (Entry<String, Builder<T>> entry : builder.subBuilders.entrySet()) {
            subTriesBuilder.put(entry.getKey(), new PathTrie<>(entry.getValue()));
        }
        this.subTries = subTriesBuilder;
        this.pathMatcher = new PathMatcher();
    }

    /**
     * Attempts to resolve a path. Resolution prefers literal paths over path parameters. The result
     * includes the object to which the path mapped, as well a map from parameter names to
     * URL-decoded values. If the path cannot be resolved, null is returned.
     * @param method method
     * @param path path
     * @return result of resolving
     */
    public Result<T> resolve(String method, String path) {
        Objects.requireNonNull(method, "method");
        Objects.requireNonNull(path, "path");
        return resolve(method, pathMatcher.tokenizePath(path), 0, new ArrayList<>(), new ArrayList<>());
    }

    private Result<T> resolve(String method, List<String> pathSegments, int index,
                              List<String> context, List<String> rawParameters) {
        if (index < pathSegments.size()) {
            String segment = pathSegments.get(index);
            PathTrie<T> subTrie = subTries.get(segment);
            if (subTrie != null) {
                context.add(segment);
                Result<T> result = subTrie.resolve(method, pathSegments, index + 1, context, rawParameters);
                if (result != null) {
                    return result;
                }
            }
            subTrie = subTries.get(PARAMETER_PATH_SEGMENT);
            if (subTrie != null) {
                rawParameters.add(segment);
                Result<T> result = subTrie.resolve(method, pathSegments, index + 1, context, rawParameters);
                if (result == null) {
                    rawParameters.remove(rawParameters.size() - 1);
                }
                return result;
            }
            return null;
        } else if (httpMethodMap.containsKey(method)) {
            MethodInfo<T> methodInfo = httpMethodMap.get(method);
            List<String> parameterNames = methodInfo.parameterNames;
            if (rawParameters.size() != parameterNames.size()) {
                throw new IllegalStateException();
            }
            Map<String, String> rawParameterMap = new LinkedHashMap<>();
            for (int i = 0; i < parameterNames.size(); i++) {
                rawParameterMap.put(parameterNames.get(i), rawParameters.get(i));
            }
            return new Result<>(methodInfo.value, context, rawParameterMap);
        }
        return null;
    }

    /**
     * The resulting information for a successful path resolution, which includes the value to which
     * the path maps, as well as the raw (but URL decoded) string values of all path parameters.
     */
    public static class Result<T> {

        private final T result;

        private final List<String> context;

        private final Map<String, String> rawParameters;

        Result(T result, List<String> context, Map<String, String> rawParameters) {
            this.result = result;
            this.context = context;
            this.rawParameters = rawParameters;
        }

        public T getResult() {
            return result;
        }

        public List<String> getContext() {
            return context;
        }

        public Map<String, String> getRawParameters() {
            return rawParameters;
        }
    }

    /**
     * Returns a new, path conflict validating {@link PathTrie.Builder}.
     *
     * @param <T> the type that the trie will be storing
     * @return the trie builder
     */
    public static <T> Builder<T> builder() {
        return new Builder<>(true);
    }

    /**
     * Returns a new {@link PathTrie.Builder}.
     *
     * @param throwOnConflict whether or not to throw an exception on path conflicts
     * @param <T> the type that the trie will be storing
     * @return the trie builder
     */
    public static <T> Builder<T> builder(boolean throwOnConflict) {
        return new Builder<>(throwOnConflict);
    }

    /**
     * A builder for creating a {@link PathTrie}, which is immutable.
     */
    public static class Builder<T> {

        private final Map<String, Builder<T>> subBuilders = new LinkedHashMap<>();

        private final Map<String, MethodInfo<T>> httpMethodMap =new LinkedHashMap<>();

        private final boolean throwOnConflict;

        private final PathMatcher pathMatcher;

        Builder(boolean throwOnConflict) {
            this.throwOnConflict = throwOnConflict;
            this.pathMatcher = new PathMatcher();
        }

        /**
         * Adds a path to the trie.
         *
         * @param method the method
         * @param path the path
         * @param value the value
         * @return the trie builder
         * @throws IllegalArgumentException if the path cannot be added to the trie
         * @throws NullPointerException if either path or value are null
         */
        public Builder<T> add(String method, String path, T value) {
            Objects.requireNonNull(method, "method");
            Objects.requireNonNull(path, "path");
            Objects.requireNonNull(value, "value");
            add(method, path, pathMatcher.tokenizePath(path).iterator(), value, new ArrayList<>());
            return this;
        }

        public PathTrie<T> build() {
            return new PathTrie<>(this);
        }

        private void add(String method, String path, Iterator<String> pathSegments, T value,
                         List<String> parameterNames) {
            if (pathSegments.hasNext()) {
                String segment = pathSegments.next();
                if (segment.startsWith("{")) {
                    if (segment.endsWith("}")) {
                        parameterNames.add(getAndCheckParameterName(segment));
                        getOrCreateSubBuilder(PARAMETER_PATH_SEGMENT)
                                .add(method, path, pathSegments, value, parameterNames);
                    } else {
                        throw new IllegalArgumentException(String.format("'%s' contains invalid parameter syntax: %s",
                                path, segment));
                    }
                } else {
                    if (RESERVED_URL_CHARS.matchesAnyOf(segment)) {
                        throw new IllegalArgumentException(String.format("'%s' contains invalid path segment: %s",
                                path, segment));
                    }
                    getOrCreateSubBuilder(segment).add(method, path, pathSegments, value, parameterNames);
                }
            } else {
                boolean pathExists = httpMethodMap.containsKey(method);
                if (pathExists && throwOnConflict) {
                    throw new IllegalArgumentException(String.format("Path '%s' is already mapped", path));
                }
                httpMethodMap.put(method, new MethodInfo<>(parameterNames, value));
            }
        }

        private String getAndCheckParameterName(String segment) {
            String name = segment.substring(1, segment.length() - 1);
            Matcher matcher = PARAMETER_NAME_PATTERN.matcher(name);
            if (!matcher.matches()) {
                throw new IllegalArgumentException(String.format("'%s' not a valid path parameter name", name));
            }
            return name;
        }

        private Builder<T> getOrCreateSubBuilder(String segment) {
            Builder<T> subBuilder = subBuilders.get(segment);
            if (subBuilder == null) {
                subBuilder = builder(throwOnConflict);
                subBuilders.put(segment, subBuilder);
            }
            return subBuilder;
        }
    }

    private static class MethodInfo<T> {
        private final List<String> parameterNames;
        private final T value;

        MethodInfo(List<String> parameterNames, T value) {
            this.parameterNames = parameterNames;
            this.value = value;
        }
    }
}
