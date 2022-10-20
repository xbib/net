package org.xbib.net.scheme;

/**
 * The IMAP scheme.
 *
 * @see <a href="https://tools.ietf.org/html/rfc5092">IMAP RFC</a>
 */
class ImapScheme extends AbstractScheme {

    ImapScheme() {
        super("imap", 143);
    }

    ImapScheme(String name, int port) {
        super(name, port);
    }
}
