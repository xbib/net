package org.xbib.net;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;
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
        return list.toString().compareTo(o.list.toString());
    }

    @Override
    public String toString() {
        return allToString();
    }

    public String getDomain() {
        return builder.domain;
    }


    @SuppressWarnings("unchecked")
    public String getAsString(String key, String... domains) {
        Object object = get(key, domains);
        if (object instanceof Collection) {
            Collection<Object> collection = (Collection<Object>) object;
            Iterator<Object> iterator = collection.iterator();
            if (iterator.hasNext()) {
                object = iterator.next();
            } else {
                object = null;
            }
            return object != null ? object.toString() : null;
        }
        return object != null ? object instanceof String ? (String) object : object.toString() : null;
    }

    @SuppressWarnings("unchecked")
    public Integer getAsInteger(String key, String... domains) {
        Object object = get(key, domains);
        if (object instanceof Collection) {
            Collection<Object> collection = (Collection<Object>) object;
            Iterator<Object> iterator = collection.iterator();
            if (iterator.hasNext()) {
                object = iterator.next();
            } else {
                object = null;
            }
            return object != null ? Integer.parseInt(object.toString()) : null;
        }
        try {
            return object != null ? object instanceof Integer ? (Integer) object : Integer.parseInt(object.toString()) : null;
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public Boolean getAsBoolean(String key, String... domains) {
        Object object = get(key, domains);
        if (object instanceof Collection) {
            Collection<Object> collection = (Collection<Object>) object;
            Iterator<Object> iterator = collection.iterator();
            if (iterator.hasNext()) {
                object = iterator.next();
            } else {
                object = null;
            }
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
        builder.parameterMap.forEach((key, value) -> sb.append(" ").append(key).append(" -> ").append(value));
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

    public List<Object> getAllDomain(String... domains) {
        Parameter parameter = null;
        for (String domain : domains) {
            if (builder.parameterMap.containsKey(domain)) {
                parameter = builder.parameterMap.get(domain);
            }
            if (parameter != null) {
                List<Object> list = parameter.getAllDomain(domains);
                if (list != null) {
                    return list;
                }
            }
            return list.stream()
                    .map(Pair::getValue)
                    .collect(Collectors.toList());
        }
        return null;
    }

    public boolean isPresent(String... domains) {
        Parameter parameter = null;
        for (String domain : domains) {
            if (builder.parameterMap.containsKey(domain)) {
                parameter = builder.parameterMap.get(domain);
            }
            if (parameter != null) {
                boolean b = parameter.isPresent(domains);
                if (b) {
                    return true;
                }
            }
            return list.stream().findAny().isPresent();
        }
        return false;
    }

    public List<Object> getAll(String key, String... domains) {
        Parameter parameter = null;
        for (String domain : domains) {
            if (builder.parameterMap.containsKey(domain)) {
                parameter = builder.parameterMap.get(domain);
            }
            if (parameter != null) {
                List<Object> list = parameter.getAll(key, domains);
                if (list != null) {
                    return list;
                }
            }
            return list.stream()
                    .filter(p -> p.getKey().equals(key))
                    .map(Pair::getValue)
                    .collect(Collectors.toList());
        }
        return null;
    }

    public boolean containsKey(String key, String... domains) {
        Parameter parameter = null;
        for (String domain : domains) {
            if (builder.parameterMap.containsKey(domain)) {
                parameter = builder.parameterMap.get(domain);
            }
            if (parameter != null) {
                boolean b = parameter.containsKey(key, domains);
                if (b) {
                    return true;
                }
            }
            return list.stream()
                    .anyMatch(p -> key.equals(p.getKey()));
        }
        return false;
    }

    public Object get(String key, String... domains) {
        Parameter parameter = null;
        for (String domain : domains) {
            if (builder.parameterMap.containsKey(domain)) {
                parameter = builder.parameterMap.get(domain);
            }
            if (parameter != null) {
                Object object = parameter.get(key, domains);
                if (object != null) {
                    return object;
                }
            } else {
                Optional<Object> optional = list.stream()
                        .filter(p -> key.equals(p.getKey()))
                        .map(Pair::getValue)
                        .findFirst();
                if (optional.isPresent()) {
                    return optional.get();
                }
            }
        }
        return null;
    }

    public String getAsQueryString() {
        return queryString;
    }
}
