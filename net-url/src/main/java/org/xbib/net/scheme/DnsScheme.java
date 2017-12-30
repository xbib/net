package org.xbib.net.scheme;

/**
 * The DNS URI scheme.
 * @see <a href="https://www.w3.org/Addressing/draft-mirashi-url-irc-01.txt">DNS RFC</a>
 */
class DnsScheme extends HttpScheme {

    DnsScheme() {
        super("dns", 53);
    }

    DnsScheme(String name, int port) {
        super(name, port);
    }

}
