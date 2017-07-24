package org.xbib.net.scheme;

/**
 *
 */
class GitSecureHttpScheme extends HttpScheme {

    GitSecureHttpScheme() {
        super("git+https", 443);
    }

    GitSecureHttpScheme(String name, int port) {
        super(name, port);
    }

}
