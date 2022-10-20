package org.xbib.net.path.spring.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Adapts a given {@link Map} to the {@link MultiValueMap} contract.
 *
 * @param <K> the key type
 * @param <V> the value element type
 * @see LinkedMultiValueMap
 */
@SuppressWarnings("serial")
public class MultiValueMapAdapter<K, V> implements MultiValueMap<K, V> {

	private final Map<K, List<V>> targetMap;

	/**
	 * Wrap the given target {@link Map} as a {@link MultiValueMap} adapter.
	 * @param targetMap the plain target {@code Map}
	 */
	public MultiValueMapAdapter(Map<K, List<V>> targetMap) {
		//Assert.notNull(targetMap, "'targetMap' must not be null");
		this.targetMap = targetMap;
	}

	@Override
	public void add(K key, V value) {
		List<V> values = this.targetMap.computeIfAbsent(key, k -> new ArrayList<>(1));
		values.add(value);
	}

	@Override
	public void addAll(K key, List<? extends V> values) {
		List<V> currentValues = this.targetMap.computeIfAbsent(key, k -> new ArrayList<>(1));
		currentValues.addAll(values);
	}

	@Override
	public void addAll(MultiValueMap<K, V> values) {
		for (Entry<K, List<V>> entry : values.entrySet()) {
			addAll(entry.getKey(), entry.getValue());
		}
	}

	@Override
	public int size() {
		return this.targetMap.size();
	}

	@Override
	public boolean isEmpty() {
		return this.targetMap.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return this.targetMap.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return this.targetMap.containsValue(value);
	}

	@Override
	public List<V> get(Object key) {
		return this.targetMap.get(key);
	}

	@Override
	public List<V> put(K key, List<V> value) {
		return this.targetMap.put(key, value);
	}

	@Override
	public List<V> remove(Object key) {
		return this.targetMap.remove(key);
	}

	@Override
	public void putAll(Map<? extends K, ? extends List<V>> map) {
		this.targetMap.putAll(map);
	}

	@Override
	public void clear() {
		this.targetMap.clear();
	}

	@Override
	public Set<K> keySet() {
		return this.targetMap.keySet();
	}

	@Override
	public Collection<List<V>> values() {
		return this.targetMap.values();
	}

	@Override
	public Set<Entry<K, List<V>>> entrySet() {
		return this.targetMap.entrySet();
	}

	@Override
	public boolean equals(Object other) {
		return (this == other || this.targetMap.equals(other));
	}

	@Override
	public int hashCode() {
		return this.targetMap.hashCode();
	}

	@Override
	public String toString() {
		return this.targetMap.toString();
	}

}
