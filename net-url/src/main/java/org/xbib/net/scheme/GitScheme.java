package org.xbib.net.scheme;

/**
 *
 */
class GitScheme extends HttpScheme {

    GitScheme() {
        super("git", 443);
    }

    GitScheme(String name, int port) {
        super(name, port);
    }

}
