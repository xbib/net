package org.xbib.net.security.ssl.util;

import org.xbib.net.security.ssl.SSLFactory;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;

public final class SSLFactoryUtils {

    private SSLFactoryUtils() {}

    /**
     * Reloads the ssl material for the KeyManager and / or TrustManager within the base SSLFactory if present and if it is swappable.
     * Other properties such as ciphers, protocols, secure-random, {@link javax.net.ssl.HostnameVerifier} and {@link javax.net.ssl.SSLParameters} will not be reloaded.
     */
    public static void reload(SSLFactory baseSslFactory, SSLFactory updatedSslFactory) {
        reload(baseSslFactory, updatedSslFactory, SSLFactory::getKeyManager, KeyManagerUtils::swapKeyManager);
        reload(baseSslFactory, updatedSslFactory, SSLFactory::getTrustManager, TrustManagerUtils::swapTrustManager);
        SSLSessionUtils.invalidateCaches(baseSslFactory);
    }

    private static <T> void reload(SSLFactory baseSslFactory,
                                   SSLFactory updatedSslFactory,
                                   Function<SSLFactory, Optional<T>> mapper, BiConsumer<T, T> consumer) {

        Optional<T> baseManager = mapper.apply(baseSslFactory);
        Optional<T> updatedManager = mapper.apply(updatedSslFactory);
        if (baseManager.isPresent() && updatedManager.isPresent()) {
            consumer.accept(baseManager.get(), updatedManager.get());
        }
    }

}
