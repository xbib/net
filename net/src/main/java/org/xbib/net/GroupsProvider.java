package org.xbib.net;

import java.util.Collection;

public abstract class GroupsProvider {

    public GroupsProvider() {
    }

    /**
     * @return list of groups associated with specified user, or null if such user doesn't exist
     * @throws RuntimeException in case of unexpected error such as connection failure
     */
    public Collection<String> getGroups(String username) {
        return null;
    }

    /**
     * Override this method in order to load user group information.
     *
     * @return list of groups associated with specified user, or null if such user doesn't exist
     * @throws RuntimeException in case of unexpected error such as connection failure
     */
    public Collection<String> getGroups(Context context) {
        return getGroups(context.getUsername());
    }

    public static final class Context {

        private final String username;

        private final Request request;

        public Context(String username, Request request) {
            this.username = username;
            this.request = request;
        }

        public String getUsername() {
            return username;
        }

        public Request getRequest() {
            return request;
        }
    }
}