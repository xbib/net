package org.xbib.net.path.structure;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.xbib.net.Parameter;
import org.xbib.net.ParameterBuilder;
import org.xbib.net.PathNormalizer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;

/**
 * A resolver that maps path specifications to arbitrary objects using a trie structure.
 * Each node in the tree is a pattern segment. For example, given a path "discovery/v1/apis", the data would
 * be stored in the node path represented by "discovery" -&gt; "v1" -&gt; "apis".
 *
 * @param <T> type
 */
public class PathResolver<T> implements org.xbib.net.path.PathResolver<T> {

    private static final Logger logger = Logger.getLogger(PathResolver.class.getName());

    private static final String PATH_DOMAIN = "PATH";

    private final Builder<T> builder;

    private final Map<PathSegment, PathResolver<T>> children;

    private PathResolver(Builder<T> builder) {
        this.builder = builder;
        this.children = new LinkedHashMap<>();
        for (Map.Entry<PathSegment, Builder<T>> entry : builder.children.entrySet()) {
            children.put(entry.getKey(), new PathResolver<>(entry.getValue()));
        }
    }

    @Override
    public void resolve(String method, String path, ResultListener<T> listener) {
        Objects.requireNonNull(method, "method");
        Objects.requireNonNull(path, "path");
        List<PathSegment> pathSegments = PathMatcher.tokenize(PathNormalizer.normalize(path),
                builder.pathSeparator, builder.trimTokens, builder.caseSensitive);
        ParameterBuilder parameterBuilder = Parameter.builder().domain(PATH_DOMAIN).enableSort();
        resolve(pathSegments, 0, parameterBuilder, listener);
    }

    @Override
    public String toString() {
        return "PathResolver[builder = " + builder + ", path resolver map = " + children + ']';
    }

    private ParameterBuilder resolve(List<PathSegment> pathSegments,
                         int index,
                         ParameterBuilder parameterBuilder,
                         ResultListener<T> listener) {
        ParameterBuilder pb = parameterBuilder;
        if (index < pathSegments.size()) {
            PathSegment segment = pathSegments.get(index);
            List<PathResolver<T>> list = new ArrayList<>();
            boolean shortCircuit = match(segment, list, pb);
            if (list.isEmpty()) {
                pb = Parameter.builder().domain(PATH_DOMAIN).enableSort();
            }
            if (shortCircuit) {
                PathResolver<T> pathResolver = list.get(list.size() - 1);
                if (pathResolver.builder.value != null) {
                    T value = pathResolver.builder.value;
                    if (listener != null) {
                        listener.onResult(new Result<>(value, pb.build(), pathResolver.builder.method));
                        pb = Parameter.builder().domain(PATH_DOMAIN).enableSort();
                    }
                }
            } else {
                for (PathResolver<T> pathResolver : list) {
                    pb = pathResolver.resolve(pathSegments, index + 1, pb, listener);
                }
            }
        } else {
            if (builder.value != null) {
                T value = builder.value;
                if (listener != null) {
                    listener.onResult(new Result<>(value, pb.build(), builder.method));
                    pb = Parameter.builder().domain(PATH_DOMAIN).enableSort();
                }
            }
        }
        return pb;
    }

    private boolean match(PathSegment segment,
                          List<PathResolver<T>> list,
                          ParameterBuilder parameterBuilder) {
        boolean lastSegment = false;
        int i = 0;
        int size = children.size();
        for (PathSegment pathSegment : children.keySet()) {
            if (pathSegment.getParameterNames() != null) {
                matchAndExtractVariables(parameterBuilder, pathSegment, segment);
                PathResolver<T> pathResolver = children.get(pathSegment);
                list.add(pathResolver);
            } else if (pathSegment.getPattern() != null) {
                if (pathSegment.getPattern().matcher(segment.getString()).matches()) {
                    list.add(children.get(pathSegment));
                }
            } else if (pathSegment.getString().equals(segment.getString())) {
                list.add(children.get(pathSegment));
            } else if (pathSegment.isCatchAll()) {
                list.add(children.get(pathSegment));
                lastSegment = i == size - 1;
            }
            i++;
        }
        logger.log(Level.INFO, "size = " + size + " lastSegment = " + lastSegment + " list.size() = " + list.size());
        return lastSegment;
    }

    private void matchAndExtractVariables(ParameterBuilder parameterBuilder, PathSegment patternSegment, PathSegment pathSegment) {
        if (patternSegment.getPattern() == null) {
            return;
        }
        Matcher matcher = patternSegment.getPattern().matcher(pathSegment.getString());
        if (matcher.matches()) {
            if (patternSegment.getParameterNames() == null) {
                return;
            }
            if (patternSegment.getParameterNames().size() != matcher.groupCount()) {
                throw new IllegalArgumentException("The number of capturing groups in the pattern segment " +
                        patternSegment.getString() + " does not match the number of URI template variables it defines, " +
                        "which can occur if capturing groups are used in a URI template regex. " +
                        "Use non-capturing groups instead.");
            }
            for (int i = 1; i <= matcher.groupCount(); i++) {
                String name = patternSegment.getParameterNames().get(i - 1);
                String value = matcher.group(i);
                parameterBuilder.add(name, value);
            }
        }
    }

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

        @Override
        public String toString() {
            return value != null ? value.toString() : null;
        }
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    public static class Builder<T> {

        private final Map<PathSegment, Builder<T>> children;

        private String pathSeparator;

        private boolean trimTokens;

        private boolean caseSensitive;

        private T value;

        private String method;

        Builder() {
            this.children = new LinkedHashMap<>();
            this.pathSeparator = "/";
            this.trimTokens = true;
            this.caseSensitive = true;
        }

        public Builder<T> pathSeparator(String pathSeparator) {
            this.pathSeparator = pathSeparator;
            return this;
        }

        public Builder<T> trimTokens(boolean trimTokens) {
            this.trimTokens = trimTokens;
            return this;
        }

        public Builder<T> caseSensitive(boolean caseSensitive) {
            this.caseSensitive = caseSensitive;
            return this;
        }

        public Builder<T> add(String method, String pathSpec, T value) {
            Objects.requireNonNull(method, "method");
            Objects.requireNonNull(pathSpec, "pathSpec");
            Objects.requireNonNull(value, "value");
            PathMatcher pathMatcher = new PathMatcher(pathSpec, pathSeparator, trimTokens, caseSensitive, true,
                    Parameter.builder().domain(PATH_DOMAIN).enableSort());
            add(pathMatcher.getAnalyzedSegments(), value, method, 0);
            return this;
        }

        public PathResolver<T> build() {
            return new PathResolver<>(this);
        }

        private void add(List<PathSegment> pathSegments, T value, String method, int index) {
            if (index < pathSegments.size()) {
                children.computeIfAbsent(pathSegments.get(index), k -> new Builder<T>()
                        .pathSeparator(pathSeparator)
                        .trimTokens(trimTokens)
                        .caseSensitive(caseSensitive)
                ).add(pathSegments, value, method, index + 1);
            } else {
                this.value = value;
                this.method = method;
            }
        }
    }
}
