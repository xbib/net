package org.xbib.net.security.jaas;

import java.io.IOException;
import java.security.Principal;
import java.util.Map;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

public class PlainLoginModule implements LoginModule {

    private CallbackHandler handler;

    private Subject subject;

    private String login;

    public PlainLoginModule() {
    }

    /**
     * This implementation always return false.
     *
     * @see javax.security.auth.spi.LoginModule#abort()
     */
    @Override
    public boolean abort() throws LoginException {
        return false;
    }

    /**
     * This is where, should the entire authentication process succeeds,
     * principal would be set.
     *
     * @see javax.security.auth.spi.LoginModule#commit()
     */
    @Override
    public boolean commit() throws LoginException {

        try {
            PlainUserPrincipal user = new PlainUserPrincipal(login);
            PlainRolePrincipal role = new PlainRolePrincipal("admin");
            subject.getPrincipals().add(user);
            subject.getPrincipals().add(role);
            return true;
        } catch (Exception e) {
            throw new LoginException(e.getMessage());
        }
    }

    /**
     * This implementation ignores both state and options.
     * @see javax.security.auth.spi.LoginModule#initialize(javax.security.auth.Subject,
     *      javax.security.auth.callback.CallbackHandler, java.util.Map,
     *      java.util.Map)
     */
    @Override
    public void initialize(Subject aSubject, CallbackHandler aCallbackHandler, Map<String, ?> aSharedState, Map<String, ?> aOptions) {
        handler = aCallbackHandler;
        subject = aSubject;
    }

    /**
     * This method checks whether the name and the password are the same.
     * @see javax.security.auth.spi.LoginModule#login()
     */
    @Override
    public boolean login() throws LoginException {
        Callback[] callbacks = new Callback[2];
        callbacks[0] = new NameCallback("login");
        callbacks[1] = new PasswordCallback("password", true);
        try {
            handler.handle(callbacks);
            String name = ((NameCallback) callbacks[0]).getName();
            String password = String.valueOf(((PasswordCallback) callbacks[1]).getPassword());
            if (!name.equals(password)) {
                throw new LoginException("Authentication failed");
            }
            login = name;
            return true;
        } catch (IOException | UnsupportedCallbackException e) {
            throw new LoginException(e.getMessage());
        }
    }

    /**
     * Clears subject from principal and credentials.
     *
     * @see javax.security.auth.spi.LoginModule#logout()
     */
    @Override
    public boolean logout() throws LoginException {
        try {
            Principal user = new PlainUserPrincipal(login);
            Principal role = new PlainRolePrincipal("admin");
            subject.getPrincipals().remove(user);
            subject.getPrincipals().remove(role);
            return true;
        } catch (Exception e) {
            throw new LoginException(e.getMessage());
        }
    }
}

