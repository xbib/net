package org.xbib.net;

import org.xbib.datastructures.common.Pair;
import org.xbib.net.scheme.Scheme;
import org.xbib.net.scheme.SchemeRegistry;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnmappableCharacterException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

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
 * in accordance with the link: <a href="https://url.spec.whatwg.org/">[{@code WHATWG} URL standard]</a>.
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

    static final URL NULL_URL = URL.builder().build();

    static final char SEPARATOR_CHAR = '/';

    static final char QUESTION_CHAR = '?';

    static final char COLON_CHAR = ':';

    static final char SEMICOLON_CHAR = ';';

    static final char EQUAL_CHAR = '=';

    static final char AMPERSAND_CHAR = '&';

    static final char NUMBER_SIGN_CHAR = '#';

    static final char AT_CHAR = '@';

    static final char LEFT_BRACKET_CHAR = '[';

    static final char RIGHT_BRACKET_CHAR = ']';

    static final String DOUBLE_SLASH = "//";

    private final transient URLBuilder builder;

    private final transient Scheme scheme;

    private final transient PercentEncoder queryParamEncoder;

    private final transient PercentEncoder regNameEncoder;

    private final transient PercentEncoder pathEncoder;

    private final transient PercentEncoder matrixEncoder;

    private final transient PercentEncoder fragmentEncoder;

    private final String hostinfo;

    private final String path;

    private final String query;

    private final String fragment;

    private String internalStringRepresentation;

    private String externalStringRepresentation;

    URL(URLBuilder builder) {
        this.builder = builder;
        this.scheme = SchemeRegistry.getInstance().getScheme(builder.scheme);
        this.queryParamEncoder = PercentEncoders.getQueryParamEncoder(builder.charset);
        this.regNameEncoder = PercentEncoders.getRegNameEncoder(builder.charset);
        this.pathEncoder = PercentEncoders.getPathEncoder(builder.charset);
        this.matrixEncoder = PercentEncoders.getMatrixEncoder(builder.charset);
        this.fragmentEncoder = PercentEncoders.getFragmentEncoder(builder.charset);
        this.hostinfo = encodeHostInfo();
        this.path = encodePath();
        this.query = encodeQuery();
        this.fragment = encodeFragment();
    }

    public static URLBuilder builder() {
        return new URLBuilder();
    }

    public static URLBuilder http() {
        return new URLBuilder().scheme(Scheme.HTTP);
    }

    public static URLBuilder https() {
        return new URLBuilder().scheme(Scheme.HTTPS);
    }

    public static URLParser parser() {
        return new URLParser(StandardCharsets.UTF_8, CodingErrorAction.REPORT);
    }

    public static URLParser parser(Charset charset, CodingErrorAction codingErrorAction) {
        return new URLParser(charset, codingErrorAction);
    }

    public static URLResolver base(String base) {
        return base(URL.create(base));
    }

    public static URLResolver base(URL base) {
        return new URLResolver(base);
    }

    /**
     * Return a special URL denoting the fact that this URL should be considered as invalid.
     * The URL has no scheme.
     * @return url
     */
    public static URL nullUrl() {
        return NULL_URL;
    }

    public static URL from(String input) {
        return from(input, StandardCharsets.UTF_8, CodingErrorAction.REPORT, true, false);
    }

    public static URL create(String input) {
        return from(input, StandardCharsets.UTF_8, CodingErrorAction.REPORT, false, false);
    }

    public static URL create(String input, boolean disableException) {
        return from(input, StandardCharsets.UTF_8, CodingErrorAction.REPORT, false, disableException);
    }

    public static URL from(String input,
                           Charset charset, CodingErrorAction codingErrorAction,
                           boolean resolve, boolean disableException) {
        try {
            return parser(charset, codingErrorAction).parse(input, resolve);
        } catch (URLSyntaxException | MalformedInputException | UnmappableCharacterException e) {
            if (disableException) {
                return null;
            } else {
                throw new IllegalArgumentException(e);
            }
        }
    }

    public URL resolve(String spec) {
        return from(this, spec, false);
    }

    public URL resolve(String spec, boolean disableException) {
        return from(this, spec, disableException);
    }

    public static URL from(URL base, String spec, boolean disableException) {
        try {
            return new URLResolver(base).resolve(spec);
        } catch (URLSyntaxException | MalformedInputException | UnmappableCharacterException e) {
            if (disableException) {
                return null;
            } else {
                throw new IllegalArgumentException(e);
            }
        }
    }

    public URL resolve(URL spec) {
        return from(this, spec, false);
    }

    public URL resolve(URL spec, boolean disableException) {
        return from(this, spec, disableException);
    }

    public static URL from(URL base, URL spec, boolean disableException) {
        try {
            return new URLResolver(base).resolve(spec);
        } catch (URLSyntaxException e) {
            if (disableException) {
                return null;
            } else {
                throw new IllegalArgumentException(e);
            }
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

    public static Parameter parseQueryString(String query) {
        return parseQueryString(query, false);
    }

    public static Parameter parseQueryString(String query, boolean disableException) {
        Objects.requireNonNull(query);
        try {
            return URL.parser().parse(query.charAt(0) == QUESTION_CHAR ? query : QUESTION_CHAR + query).getQueryParams();
        } catch (URLSyntaxException | MalformedInputException | UnmappableCharacterException e) {
            if (disableException) {
                return null;
            } else {
                throw new IllegalArgumentException(e);
            }
        }
    }

    public URLBuilder mutator() {
        return builder;
    }

    public URLBuilder newBuilder() {
        return new URLBuilder();
    }

    private String decode(String input) {
        try {
            return builder.percentDecoder.decode(input);
        } catch (MalformedInputException | UnmappableCharacterException e) {
            throw new IllegalArgumentException(e);
        }
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
        return decode(p.getKey());
    }

    public String getPassword() {
        if (builder.userInfo == null) {
            return null;
        }
        Pair<String, String> p = indexOf(COLON_CHAR, builder.userInfo);
        return decode(p.getValue());
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

    public List<URLBuilder.PathSegment> getPathSegments() {
        return builder.pathSegments;
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

    public Parameter getQueryParams() {
        return builder.queryParams.build();
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
     * Whether this is a hierarchical URL or not. That is, a URL that allows multiple path segments.
     *
     * The term <em>hierarchical</em> comes form the URI standard
     * (<a href="https://www.ietf.org/rfc/rfc3986.txt">RFC 3986</a>).
     * Other libraries might refer to it as <em>relative</em> or <em>cannot-be-a-base-URL</em>.
     * The later is the current WHATWG URL standard
     * (see <a href="https://github.com/whatwg/url/issues/89">whatwg/url#89</a> for the rationale).
     * @return true if URL is hierarchical
     */
    public boolean isHierarchical() {
        return !isOpaque();
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
        return scheme != null ? scheme.normalize(this) : this;
    }

    public String toExternalForm() {
        if (externalStringRepresentation != null) {
            return externalStringRepresentation;
        }
        externalStringRepresentation = writeExternalForm();
        return externalStringRepresentation;
    }

    public java.net.URL toURL() throws MalformedURLException {
        return new java.net.URL(toString());
    }

    public URI toURI() {
        return URI.create(toString());
    }

    public Path toPath() {
        return Paths.get(toURI());
    }

    public InputStream openStream() throws IOException {
        return toURL().openStream();
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
        Iterator<URLBuilder.PathSegment> it = builder.pathSegments.iterator();
        while (it.hasNext()) {
            URLBuilder.PathSegment pathSegment = it.next();
            try {
                sb.append(encoded ? pathEncoder.encode(pathSegment.getSegment()) : pathSegment.getSegment());
                for (Pair<String, String> matrixParam : pathSegment.getMatrixParams()) {
                    sb.append(SEMICOLON_CHAR).append(encoded ?
                            matrixEncoder.encode(matrixParam.getKey()) : matrixParam.getKey());
                    if (matrixParam.getValue() != null) {
                        sb.append(EQUAL_CHAR).append(encoded ?
                                matrixEncoder.encode(matrixParam.getValue()) : matrixParam.getValue());
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

    private void appendQuery(StringBuilder sb, boolean withEncoding, boolean withQuestionMark) {
        // a given query has priority
        if (!isNullOrEmpty(builder.query)) {
            if (withQuestionMark) {
                sb.append(QUESTION_CHAR);
            }
            // ignore encoding, the query string must already be encoded
            sb.append(builder.query);
        } else if (builder.queryParams != null && !builder.queryParams.isEmpty()) {
            if (withQuestionMark) {
                sb.append(QUESTION_CHAR);
            }
            Iterator<Pair<String, Object>> it = builder.queryParams.iterator();
            while (it.hasNext()) {
                Pair<String, Object> queryParam = it.next();
                try {
                    String k = withEncoding ? queryParamEncoder.encode(queryParam.getKey()) : queryParam.getKey();
                    sb.append(k);
                    if (queryParam.getValue() != null) {
                        Object v = withEncoding && queryParam.getValue() instanceof CharSequence ?
                                queryParamEncoder.encode((CharSequence) queryParam.getValue()) : queryParam.getValue();
                        sb.append(EQUAL_CHAR).append(v);
                    }
                } catch (CharacterCodingException e) {
                    throw new IllegalArgumentException(e);
                }
                if (it.hasNext()) {
                    sb.append(AMPERSAND_CHAR);
                }
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
    static boolean isNullOrEmpty(String str) {
        return str == null || str.isEmpty();
    }

    static Pair<String, String> indexOf(char ch, String input) {
        int i = input.indexOf(ch);
        String k = i >= 0 ? input.substring(0, i) : input;
        String v = i >= 0 ? input.substring(i + 1) : null;
        return Pair.of(k, v);
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

    @SuppressWarnings("serial")
    private static class URLWithFragmentComparator implements Comparator<URL> {

        @Override
        public int compare(URL o1, URL o2) {
            return o1.toString(true).compareTo(o2.toString(true));
        }
    }

    @SuppressWarnings("serial")
    private static class URLWithoutFragmentComparator implements Comparator<URL> {

        @Override
        public int compare(URL o1, URL o2) {
            return o1.toString(false).compareTo(o2.toString(false));
        }
    }
}
