package org.xbib.net.scheme;

/**
 * The POP3 scheme.
 *
 * @see <a href="https://tools.ietf.org/html/rfc1081">POP3 RFC</a>
 */
class Pop3Scheme extends AbstractScheme {

    Pop3Scheme() {
        super("pop3", 110);
    }

    Pop3Scheme(String name, int port) {
        super(name, port);
    }

}
