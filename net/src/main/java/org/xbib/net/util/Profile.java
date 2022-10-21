package org.xbib.net.util;

/**
 *
 */
public enum Profile {
    NONE(codepoint -> true),
    ALPHA(codepoint -> !CharUtils.isAlpha(codepoint)),
    ALPHANUM(codepoint -> !CharUtils.isAlphaDigit(codepoint)),
    FRAGMENT(codepoint -> !CharUtils.isFragment(codepoint)),
    IFRAGMENT(codepoint -> !CharUtils.isIfragment(codepoint)),
    PATH(codepoint -> !CharUtils.isPath(codepoint)),
    IPATH(codepoint -> !CharUtils.isIpath(codepoint)),
    IUSERINFO(codepoint -> !CharUtils.isIuserinfo(codepoint)),
    USERINFO(codepoint -> !CharUtils.isUserInfo(codepoint)),
    QUERY(codepoint -> !CharUtils.isQuery(codepoint)),
    IQUERY(codepoint -> !CharUtils.isIquery(codepoint)),
    SCHEME(codepoint -> !CharUtils.isScheme(codepoint)),
    PATHNODELIMS(codepoint -> !CharUtils.isPathNoDelims(codepoint)),
    IPATHNODELIMS(codepoint -> !CharUtils.isIpathnodelims(codepoint)),
    IPATHNODELIMS_SEG(codepoint -> !CharUtils.isIpathnodelims(codepoint) && codepoint != '@' && codepoint != ':'),
    IREGNAME(codepoint -> !CharUtils.isIregname(codepoint)),
    IHOST(codepoint -> !CharUtils.isIhost(codepoint)),
    IPRIVATE(codepoint -> !CharUtils.isIprivate(codepoint)),
    RESERVED(codepoint -> !CharUtils.isReserved(codepoint)),
    IUNRESERVED(codepoint -> !CharUtils.isIunreserved(codepoint)),
    UNRESERVED(codepoint -> !CharUtils.isUnreserved(codepoint)),
    SCHEMESPECIFICPART(codepoint -> !CharUtils.isIunreserved(codepoint) && !CharUtils.isReserved(codepoint)
            && !CharUtils.isIprivate(codepoint)
            && !CharUtils.isPctEnc(codepoint)
            && codepoint != '#'),
    AUTHORITY(codepoint -> !CharUtils.isRegname(codepoint) && !CharUtils.isUserInfo(codepoint) && !CharUtils.isGenDelim(codepoint)),
    ASCIISANSCRLF(codepoint -> !CharUtils.inRange(codepoint, 1, 9) && !CharUtils.inRange(codepoint, 14, 127)),
    PCT(codepoint -> !CharUtils.isPctEnc(codepoint)),
    STD3ASCIIRULES(codepoint -> !CharUtils.inRange(codepoint, 0x0000, 0x002C) &&
            !CharUtils.inRange(codepoint, 0x002E, 0x002F) &&
            !CharUtils.inRange(codepoint, 0x003A, 0x0040) &&
            !CharUtils.inRange(codepoint, 0x005B, 0x0060) &&
            !CharUtils.inRange(codepoint, 0x007B, 0x007F));

    private final CodepointFilter filter;

    Profile(CodepointFilter filter) {
        this.filter = filter;
    }

    public CodepointFilter filter() {
        return filter;
    }

    public boolean check(int codepoint) {
        return filter.accept(codepoint);
    }
}
