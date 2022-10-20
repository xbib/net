package org.xbib.net;

import java.nio.charset.MalformedInputException;
import java.nio.charset.UnmappableCharacterException;
import java.util.ArrayList;
import java.util.List;

/**
 * The URL resolver class is a class for resolving a relative URL specification to a base URL.
 */
public class URLResolver {

    private static final String EMPTY = "";

    private final URL base;

    URLResolver(URL base) {
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
        // TODO(jprante) parser(charset, codingErrorAction)
        URL url = URL.parser().parse(relative);
        return resolve(url);
    }

    public URL resolve(URL relative)
            throws URLSyntaxException {
        if (relative == null || relative.equals(URL.NULL_URL)) {
            throw new URLSyntaxException("relative URL is invalid");
        }
        if (!base.isAbsolute()) {
            throw new URLSyntaxException("base URL is not absolute");
        }
        URLBuilder builder = new URLBuilder();
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
        if (!URL.isNullOrEmpty(relative.getScheme()) || !URL.isNullOrEmpty(relative.getHost())) {
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
        if (URL.isNullOrEmpty(relPath)) {
            merged = basePath;
            isBase = true;
        } else if (relPath.charAt(0) != URL.SEPARATOR_CHAR && !URL.isNullOrEmpty(basePath)) {
            merged = basePath.substring(0, basePath.lastIndexOf(URL.SEPARATOR_CHAR) + 1) + relPath;
        } else {
            merged = relPath;
        }
        if (URL.isNullOrEmpty(merged)) {
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
        sb.append(String.join(Character.toString(URL.SEPARATOR_CHAR), result));
        if (sb.length() == 0 && result.size() == 1) {
            sb.append(URL.SEPARATOR_CHAR);
        }
        if (!URL.isNullOrEmpty(relQuery)) {
            sb.append(URL.QUESTION_CHAR).append(relQuery);
        } else if (isBase && !URL.isNullOrEmpty(baseQuery)) {
            sb.append(URL.QUESTION_CHAR).append(baseQuery);
        }
        if (!URL.isNullOrEmpty(relFragment)) {
            sb.append(URL.NUMBER_SIGN_CHAR).append(relFragment);
        } else if (isBase && !URL.isNullOrEmpty(baseFragment)) {
            sb.append(URL.NUMBER_SIGN_CHAR).append(baseFragment);
        }
        return sb.toString();
    }
}
