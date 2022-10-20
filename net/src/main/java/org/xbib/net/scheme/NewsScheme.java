package org.xbib.net.scheme;

/**
 * The news scheme.
 *
 * @see <a href="https://tools.ietf.org/html/rfc5538">news RFC</a>
 */
class NewsScheme extends AbstractScheme {

    NewsScheme() {
        super("nntp", 119);
    }

    NewsScheme(String name, int port) {
        super(name, port);
    }

}
