package org.xbib.net.scheme;

/**
 * The nttp scheme.
 *
 * @see <a href="https://tools.ietf.org/html/rfc5538">NNTP RFC</a>
 */
class NntpScheme extends AbstractScheme {

    NntpScheme() {
        super("nntp", 119);
    }

}
