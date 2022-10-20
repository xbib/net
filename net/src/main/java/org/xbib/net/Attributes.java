package org.xbib.net;

import java.util.Map;

public interface Attributes extends Map<String, Object> {

    <T> T get(Class<T> cl, String key);

    <T> T get(Class<T> cl, String key, T defaultValue);
}
