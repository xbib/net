package org.xbib.net.security.ssl.util;

import java.net.URI;

import static java.util.Objects.isNull;

public final class UriUtils {

    private UriUtils() {}

    public static void validate(URI uri) {
        if (isNull(uri)) {
            throw new IllegalArgumentException("Host should be present");
        }

        if (isNull(uri.getHost())) {
            throw new IllegalArgumentException(String.format("Hostname should be defined for the given input: [%s]", uri));
        }

        if (uri.getPort() == -1) {
            throw new IllegalArgumentException(String.format("Port should be defined for the given input: [%s]", uri));
        }
    }
}
