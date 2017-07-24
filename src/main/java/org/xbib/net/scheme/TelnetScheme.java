package org.xbib.net.scheme;

/**
 * The TELNET scheme.
 *
 * @see <a href="https://tools.ietf.org/html/rfc4248">TELNET RFC</a>
 */
class TelnetScheme extends AbstractScheme {

    TelnetScheme() {
        super("telnet", 23);
    }

}
