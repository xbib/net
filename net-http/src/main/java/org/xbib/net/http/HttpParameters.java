package org.xbib.net.http;

import org.xbib.net.PercentDecoder;
import org.xbib.net.PercentEncoder;
import org.xbib.net.PercentEncoders;
import org.xbib.net.http.util.LimitedSortedStringSet;
import org.xbib.net.http.util.LimitedStringMap;

import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnmappableCharacterException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

/**
 * A limited multi-map of HTTP request parameters. Each key references a
 * limited set of parameters collected from the request during message
 * signing. Parameter values are sorted as per
 * <a href="http://oauth.net/core/1.0a/#anchor13">OAuth specification</a></a>.
 * Every key/value pair will be percent-encoded upon insertion.
 *  This class has special semantics tailored to
 * being useful for message signing; it's not a general purpose collection class
 * to handle request parameters.
 */
public class HttpParameters implements Map<String, SortedSet<String>> {

    private final LimitedStringMap wrappedMap;

    private final PercentEncoder percentEncoder;

    private final PercentDecoder percentDecoder;

    public HttpParameters() {
        this.wrappedMap = new LimitedStringMap();
        this.percentEncoder = PercentEncoders.getQueryEncoder(StandardCharsets.UTF_8);
        this.percentDecoder = new PercentDecoder();
    }

    @Override
    public SortedSet<String> put(String key, SortedSet<String> value) {
        return wrappedMap.put(key, value);
    }

    @Override
    public SortedSet<String> get(Object key) {
        return wrappedMap.get(key);
    }

    @Override
    public void putAll(Map<? extends String, ? extends SortedSet<String>> m) {
        wrappedMap.putAll(m);
    }

