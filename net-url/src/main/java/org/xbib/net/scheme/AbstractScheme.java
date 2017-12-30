package org.xbib.net.scheme;

import org.xbib.net.URL;

/**
 * Base implementation for scheme.
 */
public abstract class AbstractScheme implements Scheme {

    protected final String name;

    protected final int defaultPort;

    protected AbstractScheme(String name, int defaultPort) {
        this.name = name;
        this.defaultPort = defaultPort;
    }

    @Override
    public int getDefaultPort() {
        return defaultPort;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public URL normalize(URL url) {
        return url;
    }

}
