package org.xbib.net.scheme;

/**
 * The secure POP3 scheme.
 *
 * @see <a href="https://tools.ietf.org/html/rfc2595">POP3 RFC</a>
 */
class SecurePop3Scheme extends Pop3Scheme {

    SecurePop3Scheme() {
        super("pop3s", 995);
    }
}
