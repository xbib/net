package org.xbib.net.scheme;

/**
 * The TFTP scheme.
 *
 * @see <a href="https://tools.ietf.org/html/rfc1350">TFTP RFC</a>
 */
class TftpScheme extends FtpScheme {

    TftpScheme() {
        super("tftp", 69);
    }
}
