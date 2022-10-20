package org.xbib.net;

import java.io.IOException;

@SuppressWarnings("rawtypes")
@FunctionalInterface
public interface Handler<C extends Context> {

    void handle(C context) throws IOException;
}
