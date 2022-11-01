package org.xbib.net;

import java.io.IOException;
import java.net.IDN;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xbib.net.scheme.Scheme;
import org.xbib.net.scheme.SchemeRegistry;
import org.xbib.net.util.CharUtils;
import org.xbib.net.util.InvalidCharacterException;
import org.xbib.net.util.Profile;

public class IRI implements Comparable<IRI> {

    private static final Pattern IRIPATTERN =
            Pattern.compile("^(?:([^:/?#]+):)?(?://([^/?#]*))?([^?#]*)(?:\\?([^#]*))?(?:#(.*))?");

    private final IRIBuilder builder;

    IRI(IRIBuilder builder) {
        this.builder = builder;
    }

    public static IRIBuilder builder() {
        return new IRIBuilder();
    }

    public static IRI create(String iri) {
        return IRI.builder().from(iri).build();
    }

    public String getScheme() {
        return builder.scheme;
    }

    public String getAuthority() {
        return (builder.authority != null && builder.authority.length() > 0) ? builder.authority : null;
    }

    public String getFragment() {
        return builder.fragment;
    }

    public String getHost() {
        return (builder.host != null && builder.host.length() > 0) ? builder.host : null;
    }

    public String getPath() {
        return builder.path;
    }

    public int getPort() {
        return builder.port;
    }

    public String getQuery() {
        return builder.query;
    }

    public String getSchemeSpecificPart() {
        return builder.schemeSpecificPart;
    }

    public String getUserInfo() {
        return builder.userinfo;
    }

    public boolean isAbsolute() {
        return builder.scheme != null;
    }

    public boolean isOpaque() {
        return builder.path == null;
    }

    public boolean isPathAbsolute() {
        String s = getPath();
        return s != null && s.length() > 0 && s.charAt(0) == '/';
    }

    public boolean isSameDocumentReference() {
        return builder.scheme == null && builder.authority == null
                && (builder.path == null || builder.path.length() == 0 || ".".equals(builder.path))
                && builder.query == null;
    }


    public String getASCIIHost() {
        return builder.getASCIIHost();
    }

    public String getASCIIAuthority() {
        return builder.getASCIIAuthority();
    }

    public String getASCIIFragment() {
        return builder.getASCIIFragment();
    }

    public String getASCIIPath() {
        return builder.getASCIIPath();
    }

    public String getASCIIQuery() {
        return builder.getASCIIQuery();
    }

    public String getASCIIUserInfo() {
        return builder.getASCIIUserInfo();
    }

    public String getASCIISchemeSpecificPart() {
        return builder.getASCIISchemeSpecificPart();
    }

    public IRI resolve(IRI iri) {
        return resolve(this, iri);
    }

    public IRI resolve(String iri) {
        return resolve(this, IRI.builder().from(iri).build());
    }

    public static IRI resolve(IRI b, IRI c) {
        if (c == null) {
            return null;
        }
        if ("".equals(c.toString()) || "#".equals(c.toString())
                || ".".equals(c.toString())
                || "./".equals(c.toString())) {
            return b;
        }
        if (b == null) {
            return c;
        }
        if (c.isOpaque() || b.isOpaque()) {
            return c;
        }
        if (c.isSameDocumentReference()) {
            String cfragment = c.getFragment();
            String bfragment = b.getFragment();
            if ((cfragment == null && bfragment == null) || (cfragment != null && cfragment.equals(bfragment))) {
                return b;
            } else {
                return IRI.builder()
                        .scheme(b.builder.scheme)
                        .authority(b.builder.authority)
                        .userinfo(b.builder.userinfo)
                        .host(b.builder.host)
                        .port(b.builder.port)
                        .path(normalizePath(b.builder.path))
                        .query(b.builder.query)
                        .fragment(cfragment)
                        .build();
            }
        }
        if (c.isAbsolute()) {
            return c;
        }
        String scheme = b.builder.scheme;
        String query = c.getQuery();
        String fragment = c.getFragment();
        String userinfo;
        String authority;
        String host;
        int port;
        String path;
        if (c.getAuthority() == null) {
            authority = b.getAuthority();
            userinfo = b.getUserInfo();
            host = b.getHost();
            port = b.getPort();
            path = c.isPathAbsolute() ? normalizePath(c.getPath()) : resolve(b.getPath(), c.getPath());
        } else {
            authority = c.getAuthority();
            userinfo = c.getUserInfo();
            host = c.getHost();
            port = c.getPort();
            path = normalizePath(c.getPath());
        }
        return IRI.builder()
                .scheme(scheme)
                .authority(authority)
                .userinfo(userinfo)
                .host(host)
                .port(port)
                .path(path)
                .query(query)
                .fragment(fragment)
                .build();
    }

