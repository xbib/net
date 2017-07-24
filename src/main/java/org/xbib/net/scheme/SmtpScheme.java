package org.xbib.net.scheme;

/**
 * The SMTP scheme.
 *
 * @see <a href="https://tools.ietf.org/html/rfc5321">SMTP RFC</a>
 */
class SmtpScheme extends AbstractScheme {

    SmtpScheme() {
        super("smtp", 25);
    }

    SmtpScheme(String name, int port) {
        super(name, port);
    }

}
