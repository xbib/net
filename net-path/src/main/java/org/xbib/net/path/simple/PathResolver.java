package org.xbib.net.path.simple;

import org.xbib.net.Parameter;
import org.xbib.net.ParameterBuilder;
import org.xbib.net.util.CharMatcher;

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
 * A simple resolver that maps pairs of method names and parameterized paths to arbitrary data. Each
 * node in the tree is a path segment. For example, given a path "discovery/v1/apis", the data would
 * be stored in the node path represented by "discovery" -&gt; "v1" -&gt; "apis". A path is
 * considered parameterized if one or more segments is of the form "{name}". When a parameterized
 * path is resolved, a map from parameter names to raw String values is returned as part of the
 * result. Null values are not acceptable values. Parameter names can only contain
 * alphanumeric characters or underscores, and cannot start with a numeric.
 * @param <T> type
 */
public class PathResolver<T> implements org.xbib.net.path.PathResolver<T> {

    private static final String PARAMETER_PATH_SEGMENT = "{}";

    private static final Pattern PARAMETER_NAME_PATTERN = Pattern.compile("[a-zA-Z_][a-zA-Z_\\d]*");

    private static final CharMatcher RESERVED_URL_CHARS = CharMatcher.anyOf(":/?#[]{}");

    private final Builder<T> builder;

    private final Map<String, PathResolver<T>> children;

    private PathResolver(Builder<T> builder) {
        this.builder = builder;
        this.children = new LinkedHashMap<>();
        for (Entry<String, Builder<T>> entry : builder.subBuilders.entrySet()) {
            children.put(entry.getKey(), new PathResolver<>(entry.getValue()));
        }
    }

    /**
     * Attempts to resolve a path. Resolution prefers literal paths over path parameters. The result
     * includes the object to which the path mapped, as well a map from parameter names to
     * values. If the path cannot be resolved, null is returned.
     * @param method method
     * @param path path
     * @param resultListener result listener
     */
    @Override
    public void resolve(String method, String path, ResultListener<T> resultListener) {
        Objects.requireNonNull(method, "method");
        Objects.requireNonNull(path, "path");
        resolve(method, builder.pathMatcher.tokenize(path), 0, new ArrayList<>(), resultListener);
    }

    @Override
    public String toString() {
        return "PathResolver[" + "builder=" + builder + ", children=" + children + ']';
    }

    private void resolve(String method,
                         List<String> pathSegments,
                         int index,
                         List<String> parameters,
                         ResultListener<T> resultListener) {
        if (index < pathSegments.size()) {
            String segment = pathSegments.get(index);
            PathResolver<T> child = children.get(segment);
            if (child != null) {
                child.resolve(method, pathSegments, index + 1, parameters, resultListener);
            }
            child = children.get(PARAMETER_PATH_SEGMENT);
            if (child != null) {
                parameters.add(segment);
                child.resolve(method, pathSegments, index + 1, parameters, resultListener);
                parameters.remove(parameters.size() - 1);
            }
        } else if (builder.infoMap.containsKey(method)) {
            Info<T> info = builder.infoMap.get(method);
            ParameterBuilder parameterBuilder = Parameter.builder()
                    .domain(Parameter.Domain.PATH)
                    .enableSort();
            for (int i = 0; i < info.parameterNames.size(); i++) {
                parameterBuilder.add(info.parameterNames.get(i), parameters.get(i));
            }
            resultListener.onResult(new Result<>(info.value, parameterBuilder.build(), info.method));
        }
    }

    /**
     * The resulting information for a successful path resolution, which includes the value to which
     * the path maps, as well as the raw string values of all path parameters.
     * @param <T> type
     */
    public static class Result<T> implements org.xbib.net.path.PathResolver.Result<T> {

        private final T value;

        private final Parameter parameter;

        private final String method;

        Result(T value, Parameter parameter, String method) {
            this.value = value;
            this.parameter = parameter;
            this.method = method;
        }

        @Override
        public T getValue() {
            return value;
        }

        @Override
        public Parameter getParameter() {
            return parameter;
        }

        @Override
        public String getMethod() {
            return method;
        }
    }

    /**
     * Returns a new, path conflict validating {@link Builder}.
     *
     * @param <T> the type that the resolver will be storing
     * @return the builder
     */
    public static <T> Builder<T> builder() {
        return new Builder<>(true);
    }

    /**
     * Returns a new {@link Builder}.
     *
     * @param throwOnConflict whether or not to throw an exception on path conflicts
     * @param <T> the type that the resolver will be storing
     * @return the builder
     */
    public static <T> Builder<T> builder(boolean throwOnConflict) {
        return new Builder<>(throwOnConflict);
    }

    /**
     * A builder for creating a {@link PathResolver}.
     * @param <T> type
     */
    public static class Builder<T> implements org.xbib.net.path.PathResolver.Builder<T> {

        private final Map<String, Builder<T>> subBuilders;

        private final Map<String, Info<T>> infoMap;

        private final boolean throwOnConflict;

        private final PathMatcher pathMatcher;

        Builder(boolean throwOnConflict) {
            this.throwOnConflict = throwOnConflict;
            this.subBuilders = new LinkedHashMap<>();
            this.infoMap = new LinkedHashMap<>();
            this.pathMatcher = new PathMatcher();
        }

        /**
         * Adds a path.
         *
         * @param method the method
         * @param path the path
         * @param value the value
         * @return the builder
         * @throws IllegalArgumentException if the path cannot be added
         * @throws NullPointerException if either path or value are null
         */
        @Override
        public Builder<T> add(String method, String path, T value) {
            Objects.requireNonNull(method, "method");
            Objects.requireNonNull(path, "path");
            Objects.requireNonNull(value, "value");
            internalAdd(method, path, pathMatcher.tokenize(path).iterator(), value, new ArrayList<>());
            return this;
        }

        @Override
        public PathResolver<T> build() {
            return new PathResolver<>(this);
        }

        private void internalAdd(String method,
                                 String path,
                                 Iterator<String> pathSegments,
                                 T value,
                                 List<String> parameterNames) {
            if (pathSegments.hasNext()) {
                String segment = pathSegments.next();
                if (segment.startsWith("{")) {
                    if (segment.endsWith("}")) {
                        parameterNames.add(getAndCheckParameterName(segment));
                        getOrCreateSubBuilder(PARAMETER_PATH_SEGMENT)
                                .internalAdd(method, path, pathSegments, value, parameterNames);
                    } else {
                        throw new IllegalArgumentException("missing closed brace } in " + segment);
                    }
                } else if (segment.contains(Path.CATCH_ALL)) {
                    getOrCreateSubBuilder(Path.CATCH_ALL).internalAdd(method, path, pathSegments, value, parameterNames);
                } else {
                    if (RESERVED_URL_CHARS.matchesAnyOf(segment)) {
                        throw new IllegalArgumentException("contains reserved URL character in " + segment);
                    }
                    getOrCreateSubBuilder(segment).internalAdd(method, path, pathSegments, value, parameterNames);
                }
            } else {
                boolean pathExists = infoMap.containsKey(method);
                if (pathExists && throwOnConflict) {
                    throw new IllegalArgumentException("path '" + path + "' is already mapped");
                }
                infoMap.put(method, new Info<>(parameterNames, value, method));
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

    private static class Info<T> {
        private final T value;
        private final List<String> parameterNames;
        private final String method;

        Info(List<String> parameterNames, T value, String method) {
            this.parameterNames = parameterNames;
            this.value = value;
            this.method = method;
        }
    }
}
