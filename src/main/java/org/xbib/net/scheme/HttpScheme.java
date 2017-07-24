package org.xbib.net.scheme;

import org.xbib.net.URL;
import org.xbib.net.path.PathNormalizer;

/**
 *
 */
class HttpScheme extends AbstractScheme {

    HttpScheme() {
        super("http", 80);
    }

    HttpScheme(String name, int port) {
        super(name, port);
    }

    @Override
    public URL normalize(URL url) {
        String host = url.getHost();
        if (host != null) {
            host = host.toLowerCase();
        }
        return URL.builder()
                .scheme(url.getScheme())
                .userInfo(url.getUserInfo())
                .host(host, url.getProtocolVersion())
                .port(url.getPort())
                .path(PathNormalizer.normalize(url.getPath()))
                .query(url.getQuery()/*PercentEncoders.getQueryEncoder().encode(url.getDecodedQuery())*/)
                .fragment(url.getFragment()/*PercentEncoders.getFragmentEncoder().encode(url.getDecodedFragment())*/)
                .build();
    }
}
