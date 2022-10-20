package org.xbib.net.scheme;

/**
 * The secure news scheme.
 *
 * @see <a href="https://tools.ietf.org/html/rfc5538">news RFC</a>
 */
class SecureNewsScheme extends NewsScheme {

    SecureNewsScheme() {
        super("snews", 563);
    }

}