    @Override
    public boolean containsKey(Object key) {
        return wrappedMap.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        if (value instanceof String) {
            for (Set<String> values : wrappedMap.values()) {
                if (values.contains(value)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public int size() {
        int count = 0;
        for (String key : wrappedMap.keySet()) {
            count += wrappedMap.get(key).size();
        }
        return count;
    }

    @Override
    public boolean isEmpty() {
        return wrappedMap.isEmpty();
    }

    @Override
    public void clear() {
        wrappedMap.clear();
    }

    @Override
    public SortedSet<String> remove(Object key) {
        return wrappedMap.remove(key);
    }

    @Override
    public Set<String> keySet() {
        return wrappedMap.keySet();
    }

    @Override
    public Collection<SortedSet<String>> values() {
        return wrappedMap.values();
    }

    @Override
    public Set<Entry<String, SortedSet<String>>> entrySet() {
        return wrappedMap.entrySet();
    }

    public SortedSet<String> put(String key, SortedSet<String> values, boolean percentEncode)
            throws MalformedInputException, UnmappableCharacterException {
        if (percentEncode) {
            remove(key);
            for (String v : values) {
                put(key, v, true);
            }
            return get(key);
        } else {
            return wrappedMap.put(key, values);
        }
    }

    /**
     * Convenience method to add a single value for the parameter specified by 'key'.
     *
     * @param key the parameter name
     * @param value the parameter value
     * @return the value
     */
    public String put(String key, String value)
            throws MalformedInputException, UnmappableCharacterException {
        return put(key, value, false);
    }

    /**
     * Convenience method to add a single value for the parameter specified by
     * 'key'.
     *
     * @param key the parameter name
     * @param value the parameter value
     * @param percentEncode whether key and value should be percent encoded before being
     *        inserted into the map
     * @return the value
     */
    public String put(String key, String value, boolean percentEncode)
            throws MalformedInputException, UnmappableCharacterException {
        String k  = percentEncode ? percentEncoder.encode(key) : key;
        SortedSet<String> values = wrappedMap.get(k);
        if (values == null) {
            values = new LimitedSortedStringSet();
            wrappedMap.put(k, values);
        }
        String v = null;
        if (value != null) {
            v = percentEncode ? percentEncoder.encode(value) : value;
            values.add(v);
        }
        return v;
    }

    /**
     * Convenience method to allow for storing null values. {@link #put} doesn't
     * allow null values, because that would be ambiguous.
     *
     * @param key the parameter name
     * @param nullString can be anything, but probably... null?
     * @return null
     */
    public String putNull(String key, String nullString)
            throws MalformedInputException, UnmappableCharacterException {
        return put(key, nullString);
    }

    public void putAll(Map<? extends String, ? extends SortedSet<String>> m, boolean percentEncode)
            throws MalformedInputException, UnmappableCharacterException {
        if (percentEncode) {
            for (String key : m.keySet()) {
                put(key, m.get(key), true);
            }
        } else {
            wrappedMap.putAll(m);
        }
    }

    public void putAll(String[] keyValuePairs, boolean percentEncode)
            throws MalformedInputException, UnmappableCharacterException {
        for (int i = 0; i < keyValuePairs.length - 1; i += 2) {
            this.put(keyValuePairs[i], keyValuePairs[i + 1], percentEncode);
        }
    }

    /**
     * Convenience method to merge a Map<String, List<String>>.
     *
     * @param m the map
     */
    public void putMap(Map<String, List<String>> m) {
        for (String key : m.keySet()) {
            SortedSet<String> vals = get(key);
            if (vals == null) {
                vals = new LimitedSortedStringSet();
                put(key, vals);
            }
            vals.addAll(m.get(key));
        }
    }


    public String getFirst(String key)
            throws MalformedInputException, UnmappableCharacterException {
        return getFirst(key, false);
    }

    /**
     * Returns the first value from the set of all values for the given
     * parameter name. If the key passed to this method contains special
     * characters, you must first percent encode it, otherwise the lookup will fail
     * (that's because upon storing values in this map, keys get
     * percent-encoded).
     *
     * @param key the parameter name (must be percent encoded if it contains unsafe
     *        characters!)
     * @param percentDecode whether the value being retrieved should be percent decoded
     * @return the first value found for this parameter
     */
    public String getFirst(String key, boolean percentDecode)
            throws MalformedInputException, UnmappableCharacterException {
        SortedSet<String> values = wrappedMap.get(key);
        if (values == null || values.isEmpty()) {
            return null;
        }
        String value = values.first();
        return percentDecode ? percentDecoder.decode(value) : value;
    }

    /**
     * Concatenates all values for the given key to a list of key/value pairs
     * suitable for use in a URL query string.
     *
     * @param key the parameter name
     * @return the query string
     */
    public String getAsQueryString(String key)
            throws MalformedInputException, UnmappableCharacterException {
        return getAsQueryString(key, true);
    }

    /**
     * Concatenates all values for the given key to a list of key/value pairs
     * suitable for use in a URL query string.
     *
     * @param key the parameter name
     * @param percentEncode whether key should be percent encoded before being
     *        used with the map
     * @return the query string
     */
    public String getAsQueryString(String key, boolean percentEncode)
            throws MalformedInputException, UnmappableCharacterException {
        String k = percentEncode ? percentEncoder.encode(key) : key;
        SortedSet<String> values = wrappedMap.get(k);
        if (values == null) {
            return k + "=";
        }
        Iterator<String> it = values.iterator();
        StringBuilder sb = new StringBuilder();
        while (it.hasNext()) {
            sb.append(k).append("=").append(it.next());
            if (it.hasNext()) {
                sb.append("&");
            }
        }
        return sb.toString();
    }

    public String getAsHeaderElement(String key)
            throws MalformedInputException, UnmappableCharacterException {
        String value = getFirst(key);
        if (value == null) {
            return null;
        }
        return key + "=\"" + value + "\"";
    }

    public HttpParameters getOAuthParameters() {
        HttpParameters oauthParams = new HttpParameters();
        for (Entry<String, SortedSet<String>> param : this.entrySet()) {
            String key = param.getKey();
            if (key.startsWith("oauth_") || key.startsWith("x_oauth_")) {
                oauthParams.put(key, param.getValue());
            }
        }
        return oauthParams;
    }
}
