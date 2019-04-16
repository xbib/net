package org.xbib.net;

import org.xbib.net.scheme.Scheme;
import org.xbib.net.scheme.SchemeRegistry;

import java.net.IDN;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnmappableCharacterException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * A Uniform Resource Locator (URL) is a compact representation of the
 * location and access method for a resource available via the Internet.
 *
 * Historically, there are many different forms of internet resource representations, for example,
 * the URL (RFC 1738 as of 1994), the URI (RFC 2396 as of 1998), and IRI (RFC 3987 as of 2005),
 * and most of them have updated specifications.
 *
 * {@link URL} is a Java implementation that serves as a universal point of handling all
 * different forms. It follows the syntax of the Uniform Resource Identifier ({@code RFC 3986})
 * in accordance with the link:https://url.spec.whatwg.org/[{@code WHATWG} URL standard].
 *
 * The reason for the name {@code URL} is merely because of the popularity of the name, which
 * overweighs the URI or IRI popularity.
 *
 * [source,java]
 * --
 * URL url = URL.http().resolveFromHost("google.com").build();
 * --
 *
 */
public class URL implements Comparable<URL> {

    private static final Logger logger = Logger.getLogger(URL.class.getName());

    private static final char SEPARATOR_CHAR = '/';

    private static final char QUESTION_CHAR = '?';

    private static final char COLON_CHAR = ':';

    private static final char SEMICOLON_CHAR = ';';

    private static final char EQUAL_CHAR = '=';

    private static final char AMPERSAND_CHAR = '&';

    private static final char NUMBER_SIGN_CHAR = '#';

    private static final char AT_CHAR = '@';

    private static final char LEFT_BRACKET_CHAR = '[';

    private static final char RIGHT_BRACKET_CHAR = ']';

    private static final String DOUBLE_SLASH = "//";

    private static final String EMPTY = "";

    private static final PathSegment EMPTY_SEGMENT = new PathSegment(EMPTY);

    private final transient Builder builder;

    private final transient Scheme scheme;

    private final transient PercentEncoder queryParamEncoder;

    private final transient PercentEncoder regNameEncoder;

    private final transient PercentEncoder pathEncoder;

    private final transient PercentEncoder matrixEncoder;

    private final transient PercentEncoder queryEncoder;

    private final transient PercentEncoder fragmentEncoder;

    private final String hostinfo;

    private final String path;

    private final String query;

    private final String fragment;

    private String internalStringRepresentation;

    private String externalStringRepresentation;

    private URL(Builder builder) {
        this.builder = builder;
        this.scheme = SchemeRegistry.getInstance().getScheme(builder.scheme);
        this.queryParamEncoder = PercentEncoders.getQueryParamEncoder(builder.charset);
        this.regNameEncoder = PercentEncoders.getRegNameEncoder(builder.charset);
        this.pathEncoder = PercentEncoders.getPathEncoder(builder.charset);
        this.matrixEncoder = PercentEncoders.getMatrixEncoder(builder.charset);
        this.queryEncoder = PercentEncoders.getQueryEncoder(builder.charset);
        this.fragmentEncoder = PercentEncoders.getFragmentEncoder(builder.charset);
        this.hostinfo = encodeHostInfo();
        this.path = encodePath();
        this.query = encodeQuery();
        this.fragment = encodeFragment();
    }

    public static Builder file() {
        return new Builder().scheme(Scheme.FILE);
    }

    public static Builder ftp() {
        return new Builder().scheme(Scheme.FTP);
    }

    public static Builder git() {
        return new Builder().scheme(Scheme.GIT);
    }

    public static Builder gopher() {
        return new Builder().scheme(Scheme.GOPHER);
    }

    public static Builder http() {
        return new Builder().scheme(Scheme.HTTP);
    }

    public static Builder https() {
        return new Builder().scheme(Scheme.HTTPS);
    }

    public static Builder imap() {
        return new Builder().scheme(Scheme.IMAP);
    }

    public static Builder imaps() {
        return new Builder().scheme(Scheme.IMAPS);
    }

    public static Builder irc() {
        return new Builder().scheme(Scheme.IRC);
    }

    public static Builder ldap() {
        return new Builder().scheme(Scheme.LDAP);
    }

    public static Builder ldaps() {
        return new Builder().scheme(Scheme.LDAPS);
    }

    public static Builder mailto() {
        return new Builder().scheme(Scheme.MAILTO);
    }

    public static Builder news() {
        return new Builder().scheme(Scheme.NEWS);
    }

    public static Builder nntp() {
        return new Builder().scheme(Scheme.NNTP);
    }

    public static Builder pop3() {
        return new Builder().scheme(Scheme.POP3);
    }

    public static Builder pop3s() {
        return new Builder().scheme(Scheme.POP3S);
    }

    public static Builder rtmp() {
        return new Builder().scheme(Scheme.RTMP);
    }

    public static Builder rtsp() {
        return new Builder().scheme(Scheme.RTSP);
    }

