package org.xbib.net.scheme;

/**
 * The IMAP scheme.
 * @see <a href="https://tools.ietf.org/html/rfc5092">IMAP scheme RFC</a>
 */
class SecureImapScheme extends ImapScheme {

    SecureImapScheme() {
        super("imaps", 993);
    }

}
