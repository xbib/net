package org.xbib.net;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.xbib.datastructures.common.ImmutableList;
import org.xbib.datastructures.common.LinkedHashSetMultiMap;
import org.xbib.datastructures.common.MultiMap;
import org.xbib.datastructures.common.Pair;

public class Parameter implements Iterable<Pair<String, Object>>, Comparable<Parameter> {

    private static final Parameter EMPTY = Parameter.builder().build();

    private final ParameterBuilder builder;

    private final ImmutableList<Pair<String, Object>> list;

    private final String queryString;

    Parameter(ParameterBuilder builder,
              ImmutableList<Pair<String, Object>> list,
              String queryString) {
        this.builder = builder;
        this.list = list;
        this.queryString = queryString;
    }

    public static ParameterBuilder builder() {
        return new ParameterBuilder();
    }

    public static ParameterBuilder builder(ParameterBuilder parameterBuilder) {
        return new ParameterBuilder(parameterBuilder);
    }

    public static Parameter of() {
        return EMPTY;
    }

    public static Parameter of(String domain) {
        return Parameter.builder().domain(domain).build();
    }

    public static Parameter of(Map<String, Object> map) {
        return Parameter.builder().enableSort().add(map).build();
    }

    public static Parameter of(String domain, Map<String, Object> map) {
        return Parameter.builder().domain(domain).enableSort().add(map).build();
    }

    @Override
    public Iterator<Pair<String, Object>> iterator() {
        return list.iterator();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Parameter &&
                builder.domain.equals(((Parameter) o).builder.domain) &&
                list.equals(((Parameter) o).list);
    }

    @Override
    public int hashCode() {
        return Objects.hash(builder.domain, list);
    }

    @Override
    public int compareTo(Parameter o) {
        return toString().compareTo(o.toString());
    }

    @Override
    public String toString() {
        return list.toString();
    }

    @SuppressWarnings("unchecked")
    public String getAsString(String domain, String key) {
        Object object = get(domain, key);
        if (object instanceof Collection) {
            Collection<Object> collection = (Collection<Object>) object;
            object = collection.iterator().next();
            return object != null ? object.toString() : null;
        }
        return object != null ? object instanceof String ? (String) object : object.toString() : null;
    }

    @SuppressWarnings("unchecked")
    public Integer getAsInteger(String domain, String key) {
        Object object = get(domain, key);
        if (object instanceof Collection) {
            Collection<Object> collection = (Collection<Object>) object;
            object = collection.iterator().next();
            return object != null ? Integer.parseInt(object.toString()) : null;
        }
        try {
            return object != null ? object instanceof Integer ? (Integer) object : Integer.parseInt(object.toString()) : null;
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public Boolean getAsBoolean(String domain, String key) {
        Object object = get(domain, key);
        if (object instanceof Collection) {
            Collection<Object> collection = (Collection<Object>) object;
            object = collection.iterator().next();
            return object != null ? Boolean.parseBoolean(object.toString()) : null;
        }
        try {
            return object != null ? object instanceof Boolean ? (Boolean) object : Boolean.parseBoolean(object.toString()) : null;
        } catch (Exception e) {
            return null;
        }
    }

    public String allToString() {
        StringBuilder sb = new StringBuilder();
        sb.append(list.toString());
        if (!builder.parameterMap.isEmpty()) {
            builder.parameterMap.forEach((key, value) -> sb.append(" ").append(key).append(" -> ").append(value));
        }
        return sb.toString();
    }

    public boolean hasElements() {
        return !list.isEmpty();
    }

    public MultiMap<String, Object> asMultiMap() {
        MultiMap<String, Object> multiMap = new LinkedHashSetMultiMap<>();
        this.forEach(p -> multiMap.put(p.getKey(), p.getValue()));
        return multiMap;
    }

    public Map<String, Object> asSingleValuedMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        this.forEach(p -> map.put(p.getKey(), createValue(p.getValue())));
        return map;
    }

    @SuppressWarnings("unchecked")
    private static Object createValue(Object object) {
        if (object instanceof Collection) {
            Collection<Object> collection = (Collection<Object>) object;
            if (collection.size() == 1) {
                return collection.iterator().next();
            } else {
                return collection;
            }
        }
        return object;
    }

    public String getDomain() {
        return builder.domain;
    }

    public Stream<Pair<String, Object>> stream(String domain) {
        if (!builder.domain.equals(domain)) {
            throw new IllegalArgumentException("domain mismatch");
        }
        return list.stream();
    }

    public List<Object> getAll(String domain, String key) {
        Optional<Parameter> optional = builder.parameterMap.values().stream().filter(p -> domain.equals(p.getDomain())).findFirst();
        if (optional.isPresent()) {
            return optional.get().getAll(domain, key);
        } else {
            if (!builder.domain.equals(domain)) {
                throw new IllegalArgumentException("domain mismatch");
            }
            return list.stream()
                    .filter(p -> key.equals(p.getKey()))
                    .map(Pair::getValue)
                    .collect(Collectors.toList());
        }
    }

    public boolean containsKey(String domain, String key) {
        Optional<Parameter> optional = builder.parameterMap.values().stream().filter(p -> domain.equals(p.getDomain())).findFirst();
        if (optional.isPresent()) {
            return optional.get().containsKey(domain, key);
        } else {
            if (!builder.domain.equals(domain)) {
                throw new IllegalArgumentException("domain mismatch");
            }
            return list.stream().anyMatch(p -> key.equals(p.getKey()));
        }
    }

    public Object get(String domain, String key) {
        Optional<Parameter> optional = builder.parameterMap.values().stream().filter(p -> domain.equals(p.getDomain())).findFirst();
        if (optional.isPresent()) {
            return optional.get().getAll(domain, key);
        } else {
            if (!builder.domain.equals(domain)) {
                throw new IllegalArgumentException("domain mismatch");
            }
            return list.stream()
                    .filter(p -> key.equals(p.getKey()))
                    .map(Pair::getValue)
                    .findFirst().orElse(null);
        }
    }

    public String getAsQueryString() {
        return queryString;
    }
}
