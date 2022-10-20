package org.xbib.net;

import java.net.IDN;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnmappableCharacterException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.xbib.datastructures.common.Pair;

/**
 * The URL builder class is required for building an URL. It uses fluent API methods
 * and pre-processes parameter accordingly.
 */
public class URLBuilder {

    private static final String EMPTY = "";

    static final PathSegment EMPTY_SEGMENT = new PathSegment(EMPTY);

    PercentEncoder regNameEncoder;

    PercentEncoder percentEncoder;

    PercentDecoder percentDecoder;

    ParameterBuilder queryParams;

    final List<PathSegment> pathSegments;

    Charset charset;

    CodingErrorAction codingErrorAction;

    String scheme;

    String schemeSpecificPart;

    String userInfo;

    String host;

    String hostAddress;

    ProtocolVersion protocolVersion;

    Integer port;

    String query;

    String fragment;

    boolean fatalResolveErrorsEnabled;

    URLBuilder() {
        this.pathSegments = new ArrayList<>();
        charset(StandardCharsets.UTF_8, CodingErrorAction.REPLACE);
    }

    /**
     * Set the character set of the URL. Default is UTF-8.
     *
     * @param charset           the character set
     * @param codingErrorAction the coding error action
     * @return this builder
     */
    public URLBuilder charset(Charset charset, CodingErrorAction codingErrorAction) {
        this.charset = charset;
        this.codingErrorAction = codingErrorAction;
        this.percentEncoder = PercentEncoders.getQueryEncoder(charset);
        this.regNameEncoder = PercentEncoders.getRegNameEncoder(charset);
        CharsetDecoder charsetDecoder = charset.newDecoder()
                .onMalformedInput(codingErrorAction)
                .onUnmappableCharacter(codingErrorAction);
        this.percentDecoder = new PercentDecoder(charsetDecoder);
        this.queryParams = Parameter.builder();
        return this;
    }

    public URLBuilder scheme(String scheme) {
        if (!URL.isNullOrEmpty(scheme)) {
            validateSchemeCharacters(scheme.toLowerCase(Locale.ROOT));
            this.scheme = scheme;
        }
        return this;
    }

    public String scheme() {
        return scheme;
    }

    public URLBuilder schemeSpecificPart(String schemeSpecificPart) {
        this.schemeSpecificPart = schemeSpecificPart;
        return this;
    }

    public URLBuilder userInfo(String userInfo) {
        this.userInfo = userInfo;
        return this;
    }

    public URLBuilder userInfo(String user, String pass) {
        try {
            // allow colons in usernames and passwords by percent-encoding them here
            this.userInfo = regNameEncoder.encode(user) + URL.COLON_CHAR + regNameEncoder.encode(pass);
        } catch (MalformedInputException | UnmappableCharacterException e) {
            throw new IllegalArgumentException(e);
        }
        return this;
    }

    public URLBuilder host(String host) {
        if (host != null) {
            this.host = host.toLowerCase(Locale.ROOT);
        }
        this.protocolVersion = ProtocolVersion.NONE;
        return this;
    }

    public URLBuilder host(String host, ProtocolVersion protocolVersion) {
        if (host != null) {
            this.host = host.toLowerCase(Locale.ROOT);
        }
        this.protocolVersion = protocolVersion;
        return this;
    }

    public String host() {
        return host;
    }

    public URLBuilder fatalResolveErrors(boolean fatalResolveErrorsEnabled) {
        this.fatalResolveErrorsEnabled = fatalResolveErrorsEnabled;
        return this;
    }

    public URLBuilder resolveFromHost(String hostname) {
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
            if (e.getMessage() != null && !e.getMessage().endsWith("invalid IPv6 address") &&
                    hostname.charAt(0) != URL.LEFT_BRACKET_CHAR &&
                    hostname.charAt(hostname.length() - 1) != URL.RIGHT_BRACKET_CHAR) {
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

    public URLBuilder port(Integer port) {
        this.port = port;
        return this;
    }

    public Integer port() {
        return port;
    }

    public URLBuilder path(String path) {
        try {
            URL.parser(charset, codingErrorAction).parsePathWithQueryAndFragment(this, path);
        } catch (CharacterCodingException e) {
            throw new IllegalArgumentException(e);
        }
        return this;
    }

    public URLBuilder pathSegments(String... segments) {
        for (String segment : segments) {
            pathSegment(segment);
        }
        return this;
    }

    public URLBuilder pathSegment(String segment) {
        if (pathSegments.isEmpty() && !URL.isNullOrEmpty(host) && !URL.isNullOrEmpty(segment)) {
            pathSegments.add(EMPTY_SEGMENT);
        }
        pathSegments.add(new PathSegment(segment));
        return this;
    }

    /**
     * Add a query parameter. Query parameters will be encoded in the order added.
     * <p>
     * Using query strings to encode key=value pairs is not part of the URI/URL specification.
     * It is specified by <a href="http://www.w3.org/TR/html401/interact/forms.html#form-content-type">http://www.w3.org/TR/html401/interact/forms.html#form-content-type</a>.
     * <p>
     * If you use this method to build a query string, or created this builder from an URL with a query string that can
     * successfully be parsed into query param pairs, you cannot subsequently use
     * {@link URLBuilder#query(String)}.
     *
     * @param name  param name
     * @param value param value
     * @return this
     */
    public URLBuilder queryParam(String name, Object value) {
        queryParams.add(name, value);
        return this;
    }

    /**
     * Set the complete and encoded query string of arbitrary structure. This is useful when you want to specify a query string that
     * is not of key=value format. If the query has previously been set via this method, subsequent calls will overwrite
     * that query.
     * If you use this method, or create a builder from a URL whose query is not parseable into query param pairs, you
     * cannot subsequently use {@link URLBuilder#queryParam(String, Object)}.
     *
     * @param query complete and encoded URI query, as specified by <a href="https://tools.ietf.org/html/rfc3986#section-3.4">https://tools.ietf.org/html/rfc3986#section-3.4</a>
     * @return this
     */
    public URLBuilder query(String query) {
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
    public URLBuilder matrixParam(String name, String value) {
        if (pathSegments.isEmpty()) {
            pathSegment(EMPTY);
        }
        pathSegments.get(pathSegments.size() - 1).getMatrixParams().add(Pair.of(name, value));
        return this;
    }

    /**
     * Set the fragment.
     *
     * @param fragment fragment string
     * @return this
     */
    public URLBuilder fragment(String fragment) {
        if (!URL.isNullOrEmpty(fragment)) {
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


    /**
     *  A path segment with associated matrix params, if any.
     */
    public static class PathSegment {

        private final String segment;

        private final List<Pair<String, String>> params;

        PathSegment(String segment) {
            this.segment = segment;
            this.params = new ArrayList<>();
        }

        public String getSegment() {
            return segment;
        }

        public List<Pair<String, String>> getMatrixParams() {
            return params;
        }

        @Override
        public String toString() {
            return segment + ";" + params;
        }
    }
}