    public static Builder redis() {
        return new Builder().scheme(Scheme.REDIS);
    }

    public static Builder rsync() {
        return new Builder().scheme(Scheme.RSYNC);
    }

    public static Builder sftp() {
        return new Builder().scheme(Scheme.SFTP);
    }

    public static Builder smtp() {
        return new Builder().scheme(Scheme.SMTP);
    }

    public static Builder smtps() {
        return new Builder().scheme(Scheme.SMTPS);
    }

    public static Builder snews() {
        return new Builder().scheme(Scheme.SNEWS);
    }

    public static Builder ssh() {
        return new Builder().scheme(Scheme.SSH);
    }

    public static Builder telnet() {
        return new Builder().scheme(Scheme.TELNET);
    }

    public static Builder tftp() {
        return new Builder().scheme(Scheme.TFTP);
    }

    public static Builder ws() {
        return new Builder().scheme(Scheme.WS);
    }

    public static Builder wss() {
        return new Builder().scheme(Scheme.WSS);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Parser parser() {
        return new Parser();
    }

    public static Resolver base(URL base) {
        return new Resolver(base);
    }

    public static Resolver base(String base) {
        return new Resolver(URL.create(base));
    }

    private static final URL NULL_URL = URL.builder().build();

    /**
     * Return a special URL denoting the fact that this URL should be considered as invalid.
     * The URL has a null scheme.
     * @return url
     */
    public static URL nullUrl() {
        return NULL_URL;
    }

    public static URL from(String input) {
        return from(input, true);
    }

    public static URL create(String input) {
        return from(input, false);
    }

    public static URL from(String input, boolean resolve) {
        try {
            return parser().parse(input, resolve);
        } catch (URLSyntaxException | MalformedInputException | UnmappableCharacterException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public URL resolve(String spec) {
        return from(this, spec);
    }

    public static URL from(URL base, String spec) {
        try {
            return new Resolver(base).resolve(spec);
        } catch (URLSyntaxException | MalformedInputException | UnmappableCharacterException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public URL resolve(URL spec) {
        return from(this, spec);
    }

    public static URL from(URL base, URL spec) {
        try {
            return new Resolver(base).resolve(spec);
        } catch (URLSyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public String relativeReference() {
        StringBuilder sb = new StringBuilder();
        if (path != null) {
            sb.append(path);
        }
        if (query != null) {
            sb.append(QUESTION_CHAR).append(query);
        }
        if (fragment != null) {
            sb.append(NUMBER_SIGN_CHAR).append(fragment);
        }
        if (sb.length() == 0) {
            sb.append(SEPARATOR_CHAR);
        }
        return sb.toString();
    }

    public String decode(String input) {
        try {
            return builder.percentDecoder.decode(input);
        } catch (MalformedInputException | UnmappableCharacterException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public Builder newBuilder() {
        return builder;
    }

    private String toString(boolean withFragment) {
        if (internalStringRepresentation != null) {
            return internalStringRepresentation;
        }
        internalStringRepresentation = toInternalForm(withFragment);
        return internalStringRepresentation;
    }

    /**
     * Gets the scheme of this {@code URL}.
     * @return the scheme ('http' or 'file' or 'ftp' etc...) of the URL if it exists, or null.
     */
    public String getScheme() {
        return builder.scheme;
    }

    /**
     * Get the user info of this {@code URL}.
     * @return  the user info part if it exists.
     */
    public String getUserInfo() {
        return builder.userInfo;
    }

    /**
     * Get the user of the user info.
     * @return the user
     */
    public String getUser() {
        if (builder.userInfo == null) {
            return null;
        }
        Pair<String, String> p = indexOf(COLON_CHAR, builder.userInfo);
        return decode(p.first);
    }

    public String getPassword() {
        if (builder.userInfo == null) {
            return null;
        }
        Pair<String, String> p = indexOf(COLON_CHAR, builder.userInfo);
        return decode(p.second);
    }

    /**
     * Get the host name ('www.example.com' or '192.168.0.1:8080' or '[fde2:d7de:302::]') of the {@code URL}.
     * @return the host name
     */
    public String getHost() {
        return builder.host;
    }

    /**
     * Get the decoded host name.
     * @return the decoded host name
     */
    public String getDecodedHost() {
        return decode(builder.host);
    }

    public String getHostInfo() {
        return hostinfo;
    }

    public ProtocolVersion getProtocolVersion() {
        return builder.protocolVersion;
    }

    public Integer getPort() {
        return builder.port;
    }

    /**
     * Get the path  ('/path/to/my/file.html') of the {@code URL} if it exists.
     * @return the path
     */
    public String getPath() {
        return path;
    }

    /**
     * Get the percent-decoded path of the  {@code URL} if it exists.
     * @return decoded path
     */
    public String getDecodedPath() {
        return decode(path);
    }

    /**
     * Get the query ('?q=foo{@literal &}bar') of the {@code URL} if it exists.
     * @return the query
     */
    public String getQuery() {
        return query;
    }

    public String getDecodedQuery() {
        return decode(query);
    }

    public QueryParameters getQueryParams() {
        return builder.queryParams;
    }

    /**
     * @return the fragment ('#foo{@literal &}bar') of the URL if it exists.
     */
    public String getFragment() {
        return fragment;
    }

    public String getDecodedFragment() {
        return decode(fragment);
    }

    /**
     * @return the opaque part of the URL if it exists.
     */
    public String getSchemeSpecificPart() {
        return builder.schemeSpecificPart;
    }

    /**
     * @return true if URL is opaque.
     */
    public boolean isOpaque() {
        return !isNullOrEmpty(builder.scheme) && !isNullOrEmpty(builder.schemeSpecificPart) && builder.host == null;
    }

    /**
     * @return true if URL is absolute.
     */
    public boolean isAbsolute() {
        return !isNullOrEmpty(builder.scheme);
    }

    public boolean isRelative() {
        return isNullOrEmpty(builder.scheme);
    }

    public Comparator<URL> withFragmentComparator() {
        return new URLWithFragmentComparator();
    }

    public Comparator<URL> withoutFragmentComparator() {
        return new URLWithoutFragmentComparator();
    }

    public URL normalize() {
        return scheme.normalize(this);
    }

    public String toExternalForm() {
        if (externalStringRepresentation != null) {
            return externalStringRepresentation;
        }
        externalStringRepresentation = writeExternalForm();
        return externalStringRepresentation;
    }

    private String toInternalForm(boolean withFragment) {
        StringBuilder sb = new StringBuilder();
        if (!isNullOrEmpty(builder.scheme)) {
            sb.append(builder.scheme).append(COLON_CHAR);
        }
        if (isOpaque()) {
            sb.append(builder.schemeSpecificPart);
        } else {
            appendHostInfo(sb, false, true);
            appendPath(sb, false);
            appendQuery(sb, false, true);
            if (withFragment) {
                appendFragment(sb, false, true);
            }
        }
        return sb.toString();
    }

    private String writeExternalForm() {
        StringBuilder sb = new StringBuilder();
        if (!isNullOrEmpty(builder.scheme)) {
            sb.append(builder.scheme).append(COLON_CHAR);
        }
        if (isOpaque()) {
            sb.append(builder.schemeSpecificPart);
        } else {
            appendHostInfo(sb, true, true);
            appendPath(sb, true);
            appendQuery(sb, true, true);
            appendFragment(sb, true, true);
        }
        return sb.toString();
    }

    private String encodeHostInfo() {
        StringBuilder sb = new StringBuilder();
        appendHostInfo(sb, true, false);
        return sb.toString();
    }

    private void appendHostInfo(StringBuilder sb, boolean encoded, boolean withSlash) {
        if (builder.host == null) {
            return;
        }
        if (withSlash) {
            if (scheme != null) {
                sb.append(DOUBLE_SLASH);
            } else {
                sb.append(SEPARATOR_CHAR);
            }
        }
        if (!builder.host.isEmpty()) {
            if (!isNullOrEmpty(builder.userInfo)) {
                sb.append(builder.userInfo).append(AT_CHAR);
            }
            if (builder.protocolVersion != null) {
                switch (builder.protocolVersion) {
                    case IPV6:
                        String s = "localhost".equals(builder.host) ?
                                InetAddress.getLoopbackAddress().getHostAddress() : builder.host;
                        // prefer host name over numeric address
                        if (s != null && !s.equals(builder.hostAddress)) {
                            sb.append(s);
                        } else if (builder.hostAddress != null) {
                            sb.append(LEFT_BRACKET_CHAR).append(builder.hostAddress).append(RIGHT_BRACKET_CHAR);
                        }
                        break;
                    case IPV4:
                        sb.append(builder.host);
                        break;
                    default:
                        if (encoded) {
                            try {
                                String encodedHostName = regNameEncoder.encode(builder.host);
                                validateHostnameCharacters(encodedHostName);
                                sb.append(encodedHostName);
                            } catch (CharacterCodingException e) {
                                throw new IllegalArgumentException(e);
                            }
                        } else {
                            sb.append(builder.host);
                        }
                        break;
                }
            } else {
                if (encoded) {
                    try {
                        String encodedHostName = regNameEncoder.encode(builder.host);
                        validateHostnameCharacters(encodedHostName);
                        sb.append(encodedHostName);
                    } catch (CharacterCodingException e) {
                        throw new IllegalArgumentException(e);
                    }
                } else {
                    sb.append(builder.host);
                }
            }
            if (scheme != null && builder.port != null && builder.port != scheme.getDefaultPort()) {
                sb.append(COLON_CHAR);
                if (builder.port != -1) {
                    sb.append(builder.port);
                }
            }
        }
    }

    private void validateHostnameCharacters(String hostname) {
        boolean valid;
        for (int i = 0; i < hostname.length(); i++) {
            char c = hostname.charAt(i);
            valid = ('a' <= c && c <= 'z') ||
                    ('A' <= c && c <= 'Z') ||
                    ('0' <= c && c <= '9') ||
                    c == '-' || c == '.' || c == '_' || c == '~' ||
                    c == '!' || c == '$' || c == '&' || c == '\'' || c == '(' || c == ')' ||
                    c == '*' || c == '+' || c == ',' || c == ';' || c == '=' || c == '%';
            if (!valid) {
                throw new IllegalArgumentException("invalid host name character in: " + hostname);
            }
        }
    }

    private String encodePath() {
        StringBuilder sb = new StringBuilder();
        appendPath(sb, true);
        return sb.toString();
    }

    private void appendPath(StringBuilder sb, boolean encoded) {
        Iterator<PathSegment> it = builder.pathSegments.iterator();
        while (it.hasNext()) {
            PathSegment pathSegment = it.next();
            try {
                sb.append(encoded ? pathEncoder.encode(pathSegment.segment) : pathSegment.segment);
                for (Pair<String, String> matrixParam : pathSegment.getMatrixParams()) {
                    sb.append(SEMICOLON_CHAR).append(encoded ?
                            matrixEncoder.encode(matrixParam.getFirst()) : matrixParam.getFirst());
                    if (matrixParam.getSecond() != null) {
                        sb.append(EQUAL_CHAR).append(encoded ?
                                matrixEncoder.encode(matrixParam.getSecond()) : matrixParam.getSecond());
                    }
                }
            } catch (CharacterCodingException e) {
                throw new IllegalArgumentException(e);
            }
            if (it.hasNext()) {
                sb.append(SEPARATOR_CHAR);
            }
        }
    }

    private String encodeQuery() {
        StringBuilder sb = new StringBuilder();
        appendQuery(sb, true, false);
        return sb.length() == 0 ? null : sb.toString();
    }

    private void appendQuery(StringBuilder sb, boolean encoded, boolean withQuestionMark) {
        if (!builder.queryParams.isEmpty()) {
            if (withQuestionMark) {
                sb.append(QUESTION_CHAR);
            }
            Iterator<QueryParameters.Pair<String, String>> it = builder.queryParams.iterator();
            while (it.hasNext()) {
                QueryParameters.Pair<String, String> queryParam = it.next();
                try {
                    sb.append(encoded ? queryParamEncoder.encode(queryParam.getFirst()) : queryParam.getFirst());
                    if (queryParam.getSecond() != null) {
                        sb.append(EQUAL_CHAR).append(encoded ?
                                queryParamEncoder.encode(queryParam.getSecond()) : queryParam.getSecond());
                    }
                } catch (CharacterCodingException e) {
                    throw new IllegalArgumentException(e);
                }
                if (it.hasNext()) {
                    sb.append(AMPERSAND_CHAR);
                }
            }
        } else if (!isNullOrEmpty(builder.query)) {
            if (withQuestionMark) {
                sb.append(QUESTION_CHAR);
            }
            if (encoded) {
                try {
                    sb.append(queryEncoder.encode(builder.query));
                } catch (CharacterCodingException e) {
                    throw new IllegalArgumentException(e);
                }
            } else {
                sb.append(builder.query);
            }
        }
    }

    private String encodeFragment() {
        StringBuilder sb = new StringBuilder();
        appendFragment(sb, true, false);
        return sb.length() == 0 ? null : sb.toString();
    }

    private void appendFragment(StringBuilder sb, boolean encoded, boolean withHashSymbol) {
        if (!isNullOrEmpty(builder.fragment)) {
            if (withHashSymbol) {
                sb.append(NUMBER_SIGN_CHAR);
            }
            if (encoded) {
                try {
                    sb.append(fragmentEncoder.encode(builder.fragment));
                } catch (CharacterCodingException e) {
                    throw new IllegalArgumentException(e);
                }
            } else {
                sb.append(builder.fragment);
            }
        }
    }

    /**
     * Returns true if the parameter string is neither null nor empty.
     */
    private static boolean isNullOrEmpty(String str) {
        return str == null || str.isEmpty();
    }

    private static Pair<String, String> indexOf(char ch, String input) {
        int i = input.indexOf(ch);
        String k = i >= 0 ? input.substring(0, i) : input;
        String v = i >= 0 ? input.substring(i + 1) : null;
        return new Pair<>(k, v);
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof URL && toString().equals(other.toString());
    }

    @Override
    public String toString() {
        return toString(true);
    }

    @Override
    public int compareTo(URL o) {
        return toString().compareTo(o.toString());
    }

    /**
     * The URL builder class is required for building an URL. It uses fluent API methods
     * and pre-processes paralameter accordingly.
     */
    public static class Builder {

        private PercentEncoder regNameEncoder;

        private final PercentDecoder percentDecoder;

        private final QueryParameters queryParams;

        private final List<PathSegment> pathSegments;

        private Charset charset;

        private String scheme;

        private String schemeSpecificPart;

        private String userInfo;

        private String host;

        private String hostAddress;

        private ProtocolVersion protocolVersion;

        private Integer port;

        private String query;

        private String fragment;

        private boolean fatalResolveErrorsEnabled;

        private Builder() {
            charset(StandardCharsets.UTF_8);
            this.percentDecoder = new PercentDecoder();
            this.queryParams = new QueryParameters();
            this.pathSegments = new ArrayList<>();
        }

        /**
         * Set the character set of the URL. Default is UTF-8.
         * @param charset the chaarcter set
         * @return this builder
         */
        public Builder charset(Charset charset) {
            this.charset = charset;
            this.regNameEncoder = PercentEncoders.getRegNameEncoder(charset);
            return this;
        }

        public Builder scheme(String scheme) {
            if (!isNullOrEmpty(scheme)) {
                validateSchemeCharacters(scheme.toLowerCase(Locale.ROOT));
                this.scheme = scheme;
            }
            return this;
        }

        public Builder schemeSpecificPart(String schemeSpecificPart) {
            this.schemeSpecificPart = schemeSpecificPart;
            return this;
        }

        public Builder userInfo(String userInfo) {
            this.userInfo = userInfo;
            return this;
        }

        public Builder userInfo(String user, String pass) {
            try {
                // allow colons in usernames and passwords by percent-encoding them here
                this.userInfo = regNameEncoder.encode(user) + COLON_CHAR + regNameEncoder.encode(pass);
            } catch (MalformedInputException | UnmappableCharacterException e) {
                throw new IllegalArgumentException(e);
            }
            return this;
        }

        public Builder host(String host) {
            if (host != null) {
                this.host = host.toLowerCase(Locale.ROOT);
            }
            this.protocolVersion = ProtocolVersion.NONE;
            return this;
        }

        public Builder host(String host, ProtocolVersion protocolVersion) {
            if (host != null) {
                this.host = host.toLowerCase(Locale.ROOT);
            }
            this.protocolVersion = protocolVersion;
            return this;
        }

        public Builder fatalResolveErrors(boolean fatalResolveErrorsEnabled) {
            this.fatalResolveErrorsEnabled = fatalResolveErrorsEnabled;
            return this;
        }

        public Builder resolveFromHost(String hostname) {
            if (hostname == null) {
                return this;
            }
            if (hostname.isEmpty()) {
                host(EMPTY);
                return this;
            }
            try {
                InetAddress inetAddress = InetAddress.getByName(hostname);
                hostAddress = inetAddress.getHostAddress();
                host(inetAddress.getHostName(), inetAddress instanceof Inet6Address ?
                        ProtocolVersion.IPV6 : inetAddress instanceof Inet4Address ?
                        ProtocolVersion.IPV4 : ProtocolVersion.NONE);
                return this;
            } catch (UnknownHostException e) {
                if (fatalResolveErrorsEnabled) {
                    throw new IllegalStateException(e);
                }
                logger.log(Level.WARNING, e.getMessage(), e);
                if (e.getMessage() != null && !e.getMessage().endsWith("invalid IPv6 address") &&
                        hostname.charAt(0) != LEFT_BRACKET_CHAR &&
                        hostname.charAt(hostname.length() - 1) != RIGHT_BRACKET_CHAR) {
                    try {
                        String idna = IDN.toASCII(percentDecoder.decode(hostname));
                        host(idna, ProtocolVersion.NONE);
                    } catch (CharacterCodingException e2) {
                        throw new IllegalArgumentException(e2);
                    }
                }
            }
            return this;
        }

        public Builder port(Integer port) {
            this.port = port;
            return this;
        }

        public Builder path(String path) {
            try {
                parser().parsePathWithQueryAndFragment(this, path);
            } catch (CharacterCodingException e) {
                throw new IllegalArgumentException(e);
            }
            return this;
        }

        public Builder pathSegments(String... segments) {
            for (String segment : segments) {
                pathSegment(segment);
            }
            return this;
        }

        public Builder pathSegment(String segment) {
            if (pathSegments.isEmpty() && !isNullOrEmpty(host) && !isNullOrEmpty(segment)) {
                 pathSegments.add(EMPTY_SEGMENT);
            }
            pathSegments.add(new PathSegment(segment));
            return this;
        }

        public Builder resetQueryParams() {
            queryParams.clear();
            query = null;
            return this;
        }

        /**
         * Add a query parameter. Query parameters will be encoded in the order added.
         *
         * Using query strings to encode key=value pairs is not part of the URI/URL specification.
         * It is specified by http://www.w3.org/TR/html401/interact/forms.html#form-content-type.
         *
         * If you use this method to build a query string, or created this builder from an URL with a query string that can
         * successfully be parsed into query param pairs, you cannot subsequently use
         * {@link Builder#query(String)}.
         *
         * @param name  param name
         * @param value param value
         * @return this
         */
        public Builder queryParam(String name, String value) {
            queryParams.add(name, value);
            return this;
        }

        /**
         * Set the complete query string of arbitrary structure. This is useful when you want to specify a query string that
         * is not of key=value format. If the query has previously been set via this method, subsequent calls will overwrite
         * that query.
         * If you use this method, or create a builder from a URL whose query is not parseable into query param pairs, you
         * cannot subsequently use {@link Builder#queryParam(String, String)}.
         *
         * @param query Complete URI query, as specified by https://tools.ietf.org/html/rfc3986#section-3.4
         * @return this
         */
        public Builder query(String query) {
            this.query = query;
            return this;
        }

        /**
         * Add a matrix param to the last added path segment. If no segments have been added, the param will be added to the
         * root. Matrix params will be encoded in the order added.
         *
         * @param name  param name
         * @param value param value
         * @return this
         */
        public Builder matrixParam(String name, String value) {
            if (pathSegments.isEmpty()) {
                pathSegment(EMPTY);
            }
            pathSegments.get(pathSegments.size() - 1).getMatrixParams().add(new Pair<>(name, value));
            return this;
        }

        /**
         * Set the fragment.
         *
         * @param fragment fragment string
         * @return this
         */
        public Builder fragment(String fragment) {
            if (!isNullOrEmpty(fragment)) {
                this.fragment = fragment;
            }
            return this;
        }

        public URL build() {
            return new URL(this);
        }

        /**
         * Encode the current builder state into a string.
         *
         * @return a string
         */
        String toUrlString() {
            return build().toExternalForm();
        }

        void validateSchemeCharacters(String scheme) {
            boolean valid;
            for (int i = 0; i < scheme.length(); i++) {
                char c = scheme.charAt(i);
                if (i == 0) {
                    valid = ('a' <= c && c <= 'z') || ('A' <= c && c <= 'Z');
                } else {
                    valid = ('a' <= c && c <= 'z') || ('A' <= c && c <= 'Z') ||
                            ('0' <= c && c <= '9') || c == '+' || c == '-' || c == '.';
                }
                if (!valid) {
                    throw new IllegalArgumentException("invalid scheme character in: " + scheme);
                }
            }
        }
    }

    /**
     * A URL parser class.
     */
    public static class Parser {

        private final Builder builder;

        private Parser() {
            builder = new Builder();
        }

        public URL parse(String input)
                throws URLSyntaxException, MalformedInputException, UnmappableCharacterException {
            return parse(input, true);
        }

        public URL parse(String input, boolean resolve)
                throws URLSyntaxException, MalformedInputException, UnmappableCharacterException {
            if (isNullOrEmpty(input)) {
                return NULL_URL;
            }
            if (input.indexOf('\n') >= 0) {
                return NULL_URL;
            }
            if (input.indexOf('\t') >= 0) {
                return NULL_URL;
            }
            String remaining = parseScheme(builder, input);
            if (remaining != null) {
                remaining = remaining.replace('\\', SEPARATOR_CHAR);
                builder.schemeSpecificPart(remaining);
                if (remaining.startsWith(DOUBLE_SLASH)) {
                    Scheme scheme = SchemeRegistry.getInstance().getScheme(builder.scheme);
                    if (builder.scheme == null || scheme.getDefaultPort() == -1) {
                        builder.host(EMPTY);
                    } else {
                        remaining = remaining.substring(2);
                        int i = remaining.indexOf(SEPARATOR_CHAR);
                        int j = remaining.indexOf(QUESTION_CHAR);
                        int pos = i >= 0 && j >= 0 ? Math.min(i, j) : i >= 0 ? i : j >= 0 ? j : -1;
                        String host = (pos >= 0 ? remaining.substring(0, pos) : remaining);
                        parseHostAndPort(builder, parseUserInfo(builder, host), resolve);
                        if (builder.host == null) {
                            return NULL_URL;
                        }
                        remaining = pos >= 0 ? remaining.substring(pos) : EMPTY;
                    }
                }
                if (!isNullOrEmpty(remaining)) {
                    try {
                        parsePathWithQueryAndFragment(builder, remaining);
                    } catch (CharacterCodingException e) {
                        throw new URLSyntaxException(e);
                    }
                }
            }
            return builder.build();
        }

        private String parseScheme(Builder builder, String input) {
            Pair<String, String> p = indexOf(COLON_CHAR, input);
            if (p.getSecond() == null) {
                return input;
            }
            if (!isNullOrEmpty(p.getFirst())) {
                builder.scheme(p.getFirst());
            }
            return p.getSecond();
        }

        private String parseUserInfo(Builder builder, String input)
                throws MalformedInputException, UnmappableCharacterException {
            String remaining = input;
            int i = input.lastIndexOf(AT_CHAR);
            if (i > 0) {
                remaining = input.substring(i + 1);
                String userInfo = input.substring(0, i);
                builder.userInfo(builder.percentDecoder.decode(userInfo));
            }
            return remaining;
        }

        private void parseHostAndPort(Builder builder, String rawHost, boolean resolve)
                throws URLSyntaxException {
            String host = rawHost;
            if (host.indexOf(LEFT_BRACKET_CHAR) == 0) {
                int i = host.lastIndexOf(RIGHT_BRACKET_CHAR);
                if (i >= 0) {
                    builder.port(parsePort(host.substring(i + 1)));
                    host = host.substring(1, i);
                }
            } else {
                int i = host.indexOf(COLON_CHAR);
                if (i >= 0) {
                    builder.port(parsePort(host.substring(i)));
                    host = host.substring(0, i);
                }
            }
            if (resolve) {
                builder.resolveFromHost(host);
            } else {
                builder.host(host);
            }
        }

        private Integer parsePort(String portStr) throws URLSyntaxException {
            if (portStr == null || portStr.isEmpty()) {
                return null;
            }
            int i = portStr.indexOf(COLON_CHAR);
            if (i >= 0) {
                portStr = portStr.substring(i + 1);
                if (portStr.isEmpty()) {
                    return -1;
                }
            }
            try {
                int port = Integer.parseInt(portStr);
                if (port > 0 && port < 65536) {
                    return port;
                } else {
                    throw new URLSyntaxException("invalid port");
                }
            } catch (NumberFormatException e) {
                throw new URLSyntaxException("no numeric port: " + portStr);
            }
        }

        private void parsePathWithQueryAndFragment(Builder builder, String input)
                throws MalformedInputException, UnmappableCharacterException {
            if (input == null) {
                return;
            }
            int i = input.lastIndexOf(NUMBER_SIGN_CHAR);
            if (i >= 0) {
                builder.fragment(builder.percentDecoder.decode(input.substring(i + 1)));
                input = input.substring(0, i);
            }
            i = input.indexOf(QUESTION_CHAR);
            if (i >= 0) {
                parseQuery(builder, input.substring(i + 1));
                input = input.substring(0, i);
            }
            if (input.length() > 0 && input.charAt(0) == SEPARATOR_CHAR) {
                builder.pathSegment(EMPTY);
            }
            String s = input;
            while (s != null) {
                Pair<String, String> pair = indexOf(SEPARATOR_CHAR, s);
                String elem = pair.getFirst();
                if (!elem.isEmpty()) {
                    if (elem.charAt(0) == SEMICOLON_CHAR) {
                        builder.pathSegment(EMPTY);
                        String t = elem.substring(1);
                        while (t != null) {
                            Pair<String, String> pathWithMatrixElem = indexOf(SEMICOLON_CHAR, t);
                            String matrixElem = pathWithMatrixElem.getFirst();
                            Pair<String, String> p = indexOf(EQUAL_CHAR, matrixElem);
                            builder.matrixParam(builder.percentDecoder.decode(p.getFirst()),
                                    builder.percentDecoder.decode(p.getSecond()));
                            t = pathWithMatrixElem.getSecond();
                        }
                    } else {
                        String t = elem;
                        i = 0;
                        while (t != null) {
                            Pair<String, String> pathWithMatrixElem = indexOf(SEMICOLON_CHAR, t);
                            String segment = pathWithMatrixElem.getFirst();
                            if (i == 0) {
                                builder.pathSegment(builder.percentDecoder.decode(segment));
                            } else {
                                Pair<String, String> p = indexOf(EQUAL_CHAR, segment);
                                builder.matrixParam(builder.percentDecoder.decode(p.getFirst()),
                                        builder.percentDecoder.decode(p.getSecond()));
                            }
                            t = pathWithMatrixElem.getSecond();
                            i++;
                        }
                    }
                }
                s = pair.getSecond();
            }
            if (input.endsWith("/")) {
                builder.pathSegment(EMPTY);
            }
        }

        private void parseQuery(Builder builder, String query)
                throws MalformedInputException, UnmappableCharacterException {
            if (query == null) {
                return;
            }
            String s = query;
            while (s != null) {
                Pair<String, String> p = indexOf(AMPERSAND_CHAR, s);
                Pair<String, String> param = indexOf(EQUAL_CHAR, p.getFirst());
                if (!isNullOrEmpty(param.getFirst())) {
                    builder.queryParam(builder.percentDecoder.decode(param.getFirst()),
                            builder.percentDecoder.decode(param.getSecond()));
                }
                s = p.getSecond();
            }
            if (builder.queryParams.isEmpty()) {
                builder.query(builder.percentDecoder.decode(query));
            } else {
                builder.query(query);
            }
        }
    }

    /**
     * The URL resolver class is a class for resolving a relative URL specification to a base URL.
     */
    public static class Resolver {

        private final URL base;

        public Resolver(URL base) {
            this.base = base;
        }

        public URL resolve(String relative)
                throws URLSyntaxException, MalformedInputException, UnmappableCharacterException {
            if (relative == null) {
                return null;
            }
            if (relative.isEmpty()) {
                return base;
            }
            URL url = parser().parse(relative);
            return resolve(url);
        }

        public URL resolve(URL relative)
                throws URLSyntaxException {
            if (relative == null || relative.equals(NULL_URL)) {
                throw new URLSyntaxException("relative URL is invalid");
            }
            if (!base.isAbsolute()) {
                throw new URLSyntaxException("base URL is not absolute");
            }
            Builder builder = new Builder();
            if (relative.isOpaque()) {
                builder.scheme(relative.getScheme());
                builder.schemeSpecificPart(relative.getSchemeSpecificPart());
                return builder.build();
            }
            if (relative.isAbsolute()) {
                builder.scheme(relative.getScheme());
            } else {
                builder.scheme(base.getScheme());
            }
            if (!isNullOrEmpty(relative.getScheme()) || !isNullOrEmpty(relative.getHost())) {
                builder.host(relative.getDecodedHost(), relative.getProtocolVersion()).port(relative.getPort());
                builder.path(relative.getPath());
                return builder.build();
            }
            if (base.isOpaque()) {
                builder.schemeSpecificPart(base.getSchemeSpecificPart());
                return builder.build();
            }
            if (relative.getHost() != null) {
                builder.host(relative.getDecodedHost(), relative.getProtocolVersion()).port(relative.getPort());
            } else {
                builder.host(base.getDecodedHost(), base.getProtocolVersion()).port(base.getPort());
            }
            builder.path(resolvePath(base, relative));
            return builder.build();
        }

        private String resolvePath(URL base, URL relative) {
            String basePath = base.getPath();
            String baseQuery = base.getQuery();
            String baseFragment = base.getFragment();
            String relPath = relative.getPath();
            String relQuery = relative.getQuery();
            String relFragment = relative.getFragment();
            boolean isBase = false;
            String merged;
            List<String> result = new ArrayList<>();
            if (isNullOrEmpty(relPath)) {
                merged = basePath;
                isBase = true;
            } else if (relPath.charAt(0) != SEPARATOR_CHAR && !isNullOrEmpty(basePath)) {
                merged = basePath.substring(0, basePath.lastIndexOf(SEPARATOR_CHAR) + 1) + relPath;
            } else {
                merged = relPath;
            }
            if (isNullOrEmpty(merged)) {
                return EMPTY;
            }
            String[] parts = merged.split("/", -1);
            for (String part : parts) {
                switch (part) {
                    case EMPTY:
                    case ".":
                        break;
                    case "..":
                        if (result.size() > 0) {
                            result.remove(result.size() - 1);
                        }
                        break;
                    default:
                        result.add(part);
                        break;
                }
            }
            if (parts.length > 0) {
                switch (parts[parts.length - 1]) {
                    case EMPTY:
                    case ".":
                    case "..":
                        result.add(EMPTY);
                        break;
                    default:
                        break;
                }
            }
            StringBuilder sb = new StringBuilder();
            sb.append(String.join(Character.toString(SEPARATOR_CHAR), result));
            if (sb.length() == 0 && result.size() == 1) {
                sb.append(SEPARATOR_CHAR);
            }
            if (!isNullOrEmpty(relQuery)) {
                sb.append(QUESTION_CHAR).append(relQuery);
            } else if (isBase && !isNullOrEmpty(baseQuery)) {
                sb.append(QUESTION_CHAR).append(baseQuery);
            }
            if (!isNullOrEmpty(relFragment)) {
                sb.append(NUMBER_SIGN_CHAR).append(relFragment);
            } else if (isBase && !isNullOrEmpty(baseFragment)) {
                sb.append(NUMBER_SIGN_CHAR).append(baseFragment);
            }
            return sb.toString();
        }
    }

    private static class URLWithFragmentComparator implements Comparator<URL> {

        @Override
        public int compare(URL o1, URL o2) {
            return o1.toString(true).compareTo(o2.toString(true));
        }
    }

    private static class URLWithoutFragmentComparator implements Comparator<URL> {

        @Override
        public int compare(URL o1, URL o2) {
            return o1.toString(false).compareTo(o2.toString(false));
        }
    }

    private static class Pair<K, V> {
        private final K first;
        private final V second;

        Pair(K first, V second) {
            this.first = first;
            this.second = second;
        }

        K getFirst() {
            return first;
        }

        V getSecond() {
            return second;
        }

        @Override
        public String toString() {
            return first + "=" + second;
        }
    }

    /**
     *  A path segment with any associated matrix params.
     */
    private static class PathSegment {

        private final String segment;

        private final List<Pair<String, String>> params;

        PathSegment(String segment) {
            this.segment = segment;
            this.params = new ArrayList<>();
        }

        List<Pair<String, String>> getMatrixParams() {
            return params;
        }

        @Override
        public String toString() {
            return segment + ";" + params;
        }
    }
}
