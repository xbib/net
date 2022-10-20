package org.xbib.net.scheme;

/**
 * The LDAP scheme.
 * @see <a href="https://tools.ietf.org/html/rfc4516">LDAP RFC</a>
 */
class LdapScheme extends AbstractScheme {

    LdapScheme() {
        super("ldap", 143);
    }

    LdapScheme(String name, int port) {
        super(name, port);
    }

}
