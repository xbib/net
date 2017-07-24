package org.xbib.net.scheme;

/**
 * The IRC scheme.
 *
 * @see <a href="https://www.w3.org/Addressing/draft-mirashi-url-irc-01.txt">IRC draft</a>
 */
class IrcScheme extends HttpScheme {

    IrcScheme() {
        super("irc", 194);
    }

    IrcScheme(String name, int port) {
        super(name, port);
    }

}
