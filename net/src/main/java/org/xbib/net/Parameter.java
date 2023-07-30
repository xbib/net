package org.xbib.net;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.xbib.datastructures.common.ImmutableList;
import org.xbib.datastructures.common.LinkedHashSetMultiMap;
import org.xbib.datastructures.common.MultiMap;
import org.xbib.datastructures.common.Pair;

public class Parameter implements Iterable<Pair<String, Object>>, Comparable<Parameter> {

    public enum Domain {
        UNDEFINED,
        QUERY,
        FORM,
        PATH,
        HEADER,
        COOKIE
    }

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

    public static Parameter of(Domain domain, Map<String, Object> map) {
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

    public Domain getDomain() {
        return builder.domain;
    }

    @SuppressWarnings("unchecked")
    public String getAsString(String key, Domain domain) throws ParameterException {
        Object object = get(key, domain);
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
    public Integer getAsInteger(String key, Domain domain) throws ParameterException {
        Object object = get(key, domain);
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
    public Boolean getAsBoolean(String key, Domain domain) throws ParameterException {
        Object object = get(key, domain);
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

    public boolean hasElements() {
        return !list.isEmpty();
    }

    public MultiMap<String, Object> asMultiMap() throws ParameterException{
        if (getDomain() == Domain.UNDEFINED) {
            throw new ParameterException("undefined domain");
        }
        MultiMap<String, Object> multiMap = new LinkedHashSetMultiMap<>();
        this.forEach(p -> multiMap.put(p.getKey(), p.getValue()));
        return multiMap;
    }

    public Map<String, Object> asSingleValuedMap() throws ParameterException {
        if (getDomain() == Domain.UNDEFINED) {
            throw new ParameterException("undefined domain");
        }
        Map<String, Object> map = new LinkedHashMap<>();
        this.forEach(p -> map.put(p.getKey(), createValue(p.getValue())));
        return map;
    }

    public List<Object> getAllInDomain(Domain domain) {
        Parameter parameter = null;
        if (builder.parameterMap.containsKey(domain)) {
            parameter = builder.parameterMap.get(domain);
        }
        if (parameter != null) {
            return parameter.getAllInDomain(domain);
        }
        if (getDomain().equals(domain)) {
            return list.stream()
                    .map(Pair::getValue)
                    .collect(Collectors.toList());
        }
        return null;
    }

    public boolean isPresent(Domain domain) {
        Parameter parameter = null;
        if (builder.parameterMap.containsKey(domain)) {
            parameter = builder.parameterMap.get(domain);
        }
        if (parameter != null) {
            return parameter.isPresent(domain);
        }
        if (getDomain().equals(domain)) {
            return list.stream().findAny().isPresent();
        }
        return false;
    }

    public Parameter get(Domain domain) throws ParameterException {
        if (getDomain() == Domain.UNDEFINED) {
            throw new ParameterException("undefined domain");
        }
        if (builder.parameterMap.containsKey(domain)) {
            return builder.parameterMap.get(domain);
        }
        if (getDomain().equals(domain)) {
            return this;
        }
        return null;
    }

    public List<Object> getAll(String key, Domain domain) throws Exception {
        if (getDomain() == Domain.UNDEFINED) {
            throw new ParameterException("undefined domain");
        }
        Parameter parameter = null;
        if (builder.parameterMap.containsKey(domain)) {
            parameter = builder.parameterMap.get(domain);
        }
        if (parameter != null) {
            return parameter.getAll(key, domain);
        }
        if (getDomain().equals(domain)) {
            return list.stream()
                    .filter(p -> p.getKey().equals(key))
                    .map(Pair::getValue)
                    .collect(Collectors.toList());
        }
        return null;
    }

    public boolean containsKey(String key, Domain domain) {
        Parameter parameter = null;
        if (builder.parameterMap.containsKey(domain)) {
            parameter = builder.parameterMap.get(domain);
        }
        if (parameter != null) {
            return parameter.containsKey(key, domain);
        }
        if (getDomain().equals(domain)) {
            return list.stream()
                    .anyMatch(p -> key.equals(p.getKey()));
        }
        return false;
    }

    public Object get(String key, Domain domain) throws ParameterException {
        if (getDomain() == Domain.UNDEFINED) {
            throw new ParameterException("undefined domain");
        }
        Parameter parameter = null;
        if (builder.parameterMap.containsKey(domain)) {
            parameter = builder.parameterMap.get(domain);
        }
        if (parameter != null) {
            return parameter.get(key, domain);
        }
        if (getDomain().equals(domain)) {
            Optional<Object> optional = list.stream()
                    .filter(p -> key.equals(p.getKey()))
                    .map(Pair::getValue)
                    .findFirst();
            if (optional.isPresent()) {
                return optional.get();
            }
        }
        return null;
    }

    public String getAsQueryString() {
        return queryString;
    }

    private String allToString() {
        StringBuilder sb = new StringBuilder();
        sb.append(list.toString());
        builder.parameterMap.forEach((key, value) -> sb.append(" ").append(key).append(" -> ").append(value));
        return sb.toString();
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
}
