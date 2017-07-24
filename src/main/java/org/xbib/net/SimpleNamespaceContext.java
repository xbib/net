package org.xbib.net;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Contains a simple context for namespaces.
 */
public class SimpleNamespaceContext {

    private static final Logger logger = Logger.getLogger(SimpleNamespaceContext.class.getName());

    private static final String DEFAULT_RESOURCE =
            SimpleNamespaceContext.class.getPackage().getName().replace('.', '/') + '/' + "namespace";

    private static final SimpleNamespaceContext DEFAULT_CONTEXT = newDefaultInstance();

    // sort namespace by length in descending order, useful for compacting prefix
    protected final SortedMap<String, String> namespaces = new TreeMap<>();

    private final SortedMap<String, Set<String>> prefixes = new TreeMap<>();

    protected SimpleNamespaceContext() {
    }

    protected SimpleNamespaceContext(ResourceBundle bundle) {
        Enumeration<String> en = bundle.getKeys();
        while (en.hasMoreElements()) {
            String prefix = en.nextElement();
            String namespace = bundle.getString(prefix);
            addNamespace(prefix, namespace);
        }
    }

    public static SimpleNamespaceContext getInstance() {
        return DEFAULT_CONTEXT;
    }

    /**
     * Empty namespace context.
     *
     * @return an XML namespace context
     */
    public static SimpleNamespaceContext newInstance() {
        return new SimpleNamespaceContext();
    }

    public static SimpleNamespaceContext newDefaultInstance() {
        return newInstance(DEFAULT_RESOURCE);
    }

    /**
     * Use thread context class laoder to instantiate a namespace context.
     * @param bundleName the resource bundle name
     * @return XML namespace context
     */
    public static SimpleNamespaceContext newInstance(String bundleName) {
        return newInstance(bundleName, Locale.getDefault(), Thread.currentThread().getContextClassLoader());
    }

    public static SimpleNamespaceContext newInstance(String bundleName, Locale locale, ClassLoader classLoader) {
        try {
            return new SimpleNamespaceContext(ResourceBundle.getBundle(bundleName, locale, classLoader));
        } catch (MissingResourceException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
            return new SimpleNamespaceContext();
        }
    }

    public void addNamespace(String prefix, String namespace) {
        namespaces.put(prefix, namespace);
        if (prefixes.containsKey(namespace)) {
            prefixes.get(namespace).add(prefix);
        } else {
            Set<String> set = new HashSet<>();
            set.add(prefix);
            prefixes.put(namespace, set);
        }
    }

    public SortedMap<String, String> getNamespaces() {
        return namespaces;
    }

    public String getNamespaceURI(String prefix) {
        if (prefix == null) {
            return null;
        }
        return namespaces.getOrDefault(prefix, null);
    }

    public String getPrefix(String namespaceURI) {
        Iterator<String> it = getPrefixes(namespaceURI);
        return it != null && it.hasNext() ? it.next() : null;
    }

    public Iterator<String> getPrefixes(String namespace) {
        if (namespace == null) {
            throw new IllegalArgumentException("namespace URI cannot be null");
        }
        return prefixes.containsKey(namespace) ?
                prefixes.get(namespace).iterator() : null;
    }

    @Override
    public String toString() {
        return namespaces.toString();
    }
}
