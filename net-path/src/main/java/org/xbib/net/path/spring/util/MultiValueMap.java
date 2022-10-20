package org.xbib.net.path.spring.util;

import java.util.List;
import java.util.Map;

/**
 * Extension of the {@code Map} interface that stores multiple values.
 *
 * @param <K> the key type
 * @param <V> the value element type
 */
public interface MultiValueMap<K, V> extends Map<K, List<V>> {

	/**
	 * Add the given single value to the current list of values for the given key.
	 * @param key the key
	 * @param value the value to be added
	 */
	void add(K key, V value);

	/**
	 * Add all the values of the given list to the current list of values for the given key.
	 * @param key they key
	 * @param values the values to be added
	 */
	void addAll(K key, List<? extends V> values);

	/**
	 * Add all the values of the given {@code MultiValueMap} to the current values.
	 * @param values the values to be added
	 */
	void addAll(MultiValueMap<K, V> values);

}
