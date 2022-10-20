package org.xbib.net;

public abstract class SecurityRealm {

    public SecurityRealm() {
    }

    /**
     * @return unique name of this realm, e.g. "ldap"
     */
    public abstract  String getName();

    /**
     * Invoked during server startup and can be used to initialize internal state.
     */
    public void init() {
    }

    public abstract Authenticator getAuthenticator();

    /**
     * @return {@link UsersProvider} associated with this realm, null if not supported
     */
    public UsersProvider getUsersProvider() {
        return null;
    }

    /**
     * @return {@link GroupsProvider} associated with this realm, null if not supported
     */
    public GroupsProvider getGroupsProvider() {
        return null;
    }
}