    public static IRI relativize(IRI b, IRI c) {
        if (c.isOpaque() || b.isOpaque()) {
            return c;
        }
        if ((b.builder.scheme == null && c.builder.scheme != null) || (b.builder.scheme != null && c.builder.scheme == null)
                || (b.builder.scheme != null && !b.builder.scheme.equalsIgnoreCase(c.builder.scheme))) {
            return c;
        }
        String bpath = normalizePath(b.getPath());
        String cpath = normalizePath(c.getPath());
        if (!bpath.equals(cpath)) {
            if (bpath.charAt(bpath.length() - 1) != '/') {
                bpath += "/";
            }
            if (!cpath.startsWith(bpath)) {
                return c;
            }
        }
        return IRI.builder()
                .scheme(null)
                .authority(null)
                .userinfo(null)
                .host(null)
                .port(-1)
                .path(normalizePath(cpath.substring(bpath.length())))
                .query(c.getQuery())
                .fragment(c.getFragment())
                .build();
    }

    private static String normalizePath(String path) {
        if (path == null || path.length() == 0) {
            return "/";
        }
        String[] segments = path.split("/");
        if (segments.length < 2) {
            return path;
        }
        StringBuilder buf = new StringBuilder("/");
        for (int n = 0; n < segments.length; n++) {
            String segment = segments[n].intern();
            if (".".equals(segment)) {
                segments[n] = null;
            }
        }
        PercentDecoder percentDecoder = new PercentDecoder();
        for (String segment : segments) {
            if (segment != null) {
                if (buf.length() > 1) {
                    buf.append('/');
                }
                try {
                    buf.append(PercentEncoders.getMatrixEncoder(StandardCharsets.UTF_8).encode(percentDecoder.decode(segment)));
                } catch (IOException e) {
                    //logger.log(Level.FINE, e.getMessage(), e);
                }
            }
        }
        if (path.endsWith("/") || path.endsWith("/.")) {
            buf.append('/');
        }
        return buf.toString();
    }

    private static String resolve(String bpath, String cpath) {
        if (bpath == null && cpath == null) {
            return null;
        }
        if (bpath == null) {
            return (!cpath.startsWith("/")) ? "/" + cpath : cpath;
        }
        if (cpath == null) {
            return bpath;
        }
        StringBuilder buf = new StringBuilder("");
        int n = bpath.lastIndexOf('/');
        if (n > -1) {
            buf.append(bpath, 0, n + 1);
        }
        if (cpath.length() != 0) {
            buf.append(cpath);
        }
        if (buf.charAt(0) != '/') {
            buf.insert(0, '/');
        }
        return normalizePath(buf.toString());
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        String s = getScheme();
        if (s != null && !s.isEmpty()) {
            buf.append(s).append(':');
        }
        buf.append(getSchemeSpecificPart());
        return buf.toString();
    }

    public String toEncodedString() throws IOException {
        return PercentEncoders.getUnreservedEncoder(StandardCharsets.UTF_8).encode(toString());
    }

    public String toASCIIString() {
        StringBuilder buf = new StringBuilder();
        String s = getScheme();
        if (s != null && !s.isEmpty()) {
            buf.append(s).append(':');
        }
        buf.append(getASCIISchemeSpecificPart());
        return buf.toString();
    }

    public String toBIDIString() {
        return CharUtils.wrapBidi(toString(), CharUtils.LRE);
    }

    public URI toURI() throws URISyntaxException {
        return new URI(toASCIIString());
    }

    public java.net.URL toURL() throws MalformedURLException, URISyntaxException {
        return toURI().toURL();
    }

