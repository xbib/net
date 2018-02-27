package org.xbib.net.scheme;

/**
 * Secure FTP scheme.
 */
class SftpScheme extends SshScheme {

    SftpScheme() {
        super("sftp", 22);
    }

}
