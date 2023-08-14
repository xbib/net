package org.xbib.net;

import java.io.IOException;
import java.util.function.Consumer;

public interface Store<S> {

    String getName();

    long size() throws IOException;

    S read(String key) throws IOException;

    void readAll(String key, Consumer<S> consumer) throws IOException;

    void write(String key, S s) throws IOException;

    void remove(String key) throws IOException;

    void purge(long expiredAfterSeconds) throws IOException;
}
