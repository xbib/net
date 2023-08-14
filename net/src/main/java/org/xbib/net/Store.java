package org.xbib.net;

import java.io.IOException;
import java.util.Map;
import java.util.function.Consumer;

public interface Store<S> {

    String getName();

    long size() throws IOException;

    S read(String key) throws IOException;

    void readAll(String prefix, Consumer<S> consumer) throws IOException;

    void write(String key, S s) throws IOException;

    void write(String prefix, String key, S s) throws IOException;

    void update(String key, Map<String, Object> map) throws IOException;

    void update(String prefix, String key, Map<String, Object> map) throws IOException;

    void remove(String key) throws IOException;

    void purge(long expiredAfterSeconds) throws IOException;

    void destroy() throws IOException;
}
