package org.xbib.net.security.ssl.trustmanager;

import javax.net.ssl.ManagerFactoryParameters;

@FunctionalInterface
public interface TrustOptions<T, R extends ManagerFactoryParameters> {

    R apply(T input) throws Exception;

}
