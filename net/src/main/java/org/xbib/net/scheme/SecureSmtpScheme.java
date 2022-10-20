package org.xbib.net.scheme;

/**
 * The secure SMTP scheme.
 *
 * @see <a href="https://tools.ietf.org/html/rfc4409">SMTP RFC</a>
 */
class SecureSmtpScheme extends SmtpScheme {

    SecureSmtpScheme() {
        super("smtps", 587);
    }

}