    @Override
    public int hashCode() {
        final int p = 31;
        int result = 1;
        result = p * result + ((builder.authority == null) ? 0 : builder.authority.hashCode());
        result = p * result + ((builder.fragment == null) ? 0 : builder.fragment.hashCode());
        result = p * result + ((builder.host == null) ? 0 : builder.host.hashCode());
        result = p * result + ((builder.path == null) ? 0 : builder.path.hashCode());
        result = p * result + builder.port;
        result = p * result + ((builder.query == null) ? 0 : builder.query.hashCode());
        result = p * result + ((builder.scheme == null) ? 0 : builder.scheme.hashCode());
        result = p * result + ((builder.userinfo == null) ? 0 : builder.userinfo.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final IRI other = (IRI) obj;
        if (builder.authority == null) {
            if (other.builder.authority != null) {
                return false;
            }
        } else if (!builder.authority.equals(other.builder.authority)) {
            return false;
        }
        if (builder.fragment == null) {
            if (other.builder.fragment != null) {
                return false;
            }
        } else if (!builder.fragment.equals(other.builder.fragment)) {
            return false;
        }
        if (builder.host == null) {
            if (other.builder.host != null) {
                return false;
            }
        } else if (!builder.host.equals(other.builder.host)) {
            return false;
        }
        if (builder.path == null) {
            if (other.builder.path != null) {
                return false;
            }
        } else if (!builder.path.equals(other.builder.path)) {
            return false;
        }
        if (builder.port != other.builder.port) {
            return false;
        }
        if (builder.query == null) {
            if (other.builder.query != null) {
                return false;
            }
        } else if (!builder.query.equals(other.builder.query)) {
            return false;
        }
        if (builder.scheme == null) {
            if (other.builder.scheme != null) {
                return false;
            }
        } else if (!builder.scheme.equals(other.builder.scheme)) {
            return false;
        }
        if (builder.userinfo == null) {
            return other.builder.userinfo == null;
        } else {
            return builder.userinfo.equals(other.builder.userinfo);
        }
    }

    @Override
    public int compareTo(IRI that) {
        int c;
        if ((c = compareIgnoringCase(builder.scheme, that.builder.scheme)) != 0) {
            return c;
        }
        if (isOpaque()) {
            if (that.isOpaque()) {
                // Both opaque
                if ((c = compare(builder.schemeSpecificPart, that.builder.schemeSpecificPart)) != 0) {
                    return c;
                }
                return compare(builder.fragment, that.builder.fragment);
            }
            return +1;
        } else if (that.isOpaque()) {
            return -1;
        }
        // Hierarchical
        if ((builder.host != null) && (that.builder.host != null)) {
            // Both server-based
            if ((c = compare(builder.userinfo, that.builder.userinfo)) != 0) {
                return c;
            }
            if ((c = compareIgnoringCase(builder.host, that.builder.host)) != 0) {
                return c;
            }
            if ((c = builder.port - that.builder.port) != 0) {
                return c;
            }
        } else {
            if ((c = compare(builder.authority, that.builder.authority)) != 0) {
                return c;
            }
        }
        if ((c = compare(builder.path, that.builder.path)) != 0) {
            return c;
        }
        if ((c = compare(builder.query, that.builder.query)) != 0) {
            return c;
        }
        return compare(builder.fragment, that.builder.fragment);
    }

    private int compare(String s, String t) {
        if (s != null) {
            if (s.equals(t)) {
                return 0;
            }
            if (t != null) {
                return s.compareTo(t);
            } else {
                return +1;
            }
        } else {
            return -1;
        }
    }

    private int compareIgnoringCase(String s, String t) {
        if (s != null) {
            if (s.equals(t)) {
                return 0;
            }
            if (t != null) {
                int sn = s.length();
                int tn = t.length();
                int n = Math.min(sn, tn);
                for (int i = 0; i < n; i++) {
                    int c = toLower(s.charAt(i)) - toLower(t.charAt(i));
                    if (c != 0) {
                        return c;
                    }
                }
                return sn - tn;
            }
            return +1;
        } else {
            return -1;
        }
    }

    private int toLower(char c) {
        if ((c >= 'A') && (c <= 'Z')) {
            return c + ('a' - 'A');
        }
        return c;
    }

    /**
     *
     */
    public static class IRIBuilder {

        final SchemeRegistry reg = SchemeRegistry.getInstance();

        Scheme schemeClass;

        String scheme;

        String schemeSpecificPart;

        String authority;

        String userinfo;

        String host;

        int port = -1;

        String path;

        String query;

        String fragment;

        private String asciiHost;

        private String asciiAuthority;

        private String asciiUserinfo;

        private String asciiSchemeSpecificPart;

        private String asciiPath;

        private String asciiQuery;

        private String asciiFragment;

        private IRIBuilder() {
        }

        public IRIBuilder from(String string) {
            parse(CharUtils.stripBidi(string));
            authorityAndSchemeSpecificPart();
            return this;
        }

        public IRIBuilder from(URI uri) {
            scheme = uri.getScheme();
            schemeClass = reg.getScheme(scheme);
            authority = uri.getAuthority();
            path = uri.getPath();
            query = uri.getQuery();
            fragment = uri.getFragment();
            parseAuthority();
            authorityAndSchemeSpecificPart();
            return this;
        }

        public IRIBuilder from(IRI uri) {
            scheme = uri.getScheme();
            schemeClass = reg.getScheme(scheme);
            authority = uri.getAuthority();
            path = uri.getPath();
            query = uri.getQuery();
            fragment = uri.getFragment();
            parseAuthority();
            authorityAndSchemeSpecificPart();
            return this;
        }

        public IRIBuilder from(String scheme, String schemeSpecificPart, String fragment) {
            this.scheme = scheme.toLowerCase();
            this.schemeSpecificPart = schemeSpecificPart;
            this.fragment = fragment;
            authorityAndSchemeSpecificPart();
            return this;
        }

        public IRIBuilder scheme(String scheme) {
            this.scheme = scheme;
            this.schemeClass = reg.getScheme(scheme);
            return this;
        }

        public IRIBuilder schemeSpecificPart(String schemeSpecificPart) {
            this.schemeSpecificPart = schemeSpecificPart;
            return this;
        }

        public IRIBuilder curie(String prefix, String path) {
            this.scheme = prefix;
            this.path = path;
            return this;
        }

        public IRIBuilder curie(String schemeAndPath) {
            int pos = schemeAndPath.indexOf(':');
            this.scheme = pos > 0 ? schemeAndPath.substring(0, pos) : null;
            this.path = pos > 0 ? schemeAndPath.substring(pos + 1) : schemeAndPath;
            return this;
        }

        public IRIBuilder authority(String authority) {
            this.authority = authority;
            return this;
        }

        public IRIBuilder userinfo(String userinfo) {
            this.userinfo = userinfo;
            return this;
        }

        public IRIBuilder host(String host) {
            this.host = host;
            return this;
        }

        public IRIBuilder port(int port) {
            this.port = port;
            return this;
        }

        public IRIBuilder path(String path) {
            this.path = path;
            return this;
        }

        public IRIBuilder query(String query) {
            this.query = query;
            return this;
        }

        public IRIBuilder fragment(String fragment) {
            this.fragment = fragment;
            return this;
        }

        public IRI build() {
            return new IRI(this);
        }

        private void parse(String iri) {
            try {
                Matcher irim = IRIPATTERN.matcher(iri);
                if (irim.find()) {
                    scheme = irim.group(1);
                    schemeClass = reg.getScheme(scheme);
                    authority = irim.group(2);
                    path = irim.group(3);
                    query = irim.group(4);
                    fragment = irim.group(5);
                    parseAuthority();
                    try {
                        CharUtils.verify(scheme, Profile.SCHEME);
                        CharUtils.verify(path, Profile.IPATH);
                        CharUtils.verify(query, Profile.IQUERY);
                        CharUtils.verify(fragment, Profile.IFRAGMENT);
                    } catch (InvalidCharacterException e) {
                        throw new IRISyntaxException(e);
                    }
                } else {
                    throw new IRISyntaxException("invalid Syntax");
                }
            } catch (IRISyntaxException e) {
                throw e;
            } catch (Exception e) {
                throw new IRISyntaxException(e);
            }
        }

        private void parseAuthority() {
            if (authority != null) {
                // [ <userinfo> '@' ] <host> [ ':' <port> ]
                int pos = authority.lastIndexOf('@');
                userinfo = pos >= 0 ? authority.substring(0, pos) : null;
                String s = pos >= 0 ? authority.substring(pos + 1) : authority;
                pos = s.indexOf(':');
                host = pos >= 0 ? s.substring(0, pos) : s;
                port = pos >= 0 ? Integer.parseInt(s.substring(pos + 1)) : -1;
                try {
                    CharUtils.verify(userinfo, Profile.IUSERINFO);
                    CharUtils.verify(host, Profile.IHOST);
                } catch (InvalidCharacterException e) {
                    throw new IRISyntaxException(e);
                }
            }
        }

        private void authorityAndSchemeSpecificPart() {
            if (authority == null && (userinfo != null || host != null)) {
                StringBuilder buf = new StringBuilder();
                buildAuthority(buf, userinfo, host, port);
                authority = (buf.length() != 0) ? buf.toString() : null;
            }
            StringBuilder buf = new StringBuilder();
            buildSchemeSpecificPart(buf, authority, path, query, fragment);
            schemeSpecificPart = buf.toString();
        }

        private static void buildSchemeSpecificPart(StringBuilder buf, String authority, String path, String query,
                                                    String fragment) {
            if (authority != null) {
                buf.append("//");
                buf.append(authority);
            }
            if (path != null && path.length() > 0) {
                buf.append(path);
            }
            if (query != null) {
                buf.append('?');
                buf.append(query);
            }
            if (fragment != null) {
                buf.append('#');
                buf.append(fragment);
            }
        }

        public String getASCIIHost() {
            if (host != null && asciiHost == null) {
                if (host.startsWith("[")) {
                    asciiHost = host;
                } else {
                    asciiHost = IDN.toASCII(host);
                }
            }
            return (asciiHost != null && asciiHost.length() > 0) ? asciiHost : null;
        }

        private String getASCIIAuthority() {
            if (authority != null && asciiAuthority == null) {
                asciiAuthority = buildASCIIAuthority();
            }
            return asciiAuthority != null && asciiAuthority.length() > 0 ? asciiAuthority : null;
        }

        private String buildASCIIAuthority() {
            StringBuilder buf = new StringBuilder();
            buildAuthority(buf, getASCIIUserInfo(), getASCIIHost(), port);
            return buf.toString();
        }

        private static void buildAuthority(StringBuilder buf, String aui, String ah, int port) {
            if (aui != null && aui.length() != 0) {
                buf.append(aui);
                buf.append('@');
            }
            if (ah != null && ah.length() != 0) {
                buf.append(ah);
            }
            if (port != -1) {
                buf.append(':');
                buf.append(port);
            }
        }

        private String getASCIIFragment() {
            if (fragment != null && asciiFragment == null) {
                try {
                    asciiFragment = PercentEncoders.getFragmentEncoder(StandardCharsets.UTF_8).encode(fragment);
                } catch (IOException e) {
                    //logger.log(Level.FINE, e.getMessage(), e);
                }
            }
            return asciiFragment;
        }

        private String getASCIIPath() {
            if (path != null && asciiPath == null) {
                try {
                    asciiPath = PercentEncoders.getPathEncoder(StandardCharsets.UTF_8).encode(path);
                } catch (IOException e) {
                    //logger.log(Level.FINE, e.getMessage(), e);
                }
            }
            return asciiPath;
        }

        public String getASCIIQuery() {
            if (query != null && asciiQuery == null) {
                try {
                    asciiQuery = PercentEncoders.getQueryEncoder(StandardCharsets.UTF_8).encode(query);
                } catch (IOException e) {
                    //logger.log(Level.FINE, e.getMessage(), e);
                }
            }
            return asciiQuery;
        }

        public String getASCIIUserInfo() {
            if (userinfo != null && asciiUserinfo == null) {
                try {
                    asciiUserinfo = PercentEncoders.getUnreservedEncoder(StandardCharsets.UTF_8).encode(userinfo);
                } catch (IOException e) {
                    //logger.log(Level.FINE, e.getMessage(), e);
                }
            }
            return asciiUserinfo;
        }

        public String getASCIISchemeSpecificPart() {
            if (asciiSchemeSpecificPart == null) {
                StringBuilder buf = new StringBuilder();
                buildSchemeSpecificPart(buf, getASCIIAuthority(), getASCIIPath(), getASCIIQuery(), getASCIIFragment());
                asciiSchemeSpecificPart = buf.toString();
            }
            return asciiSchemeSpecificPart;
        }
    }
}
