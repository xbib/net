package org.xbib.net.security.ssl.util;

import org.xbib.net.security.ssl.exception.GenericKeyManagerException;
import org.xbib.net.security.ssl.keymanager.CompositeX509ExtendedKeyManager;
import org.xbib.net.security.ssl.keymanager.DummyX509ExtendedKeyManager;
import org.xbib.net.security.ssl.keymanager.HotSwappableX509ExtendedKeyManager;
import org.xbib.net.security.ssl.keymanager.KeyManagerFactoryWrapper;
import org.xbib.net.security.ssl.keymanager.X509KeyManagerWrapper;
import org.xbib.net.security.ssl.model.KeyStoreHolder;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.X509ExtendedKeyManager;
import javax.net.ssl.X509KeyManager;
import java.net.URI;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public final class KeyManagerUtils {

    private KeyManagerUtils() {}

    public static X509ExtendedKeyManager combine(X509KeyManager... keyManagers) {
        return combine(Arrays.asList(keyManagers));
    }

    public static X509ExtendedKeyManager combine(List<? extends X509KeyManager> keyManagers) {
        return KeyManagerUtils.keyManagerBuilder()
                .withKeyManagers(keyManagers)
                .build();
    }

    public static <T extends X509KeyManager> X509ExtendedKeyManager[] toArray(T keyManager) {
        return new X509ExtendedKeyManager[]{KeyManagerUtils.wrapIfNeeded(keyManager)};
    }

    public static X509ExtendedKeyManager createKeyManager(KeyStoreHolder... keyStoreHolders) {
        return Arrays.stream(keyStoreHolders)
                .map(keyStoreHolder -> createKeyManager(keyStoreHolder.getKeyStore(), keyStoreHolder.getKeyPassword()))
                .collect(Collectors.collectingAndThen(Collectors.toList(), KeyManagerUtils::combine));
    }

    public static X509ExtendedKeyManager createKeyManager(KeyStore keyStore, char[] keyPassword) {
        return createKeyManager(keyStore, keyPassword, KeyManagerFactory.getDefaultAlgorithm());
    }

    public static X509ExtendedKeyManager createKeyManager(KeyStore keyStore, char[] keyPassword, String keyManagerFactoryAlgorithm) {
        try {
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(keyManagerFactoryAlgorithm);
            return createKeyManager(keyStore, keyPassword, keyManagerFactory);
        } catch (NoSuchAlgorithmException e) {
            throw new GenericKeyManagerException(e);
        }
    }

    public static X509ExtendedKeyManager createKeyManager(KeyStore keyStore, char[] keyPassword, String keyManagerFactoryAlgorithm, String securityProviderName) {
        try {
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(keyManagerFactoryAlgorithm, securityProviderName);
            return createKeyManager(keyStore, keyPassword, keyManagerFactory);
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            throw new GenericKeyManagerException(e);
        }
    }

    public static X509ExtendedKeyManager createKeyManager(KeyStore keyStore, char[] keyPassword, String keyManagerFactoryAlgorithm, Provider securityProvider) {
        try {
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(keyManagerFactoryAlgorithm, securityProvider);
            return createKeyManager(keyStore, keyPassword, keyManagerFactory);
        } catch (NoSuchAlgorithmException e) {
            throw new GenericKeyManagerException(e);
        }
    }

    public static X509ExtendedKeyManager createKeyManager(KeyStore keyStore, char[] keyPassword, KeyManagerFactory keyManagerFactory) {
        try {
            keyManagerFactory.init(keyStore, keyPassword);
            return KeyManagerUtils.getKeyManager(keyManagerFactory);
        } catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException e) {
            throw new GenericKeyManagerException(e);
        }
    }

    public static X509ExtendedKeyManager createKeyManager(KeyStore keyStore, Map<String, char[]> aliasToPassword) {
        List<X509ExtendedKeyManager> keyManagers = new ArrayList<>();

        for (Entry<String, char[]> entry : aliasToPassword.entrySet()) {
            try {
                String alias = entry.getKey();
                char[] password = entry.getValue();

                if (keyStore.isKeyEntry(alias)) {
                    Key key = keyStore.getKey(alias, password);
                    Certificate[] certificateChain = keyStore.getCertificateChain(alias);

                    KeyStore identityStore = KeyStoreUtils.createIdentityStore(key, password, certificateChain);
                    X509ExtendedKeyManager keyManager = KeyManagerUtils.createKeyManager(identityStore, password);
                    keyManagers.add(keyManager);
                }
            } catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException e) {
                throw new GenericKeyManagerException(e);
            }
        }

        ValidationUtils.requireNotEmpty(keyManagers, () -> new GenericKeyManagerException("Could not create any KeyManager from the given KeyStore, Alias and Password"));
        return KeyManagerUtils.combine(keyManagers);
    }

    public static X509ExtendedKeyManager wrapIfNeeded(X509KeyManager keyManager) {
        if (keyManager instanceof X509ExtendedKeyManager) {
            return (X509ExtendedKeyManager) keyManager;
        } else {
            return new X509KeyManagerWrapper(keyManager);
        }
    }

    public static KeyManagerFactory createKeyManagerFactory(KeyManager keyManager) {
        return new KeyManagerFactoryWrapper(keyManager);
    }

    public static <T extends KeyManagerFactory> X509ExtendedKeyManager getKeyManager(T keyManagerFactory) {
        return Arrays.stream(keyManagerFactory.getKeyManagers())
                .filter(X509KeyManager.class::isInstance)
                .map(X509KeyManager.class::cast)
                .map(KeyManagerUtils::wrapIfNeeded)
                .collect(Collectors.collectingAndThen(Collectors.toList(), KeyManagerUtils::combine));
    }

    public static X509ExtendedKeyManager createDummyKeyManager() {
        return DummyX509ExtendedKeyManager.getInstance();
    }

    /**
     * Wraps the given KeyManager into an instance of a Hot Swappable KeyManager
     * This type of KeyManager has the capability of swapping in and out different KeyManagers at runtime.
     *
     * @param keyManager    To be wrapped KeyManager
     * @return              Swappable KeyManager
     */
    public static X509ExtendedKeyManager createSwappableKeyManager(X509KeyManager keyManager) {
        return new HotSwappableX509ExtendedKeyManager(KeyManagerUtils.wrapIfNeeded(keyManager));
    }

    /**
     * Swaps the internal KeyManager instance with the given keyManager object.
     * The baseKeyManager should be an instance of {@link HotSwappableX509ExtendedKeyManager}
     * and can be created with {@link KeyManagerUtils#createSwappableKeyManager(X509KeyManager)}
     *
     * @param baseKeyManager                an instance of {@link HotSwappableX509ExtendedKeyManager}
     * @param newKeyManager                 to be injected instance of a KeyManager
     * @throws GenericKeyManagerException   if {@code baseKeyManager} is not instance of {@link HotSwappableX509ExtendedKeyManager}
     */
    public static void swapKeyManager(X509KeyManager baseKeyManager, X509KeyManager newKeyManager) {
        if (newKeyManager instanceof HotSwappableX509ExtendedKeyManager) {
            throw new GenericKeyManagerException(
                    String.format("The newKeyManager should not be an instance of [%s]", HotSwappableX509ExtendedKeyManager.class.getName())
            );
        }

        if (baseKeyManager instanceof HotSwappableX509ExtendedKeyManager) {
            ((HotSwappableX509ExtendedKeyManager) baseKeyManager).setKeyManager(KeyManagerUtils.wrapIfNeeded(newKeyManager));
        } else {
            throw new GenericKeyManagerException(
                    String.format("The baseKeyManager is from the instance of [%s] and should be an instance of [%s].",
                            baseKeyManager.getClass().getName(),
                            HotSwappableX509ExtendedKeyManager.class.getName())
            );
        }
    }

    public static void addIdentityRoute(X509ExtendedKeyManager keyManager, String alias, String... hosts) {
        addIdentityRoute(keyManager, alias, hosts, false);
    }

    public static void overrideIdentityRoute(X509ExtendedKeyManager keyManager, String alias, String... hosts) {
        addIdentityRoute(keyManager, alias, hosts, true);
    }

    private static void addIdentityRoute(X509ExtendedKeyManager keyManager,
                                         String alias,
                                         String[] hosts,
                                         boolean overrideExistingRouteEnabled) {

        ValidationUtils.requireNotNull(keyManager, ValidationUtils.GENERIC_EXCEPTION_MESSAGE.apply("KeyManager"));
        ValidationUtils.requireNotNull(alias, ValidationUtils.GENERIC_EXCEPTION_MESSAGE.apply("Alias"));
        ValidationUtils.requireNotNull(keyManager, ValidationUtils.GENERIC_EXCEPTION_MESSAGE.apply("Host"));

        if (keyManager instanceof CompositeX509ExtendedKeyManager) {
            CompositeX509ExtendedKeyManager compositeX509ExtendedKeyManager = (CompositeX509ExtendedKeyManager) keyManager;
            Map<String, List<URI>> aliasToHosts = compositeX509ExtendedKeyManager.getIdentityRoute();

            List<URI> uris = new ArrayList<>();
            for (String host : hosts) {
                URI uri = URI.create(host);
                UriUtils.validate(uri);
                uris.add(uri);
            }

            if (overrideExistingRouteEnabled && aliasToHosts.containsKey(alias)) {
                aliasToHosts.get(alias).clear();
            }

            for (URI uri : uris) {
                if (aliasToHosts.containsKey(alias)) {
                    aliasToHosts.get(alias).add(uri);
                } else {
                    aliasToHosts.put(alias, new ArrayList<>(Collections.singleton(uri)));
                }
            }
        } else {
            throw new GenericKeyManagerException(String.format(
                    "KeyManager should be an instance of: [%s], but received: [%s]",
                    CompositeX509ExtendedKeyManager.class.getName(),
                    keyManager.getClass().getName()));
        }
    }

    public static Map<String, List<String>> getIdentityRoute(X509ExtendedKeyManager keyManager) {
        ValidationUtils.requireNotNull(keyManager, ValidationUtils.GENERIC_EXCEPTION_MESSAGE.apply("KeyManager"));

        if (keyManager instanceof CompositeX509ExtendedKeyManager) {
            return ((CompositeX509ExtendedKeyManager) keyManager)
                    .getIdentityRoute()
                    .entrySet().stream()
                    .collect(Collectors.collectingAndThen(
                            Collectors.toMap(
                                    Entry::getKey,
                                    hosts -> hosts.getValue().stream()
                                            .map(URI::toString)
                                            .collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList))),
                            Collections::unmodifiableMap)
                    );
        } else {
            throw new GenericKeyManagerException(String.format(
                    "KeyManager should be an instance of: [%s], but received: [%s]",
                    CompositeX509ExtendedKeyManager.class.getName(),
                    keyManager.getClass().getName()));
        }
    }

    private static List<X509ExtendedKeyManager> unwrapIfPossible(X509ExtendedKeyManager keyManager) {
        if (keyManager instanceof CompositeX509ExtendedKeyManager) {
            List<X509ExtendedKeyManager> keyManagers = new ArrayList<>();
            for (X509ExtendedKeyManager innerKeyManager : ((CompositeX509ExtendedKeyManager) keyManager).getKeyManagers()) {
                List<X509ExtendedKeyManager> unwrappedKeyManagers = KeyManagerUtils.unwrapIfPossible(innerKeyManager);
                keyManagers.addAll(unwrappedKeyManagers);
            }
            return keyManagers;
        } else {
            return Collections.singletonList(keyManager);
        }
    }

    public static KeyManagerBuilder keyManagerBuilder() {
        return new KeyManagerBuilder();
    }

    public static final class KeyManagerBuilder {

        private static final String EMPTY_KEY_MANAGER_EXCEPTION = "Input does not contain KeyManagers";

        private final List<X509ExtendedKeyManager> keyManagers = new ArrayList<>();
        private final Map<String, List<URI>> aliasToHost = new HashMap<>();
        private boolean swappableKeyManagerEnabled = false;

        private KeyManagerBuilder() {}

        public <T extends X509KeyManager> KeyManagerBuilder withKeyManagers(List<T> keyManagers) {
            for (X509KeyManager keyManager : keyManagers) {
                withKeyManager(keyManager);
            }
            return this;
        }

        public <T extends X509KeyManager> KeyManagerBuilder withKeyManager(T keyManager) {
            this.keyManagers.add(KeyManagerUtils.wrapIfNeeded(keyManager));
            return this;
        }

        public KeyManagerBuilder withIdentities(List<? extends KeyStoreHolder> identities) {
            for (KeyStoreHolder identity : identities) {
                this.keyManagers.add(KeyManagerUtils.createKeyManager(identity.getKeyStore(), identity.getKeyPassword()));
            }
            return this;
        }

        public <T extends KeyStore> KeyManagerBuilder withIdentity(T identity, char[] identityPassword, String keyManagerAlgorithm) {
            this.keyManagers.add(KeyManagerUtils.createKeyManager(identity, identityPassword, keyManagerAlgorithm));
            return this;
        }

        public KeyManagerBuilder withSwappableKeyManager(boolean swappableKeyManagerEnabled) {
            this.swappableKeyManagerEnabled = swappableKeyManagerEnabled;
            return this;
        }

        public KeyManagerBuilder withIdentityRoute(Map<String, List<URI>> aliasToHost) {
            this.aliasToHost.putAll(aliasToHost);
            return this;
        }

        public X509ExtendedKeyManager build() {
            ValidationUtils.requireNotEmpty(keyManagers, () -> new GenericKeyManagerException(EMPTY_KEY_MANAGER_EXCEPTION));

            X509ExtendedKeyManager keyManager;
            if (keyManagers.size() == 1) {
                keyManager = keyManagers.get(0);
            } else {
                keyManager = keyManagers.stream()
                        .map(KeyManagerUtils::unwrapIfPossible)
                        .flatMap(Collection::stream)
                        .collect(Collectors.collectingAndThen(
                                Collectors.toList(),
                                extendedKeyManagers -> new CompositeX509ExtendedKeyManager(extendedKeyManagers, aliasToHost)
                        ));
            }

            if (swappableKeyManagerEnabled) {
                keyManager = KeyManagerUtils.createSwappableKeyManager(keyManager);
            }

            return keyManager;
        }

    }

}
