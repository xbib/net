package org.xbib.net;

public abstract class Authenticator {

    public Authenticator() {
    }

    /**
     * Authenticate.
     * @return true if user was successfully authenticated with specified credentials, false otherwise
     * @throws RuntimeException in case of unexpected error such as connection failure
     */
    public abstract boolean authenticate(Context context);

    public static final class Context {

        private final String username;

        private final String password;

        private final Request request;

        public Context(String username, String password, Request request) {
            this.request = request;
            this.username = username;
            this.password = password;
        }

        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return password;
        }

        public Request getRequest() {
            return request;
        }
    }
}
