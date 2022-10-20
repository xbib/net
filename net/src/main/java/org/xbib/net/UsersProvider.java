package org.xbib.net;

public abstract class UsersProvider {

    public UsersProvider() {
    }

    /**
     * Override this method in order to load user details.
     *
     * @return the user, or null if user doesn't exist
     * @throws RuntimeException in case of unexpected error such as connection failure
     */
    public abstract UserDetails getUserDetails(Context context);

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
