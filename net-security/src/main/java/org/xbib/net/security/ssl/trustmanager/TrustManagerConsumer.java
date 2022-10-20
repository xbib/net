package org.xbib.net.security.ssl.trustmanager;

import javax.net.ssl.X509ExtendedTrustManager;
import java.security.cert.CertificateException;

/**
 * <strong>NOTE:</strong>
 * Please don't use this class directly as it is part of the internal API. Class name and methods can be changed any time.
 */
@FunctionalInterface
interface TrustManagerConsumer {

    void checkTrusted(X509ExtendedTrustManager trustManager) throws CertificateException;

}
