package org.xbib.net.resource;
/*
 * Copyright (c) 2000, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/* @test
 * @summary Unit test for java.net.IRI
 * @bug 8019345 6345502 6363889 6345551 6348515
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;


public class Test {

    static PrintStream out = System.out;
    static int testCount = 0;
    static final List<Character> BIDIS = List.of(new Character[] {
            0x200E, 0x200F, 0x202A, 0x202B, 0x202C, 0x202D, 0x202E
    });
    private static final char[] hexDigits = {
            '0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    };

    // Properties that we check
    static final long PARSEFAIL   = 1L << 0;
    static final long SCHEME      = 1L << 1;
    static final long SSP         = 1L << 2;
    static final long SSP_D       = 1L << 3;      // Decoded form
    static final long OPAQUEPART  = 1L << 4;      // SSP, and URI is opaque
    static final long USERINFO    = 1L << 5;
    static final long USERINFO_D  = 1L << 6;      // Decoded form
    static final long HOST        = 1L << 7;
    static final long PORT        = 1L << 8;
    static final long REGISTRY    = 1L << 9;
    static final long REGISTRY_D  = 1L << 10;     // Decoded form
    static final long PATH        = 1L << 11;
    static final long PATH_D      = 1L << 12;     // Decoded form
    static final long QUERY       = 1L << 13;
    static final long QUERY_D     = 1L << 14;     // Decoded form
    static final long FRAGMENT    = 1L << 15;
    static final long FRAGMENT_D  = 1L << 16;     // Decoded form
    static final long TOASCII     = 1L << 17;
    static final long IDENT_STR   = 1L << 18;     // Identities
    static final long IDENT_URI1  = 1L << 19;
    static final long IDENT_URI3  = 1L << 20;
    static final long IDENT_URI5  = 1L << 21;
    static final long IDENT_URI7  = 1L << 22;
    static final long IDENT_IRI1  = 1L << 23;
    static final long TOSTRING    = 1L << 24;
    static final long TOISTRING   = 1L << 25;
    static final long RSLV        = 1L << 26;
    static final long RTVZ        = 1L << 27;
    static final long IDENT_QURI  = 1L << 28;
    static final long IDENT_BLD1  = 1L << 29;
    static final long IDENT_BLD2  = 1L << 30;
    static final long IDENT_ISTR  = 1L << 31;
    static final long TOLSTRING   = 1L << 32;
    static final long IDENT_RAWO  = 1L << 33;
    static final long IDENT_RAW5  = 1L << 34;
    static final long IDENT_RAW7  = 1L << 35;
    static final long HOST_TYPE   = 1L << 36;
    static final long DNS_HOST    = 1L << 37;
    static final long IRI_OF      = 1L << 38;
    static final long URI_OF      = 1L << 39;


    String input;
    IRI uri = null;
    IRI originalURI;
    IRI base = null;                    // Base for resolution/relativization
    String op = null;                   // Op performed if uri != originalURI
    long checked = 0;                   // Mask for checked properties
    long failed = 0;                    // Mask for failed properties
    Exception exc = null;
    IRI invariantURI = null;
    boolean checkInvariantURI;

    interface Parser {
        public IRI parse(String str) throws URISyntaxException;
    }
    interface Input {
        public String toInputString(String input, IRI iri);
    }
    private static String input(String input, IRI iri) {
        return input;
    }
    private static String iri(String input, IRI iri) {
        return iri == null ? input : iri.toString();
    }

    private Test(String s) {
        this(s, IRI::parseIRI, Test::input);
    }

    private Test(String s, Parser parser, Input input) {
        testCount++;
        this.input = s;
        try {
            uri = parser.parse(s);
        } catch (URISyntaxException x) {
            exc = x;
        } finally {
            this.input = input.toInputString(s, uri);
        }

        originalURI = uri;
    }

    static Test test(String s) {
        return new Test(s);
    }

    static Test lenient(String s) {
        return new Test(s, IRI::parseLenient, Test::iri);
    }

    private Test(String s, String u, String h, int n,
                 String p, String q, String f)
    {
        testCount++;
        try {
            uri = IRI.createHierarchical(s, u, h, n, p, q, f);
        } catch (URISyntaxException x) {
            exc = x;
            input = x.getInput();
        } catch (IllegalArgumentException x) {
            exc = x;
        }
        if (uri != null)
            input = uri.toString();
        originalURI = uri;
    }

    static Test test(String s, String u, String h, int n,
                     String p, String q, String f) {
        return new Test(s, u, h, n, p, q, f);
    }

    private Test(String s, String a,
                 String p, String q, String f)
    {
        testCount++;
        try {
            uri = IRI.createHierarchical(s, a, p, q, f);
        } catch (URISyntaxException x) {
            exc = x;
            input = x.getInput();
        } catch (IllegalArgumentException x) {
            exc = x;
        }
        if (uri != null)
            input = uri.toString();
        originalURI = uri;
    }

    static Test test(String s, String a,
                     String p, String q, String f) {
        return new Test(s, a, p, q, f);
    }

    private Test(String s, String h, String p, String f) {
        testCount++;
        try {
            uri = IRI.createHierarchical(s, h, p, f);
        } catch (URISyntaxException x) {
            exc = x;
            input = x.getInput();
        } catch (IllegalArgumentException x) {
            exc = x;
        }
        if (uri != null)
            input = uri.toString();
        originalURI = uri;
    }

    static Test test(String s, String h, String p, String f) {
        return new Test(s, h, p, f);
    }

    private Test(String s, String ssp, String f) {
        testCount++;
        try {
            int index = ssp == null ? -1 : ssp.indexOf('?');
            String query = index < 0 ? null : ssp.substring(index+1);
            String opaque = index < 0 ? ssp : ssp.substring(0, index);
            if (s == null || opaque == null || opaque.isEmpty()
                    || opaque.startsWith("/")) {
                if (opaque != null && opaque.startsWith("//")) {
                    Matcher m = Pattern.compile("^//([^/]*)((/.*)?)")
                            .matcher(opaque);
                    m.matches();
                    String auth = m.group(1);
                    String path = m.group(2);
                    uri = IRI.createHierarchical(s, auth, path, query, f);
                } else {
                    uri = IRI.createHierarchical(s, null, opaque, query, f);
                }
            } else {
                uri = IRI.createOpaque(s, opaque, query, f);
            }
        } catch (URISyntaxException x) {
            exc = x;
            input = x.getInput();
        } catch (IllegalArgumentException x) {
            exc = x;
        }
        if (uri != null)
            input = uri.toString();
        originalURI = uri;
    }

    static Test test(String s, String ssp, String f) {
        return new Test(s, ssp, f);
    }

    private Test(String s, boolean xxx) {
        testCount++;
        try {
            uri = IRI.of(s);
        } catch (IllegalArgumentException x) {
            exc = x;
        }
        if (uri != null)
            input = uri.toString();
        originalURI = uri;
    }

    static Test testCreate(String s) {
        return new Test(s, false);
    }

    boolean parsed() {
        return uri != null;
    }

    boolean resolved() {
        return base != null;
    }

    IRI uri() {
        return uri;
    }


    // Operations on Test instances
    //
    // These are short so as to make test cases compact.
    //
    //    s      Scheme
    //    sp     Scheme-specific part
    //    spd    Scheme-specific part, decoded
    //    o      Opaque part (isOpaque() && ssp matches)
    //    g      reGistry (authority matches, and host is not defined)
    //    gd     reGistry, decoded
    //    u      User info
    //    ud     User info, decoded
    //    h      Host
    //    hd     Host, decoded
    //    n      port Number
    //    p      Path
    //    pd     Path, decoded
    //    q      Query
    //    qd     Query, decoded
    //    f      Fragment
    //    fd     Fragment, decoded
    //
    //    rslv   Resolve against given base
    //    rtvz   Relativize
    //    psa    Parse server Authority
    //    norm   Normalize
    //    ta     ASCII form
    //
    //    x      Check that parse failed as expected
    //    z      End -- ensure that unchecked components are null

    private boolean check1(long prop) {
        checked |= prop;
        if (!parsed()) {
            failed |= prop;
            return false;
        }
        return true;
    }

    private void check2(String s, String ans, long prop) {
        if ((s == null) || !s.equals(ans))
            failed |= prop;
    }

    private static String escape(char c) {
        assert c < 0x80;
        return appendEscape(new StringBuilder(), (byte)c).toString();
    }

    private static String escapeSSP(String ssp) {
        return ssp == null ? null :
                ssp.replace("?", escape('?'))
                        .replace("#", escape('#'));
    }

    private static String appendSSP(IRI iri, String authority, String p, String q) {
        if (iri.isOpaque() || authority == null) {
            if (p != null) p = escapeSSP(p);
            return (p == null ? "" : p) +
                    (q == null ? "" : ("?" + q));
        } else {
            return "//" + escapeSSP(authority) +
                    (p == null ? "" : escapeSSP(p)) +
                    (q == null ? "" : ("?" + q));
        }
    }

    // simulate SSP
    // This is not strictly true as IRI::getSchemeSpecificPart()
    // would not completely decode the opaque path and
    // query when it contained %25hh triplets, but it's
    // an approximation which is good enough for the
    // purpose of this test.
    private static String getSchemeSpecificPart(IRI iri) {
        String authority = iri.getAuthority();
        String p = iri.getPath();
        String q = iri.getQuery();
        return appendSSP(iri, authority, p, q);
    }

    // simulate raw SSP
    private static String getRawSchemeSpecificPart(IRI iri) {
        String authority = iri.getRawAuthority();
        String p = iri.getRawPath();
        String q = iri.getRawQuery();
        return appendSSP(iri, authority, p, q);
    }

    // Allows to erase all previous checks in order
    // to force all components to be rechecked.
    // Useful when calling operations such as normalize or
    // resolve. Use with care though.
    // Example:
    //    test(..).s(..).h(..).p(..) // build, check (original form)
    //            .z().rst().norm()  // report, reset, normalize
    //            .s(..).h(..).p(..) // recheck (normalized form)
    //            .z()               // report again
    // should be safe enough.
    //
    Test rst() {
        // erase all checks but PARSEFAIL
        checked &= PARSEFAIL;
        return this;
    }

    Test s(String s) {
        if (check1(SCHEME)) check2(uri.getScheme(), s, SCHEME);
        return this;
    }

    Test u(String s) {
        if (check1(USERINFO)) check2(uri.getRawUserInfo(), s, USERINFO);
        return this;
    }

    Test ud(String s) {
        if (check1(USERINFO_D)) {
            check2(uri.getUserInfo(), s, USERINFO_D);
        }
        return this;
    }

    Test h(String s) {
        if (check1(HOST)) check2(uri.getRawHostString(), s, HOST);
        return this;
    }

    Test hd(String s) {
        if (check1(HOST)) check2(uri.getHostString(), s, HOST);
        return this;
    }

    Test ht(IRI.HostType type) {
        if (type != IRI.getHostType(uri.getHostString())) {
            failed |= HOST_TYPE;
        }
        if (type.isInternetName()) {
            if (type != IRI.getHostType(uri.getHost())) {
                failed |= HOST_TYPE;
            }
        } else if (IRI.HostType.None != IRI.getHostType(uri.getHost())) {
            failed |= HOST_TYPE;
        }
        return this;
    }

    Test ipv6() {
        return ht(IRI.HostType.IPv6);
    }

    Test ipv4() {
        return ht(IRI.HostType.IPv4);
    }

    Test ipvf() {
        return ht(IRI.HostType.IPvFuture);
    }

    Test regn() {
        return ht(IRI.HostType.RegName);
    }

    Test dns() {
        return ht(IRI.HostType.DNSRegName);
    }

    Test g(String s) {
        if (check1(REGISTRY)) {
            if (uri.getHostString() == null) // RFC 3986: host is reg-name
                failed |= REGISTRY;
            else {
                check2(uri.getRawAuthority(), s, REGISTRY);
            }
        }
        return this;
    }

    Test gd(String s) {
        if (check1(REGISTRY_D)) {
            if (uri.getHostString() == null) // RFC 3986: host is reg-name
                failed |= REGISTRY_D;
            else
                check2(uri.getAuthority(), s, REGISTRY_D);
        }
        return this;
    }

    Test n(int n) {
        checked |= PORT;
        if (!parsed() || (uri.getPort() != n))
            failed |= PORT;
        return this;
    }

    Test p(String s) {
        if (check1(PATH)) check2(uri.getRawPath(), s, PATH);
        return this;
    }

    Test pd(String s) {
        if (check1(PATH_D)) check2(uri.getPath(), s, PATH_D);
        return this;
    }

    Test o(String s) {
        if (check1(OPAQUEPART)) {
            if (!uri.isOpaque())
                failed |= OPAQUEPART;
            else
                check2(getSchemeSpecificPart(uri), s, OPAQUEPART);
        }
        return this;
    }

    Test sp(String s) {
        if (check1(SSP)) check2(getRawSchemeSpecificPart(uri), s, SSP);
        return this;
    }

    Test spd(String s) {
        if (check1(SSP_D)) check2(getSchemeSpecificPart(uri), s, SSP_D);
        return this;
    }

    Test q(String s) {
        if (check1(QUERY)) check2(uri.getRawQuery(), s, QUERY);
        return this;
    }

    Test qd(String s) {
        if (check1(QUERY_D)) check2(uri.getQuery(), s, QUERY_D);
        return this;
    }

    Test f(String s) {
        if (check1(FRAGMENT)) check2(uri.getRawFragment(), s, FRAGMENT);
        return this;
    }

    Test fd(String s) {
        if (check1(FRAGMENT_D)) check2(uri.getFragment(), s, FRAGMENT_D);
        return this;
    }

    Test ta(String s) {
        if (check1(TOASCII))
            check2(uri.toASCIIString(), s, TOASCII);
        return this;
    }

    Test ts(String s) {
        if (check1(TOSTRING))
            check2(uri.toString(), s, TOSTRING);
        return this;
    }

    Test ti(String s) {
        if (check1(TOISTRING))
            check2(uri.toIRIString(), s, TOISTRING);
        return this;
    }

    Test tl(String s) {
        if (check1(TOLSTRING))
            check2(uri.toLenientString(), s, TOLSTRING);
        return this;
    }

    Test x() {
        checked |= PARSEFAIL;
        if (parsed())
            failed |= PARSEFAIL;
        return this;
    }

    Test iae() {
        checked |= PARSEFAIL;
        if (parsed())
            failed |= PARSEFAIL;
        else if (!IllegalArgumentException.class.isInstance(exc)) {
            throw new RuntimeException("Expected IllegalArgumentException, got: " + exc, exc);
        }
        return this;
    }

    Test use() {
        checked |= PARSEFAIL;
        if (parsed())
            failed |= PARSEFAIL;
        else if (!URISyntaxException.class.isInstance(exc)) {
            throw new RuntimeException("Expected IllegalArgumentException, got: " + exc, exc);
        }
        return this;
    }

    Test rslv(String base) {
        return rslv(IRI.of(base));
    }

    Test rslv(IRI base) {
        if (!parsed())
            return this;
        this.base = base;
        op = "rslv";
        IRI u = uri;
        uri = null;
        try {
            this.uri = base.resolve(u);
            String up = u.normalize().getPath();
            this.invariantURI = base.relativize(uri);
            if (u.getScheme() != null ||
                    !up.startsWith("../") &&
                    !up.equals("..") &&
                    !mustExclude(base, u))
            {
                checkInvariantURI = true;
            }
        } catch (IllegalArgumentException x) {
            exc = x;
        }
        checked = 0;
        failed = 0;
        return this;
    }

    // return true if we can't guarantee that the invariant will hold.
    // tries to include as many corner cases as possible...
    //
    // Note1: This is the negation of this big condition:
    // ((!up.startsWith("/") &&
    //   (!up.isEmpty() ||
    //     (u.getAuthority() == null && (base.getQuery() == null || u.getQuery() != null))))
    // || base.getScheme() == null)
    //
    // Note2: we could widen the condition to the negation of:
    //   ((!up.startsWith("/") && !up.isEmpty()) || base.getScheme() == null)
    //   but that would exclude too many cases from testing.
    static boolean mustExclude(IRI base, IRI u) {
        assert u.getScheme() == null;
        String up = u.normalize().getPath();
        if (!up.startsWith("/") && !up.isEmpty()
                && base.getPath().isEmpty() && base.getAuthority() != null)
            return true; // if the base has an authority and no path
                         // a / will be prepended to the relative path
        if (up.startsWith("/")) return base.getScheme() != null;
        if (!up.isEmpty()) return base.getScheme() == null;
        if (u.getAuthority() != null) return base.getScheme() != null;
        return base.getQuery() != null && u.getQuery() == null;
    }

    Test norm() {
        if (!parsed())
            return this;
        op = "norm";
        uri = uri.normalize();
        return this;
    }

    Test rtvz(IRI base) {
        if (!parsed())
            return this;
        this.base = base;
        op = "rtvz";
        try {
            IRI u = uri;
            uri = base.relativize(uri);
            String up = u.normalize().getPath();
            invariantURI = base.resolve(uri);
            if (u.getScheme() != null ||
                    !up.startsWith("../")
                    && !up.equals("..")
                    && !mustExclude(base, u)
                    ) {
                checkInvariantURI = true;
            }
        } catch (IllegalArgumentException x) {
            exc = x;
        }
        checked = 0;
        failed = 0;
        return this;
    }

//    Test psa() {
//        try {
//            uri.parseServerAuthority();
//        } catch (URISyntaxException x) {
//            exc = x;
//            uri = null;
//        }
//        checked = 0;
//        failed = 0;
//        return this;
//    }

    Test psa() {
        return this;
    }

    private void checkEmpty(String s, long prop) {
        if (((checked & prop) == 0) && (s != null))
            failed |= prop;
    }

    // Check identity for the seven-argument IRI constructor
    //
    void checkURI7() {
        // Only works on hierarchical URIs
        if (uri.isOpaque())
            return;
        // Not true if decoding getters return %xx triplets
        // that could be further decoded. Those will be encoded
        // as %25xx sequences in the IRI string
        if (uri.toIRIString().matches(".*%25[0-9a-bA-B]{2}.*")) {
            // out.println("Skipping IDENT_URI5 for: " + uri.toString());
            return;
        }
        try {
            IRI u2 = IRI.createHierarchical(uri.getScheme(), uri.getUserInfo(),
                             uri.getHostString(), uri.getPort(), uri.getPath(),
                             uri.getQuery(), uri.getFragment());
            if (!uri.equals(u2))
                failed |= IDENT_URI7;
        } catch (URISyntaxException x) {
            if (exc == null) exc = x;
            failed |= IDENT_URI7;
        }
    }

    // Check identity for the five-argument IRI constructor
    //
    void checkURI5() {
        // Only works on hierarchical URIs
        if (uri.isOpaque())
            return;
        // Not true if decoding getters return %xx triplets
        // that could be further decoded. Those will be encoded
        // as %25xx sequences in the IRI string
        if (uri.toIRIString().matches(".*%25[0-9a-bA-B]{2}.*")) {
            // out.println("Skipping IDENT_URI5 for: " + uri.toString());
            return;
        }
        try {
            IRI u2 = IRI.createHierarchical(uri.getScheme(), uri.getAuthority(),
                             uri.getPath(), uri.getQuery(), uri.getFragment());
            if (!uri.equals(u2))
                failed |= IDENT_URI5;
        } catch (URISyntaxException x) {
            if (exc == null) exc = x;
            failed |= IDENT_URI5;
        }
    }

    // Check identity for the three-argument IRI constructor
    //
    void checkURI3() {
        try {
            IRI u2 = uri.isOpaque()
                    ? IRI.createOpaque(uri.getScheme(),
                         uri.getRawPath(),
                         uri.getRawQuery(),
                         uri.getRawFragment())
                    : IRI.createHierarchical(uri.getScheme(),
                         uri.getAuthority(),
                         uri.getRawPath(),
                         uri.getRawQuery(),
                         uri.getRawFragment());
            if (!uri.equals(u2)) {
                out.println("IDENT-URI3 failed \"" + uri +"\" != \"" + u2 + "\"");
                failed |= IDENT_URI3;
            }
        } catch (URISyntaxException x) {
            out.println("IDENT-URI3 failed for \"" + uri +"\": " + x);
            if (exc == null) exc = x;
            failed |= IDENT_URI3;
        }
    }

    void conversions() {
        if (uri == null) {
            try {
                IRI.of(uri);
                failed |= IRI_OF;
            } catch (NullPointerException x) {
                // OK
            }
        } else {
            try {
                if (IRI.of(uri) != uri) {
                    failed |= IRI_OF;
                }
            } catch (Exception x) {
                if (exc == null) exc = x;
                failed |= IRI_OF;
            }
        }

        /*if (uri == null) {
            //
        } else {
            boolean expectFail = !isURI(uri);
            try {
                if (expectFail) {
                    failed |= URI_OF;
                }
            } catch (Exception x) {
                if (!expectFail ||
                        !(x instanceof IllegalArgumentException)) {
                    if (exc == null) exc = x;
                    failed |= URI_OF;
                }
            }
        }*/
    }

    private static boolean isURI(IRI uri) {
        String s = uri.getScheme();
        String ra = uri.getRawAuthority();
        String rp = uri.getRawPath();
        String rq = uri.getRawQuery();
        String rf = uri.getRawFragment();
        if (s != null && (ra == null || ra.isEmpty())
                && (rp == null || rp.isEmpty())
                && rq == null && rf == null) {
            // "s:" or "s://" can't be converted
            return false;
        }
        if (s == null && (ra != null && ra.isEmpty())
                && (rp == null || rp.isEmpty())
                && rq == null && rf == null) {
            // "//" cannot be converted
            return false;
        }
        if (":".equals(ra) && (rp == null || rp.isEmpty())) {
            // s://: would parse with ':' as a reg-name,
            // but IRI.toURI() removes the superfluous ':'
            // and s:// or // do not parse.
            return false;
        }
        String h = uri.getRawHostString();
        // IPvFuture won't parse as a URI
        if (IRI.getHostType(h) == IRI.HostType.IPvFuture) {
            return false;
        }
        return true;
    }

    void checkHostType() {

        // No host means all accessors should return null.
        if (IRI.getHostType(uri.getRawHostString()) == IRI.HostType.None) {
            if (IRI.getHostType(uri.getHostString()) != IRI.HostType.None) {
                failed |= DNS_HOST;
            }
            if (IRI.getHostType(uri.getHost()) != IRI.HostType.None) {
                failed |= DNS_HOST;
            }
        }

        // If the raw host parses as an internet name then the others
        // should too.
        if (IRI.getHostType(uri.getRawHostString()).isInternetName()) {
            if (!IRI.getHostType(uri.getHostString()).isInternetName()) {
                failed |= DNS_HOST;
            }
            if (!IRI.getHostType(uri.getHost()).isInternetName()) {
                failed |= DNS_HOST;
            }
            // In addition, raw host string, host string, and host
            // should all be equal.
            if (!uri.getRawHostString().equals(uri.getHostString())) {
                failed |= DNS_HOST;
            }
            if (!uri.getRawHostString().equals(uri.getHost())) {
                failed |= DNS_HOST;
            }
        }

        // if host is null then neither raw host string nor host string
        // should parse as an internet name.
        if (uri.getHost() == null) {
            if (uri.getRawHostString() != null) {
                if (IRI.getHostType(uri.getRawHostString()).isInternetName()) {
                    failed |= DNS_HOST;
                }
                if (IRI.getHostType(uri.getHostString()).isInternetName()) {
                    failed |= DNS_HOST;
                }
            }
        } else {
            // if host is non null then it must be an internet name.
            if (!IRI.getHostType(uri.getHost()).isInternetName()) {
                failed |= DNS_HOST;
            }
            // if host is non null then the host string must be an
            // internet name too (though the raw host string might not).
            if (!IRI.getHostType(uri.getHostString()).isInternetName()) {
                failed |= DNS_HOST;
            }
            // if host is non null then it should be equal to the
            // host string
            if (!uri.getHost().equals(uri.getHostString())) {
                failed |= DNS_HOST;
            }
        }
    }

    // Check all identities mentioned in the IRI class specification
    // (and some more)
    void checkIdentities() {
        if (input != null) {
            if (!uri.toString().equals(input))
                failed |= IDENT_STR;
        }
        try {
            if (!(IRI.parseIRI(uri.toString())).equals(uri))
                failed |= IDENT_URI1;
        } catch (URISyntaxException x) {
            if (exc == null) exc = x;
            failed |= IDENT_URI1;
        }

        try {
            if (!(IRI.parseIRI(uri.toIRIString())).equals(uri))
                failed |= IDENT_IRI1;
        } catch (URISyntaxException x) {
            if (exc == null) exc = x;
            failed |= IDENT_IRI1;
        }

        try {
            if (!(uri.with(0).build().equals(uri)))
                failed |= IDENT_BLD1;
        } catch (URISyntaxException x) {
            if (exc == null) exc = x;
            failed |= IDENT_BLD1;
        }

        try {
            if (!(uri.with(IRI.Builder.QUOTE_ENCODED_CAPABILITY)
                    .build().equals(uri)))
                failed |= IDENT_BLD2;
        } catch (URISyntaxException x) {
            if (exc == null) exc = x;
            failed |= IDENT_BLD2;
        }

        try {
            if (uri.isOpaque() &&
                    !uri.equals(IRI.createOpaque(uri.getScheme(),
                            uri.getRawPath(), uri.getRawQuery(),
                            uri.getRawFragment()))) {
                failed |= IDENT_RAWO;
            }
        } catch (URISyntaxException x) {
            if (exc == null) exc = x;
            failed |= IDENT_RAWO;
        }

        try {
            if (!uri.isOpaque() &&
                    !uri.equals(IRI.createHierarchical(uri.getScheme(),
                            uri.getRawAuthority(),
                            uri.getRawPath(), uri.getRawQuery(),
                            uri.getRawFragment()))) {
                failed |= IDENT_RAW5;
            }
        } catch (URISyntaxException x) {
            if (exc == null) exc = x;
            failed |= IDENT_RAW5;
        }

        try {
            if (!uri.isOpaque() &&
                    !uri.equals(IRI.createHierarchical(uri.getScheme(),
                            uri.getRawUserInfo(), uri.getRawHostString(), uri.getPort(),
                            uri.getRawPath(), uri.getRawQuery(),
                            uri.getRawFragment()))) {
                failed |= IDENT_RAW7;
            }
        } catch (URISyntaxException x) {
            if (exc == null) exc = x;
            failed |= IDENT_RAW7;
        }

        try {
            String iriStr1 = uri.toIRIString();
            String iriStr2 = IRI.parseIRI(iriStr1).toIRIString();
            if (!iriStr1.equals(iriStr2)) {
                failed |= IDENT_ISTR;
                System.out.println(String.format(
                        "*** IRI Strings differ for \"%s\": " +
                        "\n\t original: \"%s\"" +
                        "\n\t  rebuilt: \"%s\"", uri, iriStr1, iriStr2));
            }
        } catch (URISyntaxException x) {
            if (exc == null) exc = x;
            failed |= IDENT_ISTR;
        }

        // Verifies that an IRI can be safely embedded into a query and subsequently
        // extracted from the query string if it is quoted first.
        try {
            IRI otherU = IRI.createHierarchical("s", "h", "/p",
                    "uri=" + IRI.quoteEncodedOctets(uri.toString()), "f");
            IRI otherI = IRI.createHierarchical("s", "h", "/p",
                    "uri=" + IRI.quoteEncodedOctets(uri.toIRIString()), "f");
            IRI otherA = IRI.createHierarchical("s", "h", "/p",
                    "uri=" + IRI.quoteEncodedOctets(uri.toASCIIString()), "f");
            if (!uri.equals(IRI.parseIRI(otherU.getQuery().substring(4)))) {
                out.println("query-ident failed for uri.toString()");
                failed |= IDENT_QURI;
            }
            if (!uri.equals(IRI.parseIRI(otherI.getQuery().substring(4)))) {
                out.println("query-ident failed for uri.toIRIString()");
                failed |= IDENT_QURI;
            }
            if (!uri.equals(IRI.parseIRI(otherA.getQuery().substring(4)))) {
                out.println("query-ident failed for uri.toASCIIString()");
                failed |= IDENT_QURI;
            }
        } catch (URISyntaxException x) {
            if (exc == null) exc = x;
            failed |= IDENT_QURI;
        }

        // Remaining identities fail if "//" given but authority is undefined
        if ((uri.getAuthority() == null)
            && (getSchemeSpecificPart(uri) != null)
            && (getSchemeSpecificPart(uri).startsWith("///")
                || getSchemeSpecificPart(uri).startsWith("//?")
                || getSchemeSpecificPart(uri).equals("//")))
            return;

        checkURI3();
        checkURI5();
        checkURI7();
    }

    // Check identities, check that unchecked component properties are not
    // defined, and report any failures
    //
    Test z() {
        conversions();

        if (!parsed()) {
            report();
            return this;
        }

        if (op == null)
            checkIdentities();

        checkHostType();

        // Check that unchecked components are undefined
        checkEmpty(uri.getScheme(), SCHEME);
        checkEmpty(uri.getUserInfo(), USERINFO);
        checkEmpty(uri.getHostString(), HOST);
        if (((checked & PORT) == 0) && (uri.getPort() != -1)) failed |= PORT;
        checkEmpty(uri.getPath(), PATH);
        checkEmpty(uri.getQuery(), QUERY);
        checkEmpty(uri.getFragment(), FRAGMENT);

        if (invariantURI != null) {
            if (checkInvariantURI && !normeq(invariantURI, originalURI)) {
                switch(op) {
                    case "rtvz":
                        failed |= RTVZ; break;
                    case "rslv":
                        failed |= RSLV ; break;
                    default: break;
                }
            }
        }

        // Report failures
        report();
        return this;
    }

    static boolean normeq(IRI u, IRI v) {
        return u.normalize().equals(v.normalize());
    }

    // Summarization and reporting

    static void header(String s) {
        out.println();
        out.println();
        out.println("-- " + s + " --");
    }

    static void show(String prefix, URISyntaxException x) {
        out.println(uquote(x.getInput()));
        if (x.getIndex() >= 0) {
            for (int i = 0; i < x.getIndex(); i++) {
                if (x.getInput().charAt(i) >= '\u0080')
                    out.print("      ");        // Skip over \u1234
                else
                    out.print(" ");
            }
            out.println("^");
        }
        out.println(prefix + ": " + x.getReason());
    }

    private void summarize() {
        out.println();
        StringBuffer sb = new StringBuffer();
        if (input != null && input.length() == 0)
            sb.append("\"\"");
        else if (input != null)
            sb.append(input);
        else if (input == null)
            sb.append("create failed");
        if (base != null) {
            sb.append(" ");
            sb.append(base);
        }
        if (!parsed()) {
            String s = (((checked & PARSEFAIL) != 0)
                        ? "Correct exception" : "UNEXPECTED EXCEPTION");
            if (exc instanceof URISyntaxException) {
                show(s, (URISyntaxException) exc);
                if ((checked & PARSEFAIL) == 0)
                    exc.printStackTrace(out);
            } else if (exc instanceof IllegalArgumentException) {
                out.println(s + ": " + exc);
                if ((checked & PARSEFAIL) == 0)
                    exc.printStackTrace(out);
            } else {
                out.println(uquote(sb.toString()));
                out.print(s + ": ");
                exc.printStackTrace(out);
            }
        } else {
            if (op != null) {
                sb.append(" ");
                sb.append(op);
                sb.append(" --> ");
                sb.append(uri);
                if (invariantURI != null) {
                    sb.append(" [ ");
                    sb.append(originalURI.normalize());
                    sb.append(" <--> ");
                    sb.append(invariantURI.normalize());
                    sb.append(" : ");
                    sb.append(normeq(invariantURI, originalURI));
                    sb.append(" ]");
                }
            }
            Throwable opexc = null;
            if (exc != null && (failed & (RTVZ | RSLV)) != 0) {
                sb.append(" ").append(op).append(" failed: ").append(exc);
                opexc = exc;
            }
            out.println(uquote(sb.toString()));
            if (opexc != null) {
                opexc.printStackTrace(out);
            }
        }
    }

    public static String uquote(String str) {
        if (str == null)
            return str;
        StringBuffer sb = new StringBuffer();
        int n = str.length();
        for (int i = 0; i < n; i++) {
            char c = str.charAt(i);
            if ((c >= ' ') && (c < 0x7f)) {
                sb.append(c);
                continue;
            }
            sb.append("\\u");
            String s = Integer.toHexString(c).toUpperCase();
            while (s.length() < 4)
                s = "0" + s;
            sb.append(s);
        }
        return sb.toString();
    }

    private static StringBuilder appendEscape(StringBuilder sb, byte b) {
        sb.append('%');
        sb.append(hexDigits[(b >> 4) & 0x0f]);
        sb.append(hexDigits[(b >> 0) & 0x0f]);
        return sb;
    }

    private static StringBuilder appendEscape(StringBuilder sb, char... c) {
        for (byte b : String.valueOf(c).getBytes(StandardCharsets.UTF_8)) {
            sb = appendEscape(sb, b);
        }
        return sb;
    }

    static void show(String n, String v) {
        out.println("  " + n
                    + "          = ".substring(n.length())
                    + uquote(v));
    }

    static void show(String n, String v, String vd) {
        if ((v == null) || v.equals(vd))
            show(n, v);
        else {
            out.println("  " + n
                        + "          = ".substring(n.length())
                        + uquote(v)
                        + " = " + uquote(vd));
        }
    }

    public static void show(IRI u) {
        show("opaque", "" + u.isOpaque());
        show("scheme", u.getScheme());
        show("ssp", getRawSchemeSpecificPart(u), getSchemeSpecificPart(u));
        show("authority", u.getRawAuthority(), u.getAuthority());
        show("userinfo", u.getRawUserInfo(), u.getUserInfo());
        show("host", u.getRawHostString(), u.getHostString());
        show("dns-host", u.getHost());
        show("host-type", u.getHostType(u.getHostString()).name());
        show("port", "" + u.getPort());
        show("path", u.getRawPath(), u.getPath());
        show("query", u.getRawQuery(), u.getQuery());
        show("fragment", u.getRawFragment(), u.getFragment());
        if (!u.toString().equals(u.toASCIIString())) {
            show("toascii", u.toASCIIString());
        }
        if (!u.toString().equals(u.toIRIString())) {
            show("toiri", u.toIRIString());
        }
        if (!u.toString().equals(u.toLenientString())) {
            show("tolenient", u.toLenientString());
        }
    }

    private void report() {
        summarize();
        if (failed == 0) return;
        StringBuffer sb = new StringBuffer();
        sb.append("FAIL:");
        if ((failed & PARSEFAIL) != 0) sb.append(" parsefail");
        if ((failed & SCHEME) != 0) sb.append(" scheme");
        if ((failed & SSP) != 0) sb.append(" ssp");
        if ((failed & SSP_D) != 0) sb.append(" sspd");
        if ((failed & OPAQUEPART) != 0) sb.append(" opaquepart");
        if ((failed & USERINFO) != 0) sb.append(" userinfo");
        if ((failed & USERINFO_D) != 0) sb.append(" userinfod");
        if ((failed & HOST) != 0) sb.append(" host");
        if ((failed & PORT) != 0) sb.append(" port");
        if ((failed & REGISTRY) != 0) sb.append(" registry");
        if ((failed & REGISTRY_D) != 0) sb.append(" registryd");
        if ((failed & PATH) != 0) sb.append(" path");
        if ((failed & PATH_D) != 0) sb.append(" pathd");
        if ((failed & QUERY) != 0) sb.append(" query");
        if ((failed & QUERY_D) != 0) sb.append(" queryd");
        if ((failed & FRAGMENT) != 0) sb.append(" fragment");
        if ((failed & FRAGMENT_D) != 0) sb.append(" fragmentd");
        if ((failed & TOASCII) != 0) sb.append(" toascii");
        if ((failed & IDENT_STR) != 0) sb.append(" ident-str");
        if ((failed & IDENT_URI1) != 0) sb.append(" ident-uri1");
        if ((failed & IDENT_IRI1) != 0) sb.append(" ident-iri1");
        if ((failed & IDENT_URI3) != 0) sb.append(" ident-uri3");
        if ((failed & IDENT_URI5) != 0) sb.append(" ident-uri5");
        if ((failed & IDENT_URI7) != 0) sb.append(" ident-uri7");
        if ((failed & IDENT_QURI) != 0) sb.append(" ident-query");
        if ((failed & IDENT_BLD1) != 0) sb.append(" ident-build1");
        if ((failed & IDENT_BLD2) != 0) sb.append(" ident-build2");
        if ((failed & IDENT_ISTR) != 0) sb.append(" ident-istring");
        if ((failed & IDENT_RAWO) != 0) sb.append(" ident-raw-opaque");
        if ((failed & IDENT_RAW5) != 0) sb.append(" ident-raw-5");
        if ((failed & IDENT_RAW7) != 0) sb.append(" ident-raw-7");
        if ((failed & TOSTRING) != 0) sb.append(" tostring");
        if ((failed & TOISTRING) != 0) sb.append(" iristring");
        if ((failed & TOLSTRING) != 0) sb.append(" lenient");
        if ((failed & RTVZ) != 0) sb.append(" relativize");
        if ((failed & RSLV) != 0) sb.append(" resolve");
        if ((failed & HOST_TYPE) != 0) sb.append(" host-type");
        if ((failed & DNS_HOST) != 0) sb.append(" dns-host");
        if ((failed & IRI_OF) != 0) sb.append(" IRI.of");
        if ((failed & URI_OF) != 0) sb.append(" URI.of");
        out.println(sb);
        if (uri != null) show(uri);
        throw new RuntimeException("Test failed");
    }



    // -- Tests --

    static void rfc2396() {


        header("RFC2396: Basic examples");

        test("ftp://ftp.is.co.za/rfc/rfc1808.txt")
            .s("ftp").h("ftp.is.co.za").p("/rfc/rfc1808.txt").z();

        test("http://www.math.uio.no/faq/compression-faq/part1.html")
            .s("http").h("www.math.uio.no").p("/faq/compression-faq/part1.html").z();

        test("mailto:mduerst@ifi.unizh.ch")
                .s("mailto")
                .o("mduerst@ifi.unizh.ch")
                .p("mduerst@ifi.unizh.ch").z();

        test("news:comp.infosystems.www.servers.unix")
                .s("news")
                .o("comp.infosystems.www.servers.unix")
                .p("comp.infosystems.www.servers.unix").z();

        test("telnet://melvyl.ucop.edu/")
            .s("telnet").h("melvyl.ucop.edu").p("/").z();

        test("http://www.w3.org/Addressing/")
            .s("http").h("www.w3.org").p("/Addressing/").z();

        test("ftp://ds.internic.net/rfc/")
            .s("ftp").h("ds.internic.net").p("/rfc/").z();

        test("http://www.ics.uci.edu/pub/ietf/uri/historical.html#WARNING")
            .s("http").h("www.ics.uci.edu").p("/pub/ietf/uri/historical.html")
            .f("WARNING").z();

        test("http://www.ics.uci.edu/pub/ietf/uri/#Related")
            .s("http").h("www.ics.uci.edu").p("/pub/ietf/uri/")
            .f("Related").z();


        header("RFC2396: Normal relative-URI examples (appendix C)");

        IRI base = (test("http://a/b/c/d;p?q")
                    .s("http").h("a").p("/b/c/d;p").q("q").z().uri());

        // g:h       g:h
        test("g:h")
            .s("g").o("h").p("h").z()
            .rslv(base).s("g").o("h").p("h").z();

        // g         http://a/b/c/g
        test("g")
            .p("g").z()
            .rslv(base).s("http").h("a").p("/b/c/g").z();

        // ./g       http://a/b/c/g
        test("./g")
            .p("./g").z()
            .rslv(base).s("http").h("a").p("/b/c/g").z();

        // g/        http://a/b/c/g/
        test("g/")
            .p("g/").z()
            .rslv(base).s("http").h("a").p("/b/c/g/").z();

        // /g        http://a/g
        test("/g")
            .p("/g").z()
            .rslv(base).s("http").h("a").p("/g").z();

        // //g/x/y       http://g/x/y
        test("//g/x/y")
                .h("g").p("/x/y").z()
                .rslv(base).s("http").h("g").p("/x/y").z();

        // //g       http://g
        test("//g")
                .h("g").p("").z()
                .rslv(IRI.of("//a")).h("g").p("").z();

        // //g       http://g
        test("//g")
                .h("g").p("").z()
                .rslv(IRI.of("//a?q")).h("g").p("").z();

        // //g?       http://g?
        test("//g?")
                .h("g").q("").p("").z()
                .rslv(IRI.of("//a?q")).h("g").q("").p("").z();

        // //g       http://g
        test("//g")
            .h("g").p("").z()
            .rslv(base).s("http").h("g").p("").z();

        // ?y        http://a/b/c/d;p?y      as per RFC3986
        test("?y")
            .p("").q("y").z()
            .rslv(base).s("http").h("a").p("/b/c/d;p").q("y").z();

        // #?y        http://a/b/c/d;p?y      as per RFC3986
        test("?y#")
                .p("").q("y").f("").z()
                .rslv(base).s("http").h("a").p("/b/c/d;p").q("y").f("").z();

        // #?y        http://a/b/c/d;p?y      as per RFC3986
        test("//a/b/c/?y#")
                .p("/b/c/").h("a").q("y").f("").z()
                .rtvz(IRI.of("//a/b/c/")).p("").q("y").f("").z();

        // g?y       http://a/b/c/g?y
        test("g?y")
            .p("g").q("y").z()
            .rslv(base).s("http").h("a").p("/b/c/g").q("y").z();

        // #s        (current document)#s
        // DEVIATION: Lone fragment parses as relative URI with empty path
        test("#s")
            .p("").f("s").z()
            .rslv(base).s("http").h("a").p("/b/c/d;p").f("s").q("q").z();

        test("#s")
                .p("").f("s").z()
                .rslv(IRI.of("a/b/c#f")).p("a/b/c").f("s").z();

        test("#s")
                .p("").f("s").z()
                .rslv(IRI.of("a/b/c?q#f")).p("a/b/c").f("s").q("q").z();

        // g#s       http://a/b/c/g#s
        test("g#s")
            .p("g").f("s").z()
            .rslv(base).s("http").h("a").p("/b/c/g").f("s").z();

        // g?y#s     http://a/b/c/g?y#s
        test("g?y#s")
            .p("g").q("y").f("s").z()
            .rslv(base).s("http").h("a").p("/b/c/g").q("y").f("s").z();

        // ;x        http://a/b/c/;x
        test(";x")
            .p(";x").z()
            .rslv(base).s("http").h("a").p("/b/c/;x").z();

        // g;x       http://a/b/c/g;x
        test("g;x")
            .p("g;x").z()
            .rslv(base).s("http").h("a").p("/b/c/g;x").z();

        // g;x?y#s   http://a/b/c/g;x?y#s
        test("g;x?y#s")
            .p("g;x").q("y").f("s").z()
            .rslv(base).s("http").h("a").p("/b/c/g;x").q("y").f("s").z();

        // .         http://a/b/c/
        test(".")
            .p(".").z()
            .rslv(base).s("http").h("a").p("/b/c/").z();

        // ./        http://a/b/c/
        test("./")
            .p("./").z()
            .rslv(base).s("http").h("a").p("/b/c/").z();

        // ..        http://a/b/
        test("..")
            .p("..").z()
            .rslv(base).s("http").h("a").p("/b/").z();

        // ../       http://a/b/
        test("../")
            .p("../").z()
            .rslv(base).s("http").h("a").p("/b/").z();

        // ../g      http://a/b/g
        test("../g")
            .p("../g").z()
            .rslv(base).s("http").h("a").p("/b/g").z();

        // ../..     http://a/
        test("../..")
            .p("../..").z()
            .rslv(base).s("http").h("a").p("/").z();

        // ../../    http://a/
        test("../../")
            .p("../../").z()
            .rslv(base).s("http").h("a").p("/").z();

        // ../../g   http://a/g
        test("../../g")
            .p("../../g").z()
            .rslv(base).s("http").h("a").p("/g").z();


        header("RFC2396: Abnormal relative-URI examples (appendix C)");

        // g.            =  http://a/b/c/g.
        test("g.")
            .p("g.").z()
            .rslv(base).s("http").h("a").p("/b/c/g.").z();

        // .g            =  http://a/b/c/.g
        test(".g")
            .p(".g").z()
            .rslv(base).s("http").h("a").p("/b/c/.g").z();

        // g..           =  http://a/b/c/g..
        test("g..")
            .p("g..").z()
            .rslv(base).s("http").h("a").p("/b/c/g..").z();

        // ..g           =  http://a/b/c/..g
        test("..g")
            .p("..g").z()
            .rslv(base).s("http").h("a").p("/b/c/..g").z();

        // ./../g        =  http://a/b/g
        test("./../g")
            .p("./../g").z()
            .rslv(base).s("http").h("a").p("/b/g").z();

        // ./g/.         =  http://a/b/c/g/
        test("./g/.")
            .p("./g/.").z()
            .rslv(base).s("http").h("a").p("/b/c/g/").z();

        // g/./h         =  http://a/b/c/g/h
        test("g/./h")
            .p("g/./h").z()
            .rslv(base).s("http").h("a").p("/b/c/g/h").z();

        // g/../h        =  http://a/b/c/h
        test("g/../h")
            .p("g/../h").z()
            .rslv(base).s("http").h("a").p("/b/c/h").z();

        // g;x=1/./y     =  http://a/b/c/g;x=1/y
        test("g;x=1/./y")
            .p("g;x=1/./y").z()
            .rslv(base).s("http").h("a").p("/b/c/g;x=1/y").z();

        // g;x=1/../y    =  http://a/b/c/y
        test("g;x=1/../y")
            .p("g;x=1/../y").z()
            .rslv(base).s("http").h("a").p("/b/c/y").z();

        // g?y/./x       =  http://a/b/c/g?y/./x
        test("g?y/./x")
            .p("g").q("y/./x").z()
            .rslv(base).s("http").h("a").p("/b/c/g").q("y/./x").z();

        // g?y/../x      =  http://a/b/c/g?y/../x
        test("g?y/../x")
            .p("g").q("y/../x").z()
            .rslv(base).s("http").h("a").p("/b/c/g").q("y/../x").z();

        // g#s/./x       =  http://a/b/c/g#s/./x
        test("g#s/./x")
            .p("g").f("s/./x").z()
            .rslv(base).s("http").h("a").p("/b/c/g").f("s/./x").z();

        // g#s/../x      =  http://a/b/c/g#s/../x
        test("g#s/../x")
            .p("g").f("s/../x").z()
            .rslv(base).s("http").h("a").p("/b/c/g").f("s/../x").z();

        // http:g        =  http:g
        test("http:g")
            .s("http").o("g").p("g").z()
            .rslv(base).s("http").o("g").p("g").z();

    }

    // most of the cases are covered by rfc2396(), this one
    // only covers what has been modified in RFC3986 and
    // RFC3987
    static void rfc3986() throws URISyntaxException {
        header("RFC3986: Basic examples");
        IRI base = (test("http://a/b/c/d;p?q")
                .s("http").h("a").p("/b/c/d;p").q("q").z().uri());
        // ?y           = http://a/b/c/d;p?y
        test("?y").p("").q("y").z()
                .rslv(base)
                .s("http").h("a").p("/b/c/d;p").q("y").z();

        // ""           = http://a/b/c/d;p?q
        test("").p("").z()
                .rslv(base)
                .s("http").h("a").p("/b/c/d;p").q("q").z();

        // /./g         = http://a/g
        test("/./g").p("/./g").z()
                .rslv(base)
                .s("http").h("a").p("/g").z();

        header("RFC3986: Abnormal relative-URI examples (appendix C) - diff from RFC 2396");

        // /./g          =  http://a/g
        test("/./g")
                .p("/./g").z()
                .rslv(base).s("http").h("a").p("/g").z();


        // ../../../g    =  http://a/g
        test("../../../g")
                .p("../../../g").z()
                .rslv(base).s("http").h("a").p("/g").z();

        // ../../../../g =  http://a/g
        test("../../../../g")
                .p("../../../../g").z()
                .rslv(base).s("http").h("a").p("/g").z();


        // /../g         =  http://a/g
        test("/../g")
                .p("/../g").z()
                .rslv(base).s("http").h("a").p("/g").z();

        test("/../../g")
                .p("/../../g").z()
                .rslv(base).s("http").h("a").p("/g").z();

        header("RFC3986: scheme only");

        // http:        = http:
        //test("http:").s("http").p("").z()
        //        .rslv(base)
        //        .s("http").p("").z();

        header("RFC3986: opaque URI with query and fragment");

        // 6350321
        test("host:opa:que?query#fragment")
                .s("host").o("opa:que?query")
                .p("opa:que").q("query").f("fragment").z();

        header("RFC3987: URI -> IRI mapping");
        IRI uri = null;

        // an IRI with a char excluded from iunreserved in it
        test("http://foo.com/invalid\ufffdreplacement")
                .x().z();

        // an IRI with a char excluded from iunreserved in it
        IRI u1 = test("http://foo.com/invalid%ef%bf%bdreplacement")
                .s("http").h("foo.com")
                .p("/invalid%ef%bf%bdreplacement")
                .pd("/invalid\ufffdreplacement")
                .ti("http://foo.com/invalid%EF%BF%BDreplacement")
                .ts("http://foo.com/invalid%ef%bf%bdreplacement")
                .z().uri();

        // an IRI with a char excluded from iunreserved in it
        IRI u2 = test("http", "foo.com", "/invalid\ufffdreplacement", null, null)
                .s("http").h("foo.com")
                .p("/invalid%EF%BF%BDreplacement")
                .pd("/invalid\ufffdreplacement")
                .ti("http://foo.com/invalid%EF%BF%BDreplacement")
                .ts("http://foo.com/invalid%EF%BF%BDreplacement")
                .z().uri();
        eq(u1, u2);

        // an IRI with invalid %C0%AF sequence
        // verify it's not decoded as / - see RFC 3987 section 8
        // Note: U+C0AF is a valid character, but it encodes in UTF-8 with 3 bytes
        //       into %EC%82%AF - not as %C0%AF
        u1 = test("http", "foo.com", "/with%C0%AF0xC0AF%EC%82%AF\uc0af", null)
                .s("http").h("foo.com")
                .p("/with%C0%AF0xC0AF%EC%82%AF\uc0af")
                .pd("/with%C0%AF0xC0AF\uc0af\uc0af")
                .ti("http://foo.com/with%C0%AF0xC0AF\uc0af\uc0af")
                .ta("http://foo.com/with%C0%AF0xC0AF%EC%82%AF%EC%82%AF")
                .ts("http://foo.com/with%C0%AF0xC0AF%EC%82%AF\uc0af")
                .z().uri();
        u2 = test("http://foo.com/with%C0%AF0xC0AF%EC%82%AF\uc0af")
                .s("http").h("foo.com")
                .p("/with%C0%AF0xC0AF%EC%82%AF\uc0af")
                .pd("/with%C0%AF0xC0AF\uc0af\uc0af")
                .ti("http://foo.com/with%C0%AF0xC0AF\uc0af\uc0af")
                .z().uri();
        eq(u2, u1);

        // legal UTF-8 sequence mixed with bidi formatting character
        uri = test("http://www.example.org/D%C3%BC%E2%80%AE%C3%BCrst")
                .s("http").h("www.example.org").p("/D%C3%BC%E2%80%AE%C3%BCrst")
                .pd("/D\u00FC%E2%80%AE\u00FCrst").z().uri();
        eq(uri.toIRIString(), "http://www.example.org/D\u00FC%E2%80%AE\u00FCrst");

        // strictly legal UTF-8 sequence mixed with not strictly legal UTF-8 sequence
        uri = test("http://www.example.org/D%C3%BC%FC%C3%BCrst")
                .s("http").h("www.example.org").p("/D%C3%BC%FC%C3%BCrst")
                .pd("/D\u00FC%FC\u00FCrst").z().uri();
        eq(uri.toIRIString(), "http://www.example.org/D\u00FC%FC\u00FCrst");

        // toIRIString() involves decoding operation, which is quite dangerous.
        // Especially it should not decode illegal character in corresponding
        // components, e.g. character '^' (ASCII value 0x5E) is illegal in path
        uri = test("http", "www.example.org", "/D%C3%BCrst^", null, null)
                .s("http").h("www.example.org").p("/D%C3%BCrst%5E").z().uri();
        eq(uri.toIRIString(), "http://www.example.org/D\u00FCrst%5E");

        // an IRI with a legal UTF-8 sequence in it
        uri = test("http://www.example.org/D%C3%BCrst")
                .s("http").h("www.example.org").p("/D%C3%BCrst").z().uri();
        eq(uri.toIRIString(), "http://www.example.org/D\u00FCrst");

        // an IRI with a not strictly legal UTF-8 sequence in it
        uri = test("http://www.example.org/D%FCrst")
                .s("http").h("www.example.org")
                .p("/D%FCrst").pd("/D%FCrst")
                .z().uri();
        eq(uri.toIRIString(), "http://www.example.org/D%FCrst");

        // an IRI with a bidi formatting character in it
        // in this case, the bidi formatting character is RLO
        uri = test("http://xn--99zt52a.example.org/%e2%80%ae")
                .s("http").h("xn--99zt52a.example.org").p("/%e2%80%ae").z().uri();
        eq(uri.toIRIString(), "http://xn--99zt52a.example.org/%E2%80%AE");

        // unencoded bidi is not allowed
        test("http://xn--99zt52a.example.org/a\u202eb").x().z();
        test("http", "xn--99zt52a.example.org", "/a\u202eb", null)
                .s("http").h("xn--99zt52a.example.org")
                .p("/a%E2%80%AEb")
                .pd("/a%E2%80%AEb").z();
        test("http", "//xn--99zt52a.example.org/a\u202eb", null)
                .s("http").h("xn--99zt52a.example.org")
                .p("/a%E2%80%AEb")
                .pd("/a%E2%80%AEb").z();
        test("http", "xn--99zt52a.example.org", "/a\u202eb", null, null)
                .s("http").h("xn--99zt52a.example.org")
                .p("/a%E2%80%AEb")
                .pd("/a%E2%80%AEb").z();
        test("http", null, "xn--99zt52a.example.org", -1, "/a\u202eb", null, null)
                .s("http").h("xn--99zt52a.example.org")
                .p("/a%E2%80%AEb")
                .pd("/a%E2%80%AEb").z();

        // bug 6345502
        test("scheme://userInfo@:5555/home/root?a=b#ABC")
                .s("scheme").u("userInfo").h("").n(5555)
                .p("/home/root").q("a=b").f("ABC").z();

        // bug 6363889
        test("s://@/path").s("s").u("").h("").p("/path").z();
        test("s://@").s("s").u("").h("").p("").z();
        test("s://@:8000/path").s("s").u("").h("").n(8000).p("/path").z();
        test("s://@:8000").s("s").u("").h("").n(8000).p("").z();

        header("Case of %encoded gen-delims: ssp starts with %2F%2F (//)");

        IRI i1 = test("s:%2F%2F%42u@h/foo?q#f").s("s")
                .o("%2F%2FBu@h/foo?q")
                .sp("%2F%2F%42u@h/foo?q")
                .spd("%2F%2FBu@h/foo?q")
                .p("%2F%2F%42u@h/foo")
                .pd("%2F%2FBu@h/foo")
                .q("q").f("f")
                .ts("s:%2F%2F%42u@h/foo?q#f")
                .ti("s:%2F%2FBu@h/foo?q#f")
                .z().uri();
        test("s", null, "%2F%2F%42u@h/foo", "q", "f")
                .iae().z(); // relative path in absolute uri
        test("s", null, null, -1, "%2F%2F%42u@h/foo", "q", "f")
                .iae().z(); // relative path in absolute uri
        IRI i2 = test("s", "%2F%2F%42u@h/foo?q", "f")
                .s("s")
                .o("%2F%2FBu@h/foo?q")
                .sp("%2F%2F%42u@h/foo?q")
                .spd("%2F%2FBu@h/foo?q")
                .p("%2F%2F%42u@h/foo")
                .pd("%2F%2FBu@h/foo")
                .q("q").f("f")
                .ts("s:%2F%2F%42u@h/foo?q#f")
                .ti("s:%2F%2FBu@h/foo?q#f")
                .z().uri();
        eq(i1,i2);
        IRI i3 = test("%2F%2F%42u@h/foo?q#f")
                .sp("%2F%2F%42u@h/foo?q")
                .spd("%2F%2FBu@h/foo?q")
                .p("%2F%2F%42u@h/foo")
                .pd("%2F%2FBu@h/foo")
                .q("q").f("f")
                .ts("%2F%2F%42u@h/foo?q#f")
                .ti("%2F%2FBu@h/foo?q#f")
                .z().uri();
        IRI i4 = test(null, null, "%2F%2F%42u@h/foo", "q", "f")
                .sp("%2F%2F%42u@h/foo?q")
                .spd("%2F%2FBu@h/foo?q")
                .p("%2F%2F%42u@h/foo")
                .pd("%2F%2FBu@h/foo")
                .q("q").f("f")
                .ts("%2F%2F%42u@h/foo?q#f")
                .ti("%2F%2FBu@h/foo?q#f")
                .z().uri();
        eq(i3,i4);
        IRI i5 = test(null, null, null, -1, "%2F%2F%42u@h/foo", "q", "f")
                .sp("%2F%2F%42u@h/foo?q")
                .spd("%2F%2FBu@h/foo?q")
                .p("%2F%2F%42u@h/foo")
                .pd("%2F%2FBu@h/foo")
                .q("q").f("f")
                .ts("%2F%2F%42u@h/foo?q#f")
                .ti("%2F%2FBu@h/foo?q#f")
                .z().uri();
        eq(i3,i5);
        IRI i6 = test(null, "%2F%2F%42u@h/foo?q", "f")
                .sp("%2F%2F%42u@h/foo?q")
                .spd("%2F%2FBu@h/foo?q")
                .p("%2F%2F%42u@h/foo")
                .pd("%2F%2FBu@h/foo")
                .q("q").f("f")
                .ts("%2F%2F%42u@h/foo?q#f")
                .ti("%2F%2FBu@h/foo?q#f")
                .z().uri();
        eq(i3, i6);
        IRI i7 = test(null, "//%42u@h/foo?q", "f")
                .sp("//%42u@h/foo?q")
                .spd("//Bu@h/foo?q")
                .p("/foo").pd("/foo")
                .u("%42u").ud("Bu").h("h")
                .g("%42u@h").gd("Bu@h")
                .q("q").f("f")
                .ts("//%42u@h/foo?q#f")
                .ti("//Bu@h/foo?q#f")
                .z().uri();
        ne(i6, i7);

        header("Case of %encoded gen-delims: host contains %2F (/)");

        i1 = test("s://%42u@h%2Ffoo?q#f").s("s").p("")
                .u("%42u").ud("Bu")
                .h("h%2Ffoo").hd("h/foo")
                .g("%42u@h%2Ffoo").gd("Bu@h/foo")
                .q("q").f("f")
                .ts("s://%42u@h%2Ffoo?q#f")
                .ti("s://Bu@h%2Ffoo?q#f")
                .z().uri();
        i2 = test("s", "%42u@h%2Ffoo", "", "q", "f")
                .s("s").p("").q("q").f("f")
                .u("%42u").ud("Bu")
                .h("h%2Ffoo").hd("h/foo")
                .g("%42u@h%2Ffoo").gd("Bu@h/foo")
                .z().uri();
        eq(i1,i2);
        i3 = test("s", "%42u@h/foo", "", "q", "f")
                .s("s").p("").q("q").f("f")
                .u("%42u").ud("Bu")
                .h("h%2Ffoo").hd("h/foo")
                .g("%42u@h%2Ffoo").gd("Bu@h/foo")
                .z().uri();
        eq(i1,i3);
        // IRIs are still equals if a non reserverd %encoded char is replaced
        // by its decoded form
        i4 = test("s", "Bu@h/foo", "", "q", "f")
                .s("s").p("").q("q").f("f")
                .u("Bu").ud("Bu")
                .h("h%2Ffoo").hd("h/foo")
                .g("Bu@h%2Ffoo").gd("Bu@h/foo")
                .z().uri();
        eq(i1,i4);
        i5 = test("s", "Bu", "h/foo", -1, "", "q", "f")
                .s("s").p("").q("q").f("f")
                .u("Bu").ud("Bu")
                .h("h%2Ffoo").hd("h/foo")
                .g("Bu@h%2Ffoo").gd("Bu@h/foo")
                .z().uri();
        eq(i1,i5);
        i6 = test("s://%42u@h%2ffoo?q#f").s("s").p("")
                .u("%42u").ud("Bu")
                .h("h%2ffoo").hd("h/foo")
                .g("%42u@h%2ffoo").gd("Bu@h/foo")
                .q("q").f("f")
                .ts("s://%42u@h%2ffoo?q#f")
                .ti("s://Bu@h%2Ffoo?q#f")
                .z().uri();
        eq(i1,i6);

        header("Case of %encoded gen-delims: user-info contains %40 (@)");

        i1 = test("s://%42%40u@h%2Ffoo?q#f").s("s").p("")
                .u("%42%40u").ud("B@u")
                .h("h%2Ffoo").hd("h/foo")
                .g("%42%40u@h%2Ffoo").gd("B%40u@h/foo")
                .q("q").f("f")
                .ts("s://%42%40u@h%2Ffoo?q#f")
                .ti("s://B%40u@h%2Ffoo?q#f")
                .z().uri();
        i2 = test("s", "%42%40u@h%2Ffoo", "", "q", "f")
                .s("s").p("").q("q").f("f")
                .u("%42%40u").ud("B@u")
                .h("h%2Ffoo").hd("h/foo")
                .g("%42%40u@h%2Ffoo").gd("B%40u@h/foo")
                .z().uri();
        eq(i1,i2);
        i3 = test("s", "%42%40u@h/foo", "", "q", "f")
                .s("s").p("").q("q").f("f")
                .u("%42%40u").ud("B@u")
                .h("h%2Ffoo").hd("h/foo")
                .g("%42%40u@h%2Ffoo").gd("B%40u@h/foo")
                .z().uri();
        eq(i1,i3);
        // IRIs are still equals if a non reserverd %encoded char is replaced
        // by its decoded form
        i4 = test("s", "B%40u@h/foo", "", "q", "f")
                .s("s").p("").q("q").f("f")
                .u("B%40u").ud("B@u")
                .h("h%2Ffoo").hd("h/foo")
                .g("B%40u@h%2Ffoo").gd("B%40u@h/foo")
                .z().uri();
        eq(i1,i4);
        i5 = test("s", "B@u", "h/foo", -1, "", "q", "f")
                .s("s").p("").q("q").f("f")
                .u("B%40u").ud("B@u")
                .h("h%2Ffoo").hd("h/foo")
                .g("B%40u@h%2Ffoo").gd("B%40u@h/foo")
                .z().uri();
        eq(i1,i5);
        i6 = test("s://%42%40u@h%2ffoo?q#f").s("s").p("")
                .u("%42%40u").ud("B@u")
                .h("h%2ffoo").hd("h/foo")
                .g("%42%40u@h%2ffoo").gd("B%40u@h/foo")
                .q("q").f("f")
                .ts("s://%42%40u@h%2ffoo?q#f")
                .ti("s://B%40u@h%2Ffoo?q#f")
                .z().uri();
        eq(i1,i6);

        header("Case of %encoded gen-delims: host contains %3A43 (:43)");

        i1 = test("s://%42%40u@h%2Ffoo%3A43:80?q#f").s("s").p("")
                .u("%42%40u").ud("B@u")
                .h("h%2Ffoo%3A43").hd("h/foo:43").n(80)
                .g("%42%40u@h%2Ffoo%3A43:80").gd("B%40u@h/foo%3A43:80")
                .q("q").f("f")
                .ts("s://%42%40u@h%2Ffoo%3A43:80?q#f")
                .ti("s://B%40u@h%2Ffoo%3A43:80?q#f")
                .z().uri();
        i2 = test("s", "%42%40u@h%2Ffoo%3A43:80", "", "q", "f")
                .s("s").p("").q("q").f("f")
                .u("%42%40u").ud("B@u")
                .h("h%2Ffoo%3A43").hd("h/foo:43").n(80)
                .g("%42%40u@h%2Ffoo%3A43:80").gd("B%40u@h/foo%3A43:80")
                .z().uri();
        eq(i1,i2);
        i3 = test("s", "%42%40u@h/foo%3A43:80", "", "q", "f")
                .s("s").p("").q("q").f("f")
                .u("%42%40u").ud("B@u")
                .h("h%2Ffoo%3A43").hd("h/foo:43").n(80)
                .g("%42%40u@h%2Ffoo%3A43:80").gd("B%40u@h/foo%3A43:80")
                .z().uri();
        eq(i1,i3);
        // IRIs are still equals if a non reserverd %encoded char is replaced
        // by its decoded form
        i4 = test("s", "B%40u@h/foo%3a43:80", "", "q", "f")
                .s("s").p("").q("q").f("f")
                .u("B%40u").ud("B@u")
                .h("h%2Ffoo%3a43").hd("h/foo:43").n(80)
                .g("B%40u@h%2Ffoo%3a43:80").gd("B%40u@h/foo%3A43:80")
                .z().uri();
        eq(i1,i4);
        i5 = test("s", "B@u", "h/foo:43", 80, "", "q", "f")
                .s("s").p("").q("q").f("f")
                .u("B%40u").ud("B@u")
                .h("h%2Ffoo%3A43").hd("h/foo:43").n(80)
                .g("B%40u@h%2Ffoo%3A43:80").gd("B%40u@h/foo%3A43:80")
                .z().uri();
        eq(i1,i5);
        i6 = test("s://%42%40u@h%2ffoo%3a43:80?q#f").s("s").p("")
                .u("%42%40u").ud("B@u")
                .h("h%2ffoo%3a43").hd("h/foo:43").n(80)
                .g("%42%40u@h%2ffoo%3a43:80").gd("B%40u@h/foo%3A43:80")
                .q("q").f("f")
                .ts("s://%42%40u@h%2ffoo%3a43:80?q#f")
                .ti("s://B%40u@h%2Ffoo%3A43:80?q#f")
                .z().uri();
        eq(i1,i6);

        header("Case of %encoded gen-delims: path contains %2F%5B%42%5D%3A%2F (/[B]:/)");

        i1 = test("s://%42%40u@h%2Ffoo%3A43:80/%2F%5B%42%5D%3A%2Fbar?q#f").s("s")
                .p("/%2F%5B%42%5D%3A%2Fbar").pd("/%2F[B]:%2Fbar")
                .u("%42%40u").ud("B@u")
                .h("h%2Ffoo%3A43").hd("h/foo:43").n(80)
                .g("%42%40u@h%2Ffoo%3A43:80").gd("B%40u@h/foo%3A43:80")
                .q("q").f("f")
                .ts("s://%42%40u@h%2Ffoo%3A43:80/%2F%5B%42%5D%3A%2Fbar?q#f")
                .ta("s://%42%40u@h%2Ffoo%3A43:80/%2F%5B%42%5D%3A%2Fbar?q#f")
                .ti("s://B%40u@h%2Ffoo%3A43:80/%2F%5BB%5D:%2Fbar?q#f")
                .z().uri();
        i2 = test("s", "%42%40u@h%2Ffoo%3A43:80", "/%2F[%42]:%2Fbar", "q", "f")
                .p("/%2F%5B%42%5D:%2Fbar").pd("/%2F[B]:%2Fbar")
                .s("s").q("q").f("f")
                .u("%42%40u").ud("B@u")
                .h("h%2Ffoo%3A43").hd("h/foo:43").n(80)
                .g("%42%40u@h%2Ffoo%3A43:80").gd("B%40u@h/foo%3A43:80")
                .ts("s://%42%40u@h%2Ffoo%3A43:80/%2F%5B%42%5D:%2Fbar?q#f")
                .ta("s://%42%40u@h%2Ffoo%3A43:80/%2F%5B%42%5D:%2Fbar?q#f")
                .ti("s://B%40u@h%2Ffoo%3A43:80/%2F%5BB%5D:%2Fbar?q#f")
                .z().uri();
        eq(i1,i2);
        i3 = test("s", "%42%40u@h/foo%3A43:80", "/%2F%5BB%5D%3A%2Fbar", "q", "f")
                .p("/%2F%5BB%5D%3A%2Fbar").pd("/%2F[B]:%2Fbar")
                .s("s").q("q").f("f")
                .u("%42%40u").ud("B@u")
                .h("h%2Ffoo%3A43").hd("h/foo:43").n(80)
                .g("%42%40u@h%2Ffoo%3A43:80").gd("B%40u@h/foo%3A43:80")
                .ts("s://%42%40u@h%2Ffoo%3A43:80/%2F%5BB%5D%3A%2Fbar?q#f")
                .ta("s://%42%40u@h%2Ffoo%3A43:80/%2F%5BB%5D%3A%2Fbar?q#f")
                .ti("s://B%40u@h%2Ffoo%3A43:80/%2F%5BB%5D:%2Fbar?q#f")
                .z().uri();
        eq(i1,i3);
        // IRIs are still equals if a non reserverd %encoded char is replaced
        // by its decoded form
        i4 = test("s", "B%40u@h/foo%3a43:80", "/%2f%5BB%5D%3a%2Fbar", "q", "f")
                .p("/%2f%5BB%5D%3a%2Fbar").pd("/%2F[B]:%2Fbar")
                .s("s").q("q").f("f")
                .u("B%40u").ud("B@u")
                .h("h%2Ffoo%3a43").hd("h/foo:43").n(80)
                .g("B%40u@h%2Ffoo%3a43:80").gd("B%40u@h/foo%3A43:80")
                .ts("s://B%40u@h%2Ffoo%3a43:80/%2f%5BB%5D%3a%2Fbar?q#f")
                .ta("s://B%40u@h%2Ffoo%3a43:80/%2f%5BB%5D%3a%2Fbar?q#f")
                .ti("s://B%40u@h%2Ffoo%3A43:80/%2F%5BB%5D:%2Fbar?q#f")
                .z().uri();
        eq(i1,i4);
        i5 = test("s", "B@u", "h/foo:43", 80, "/%2F[B]:%2Fbar", "q", "f")
                .p("/%2F%5BB%5D:%2Fbar").pd("/%2F[B]:%2Fbar")
                .s("s").q("q").f("f")
                .u("B%40u").ud("B@u")
                .h("h%2Ffoo%3A43").hd("h/foo:43").n(80)
                .g("B%40u@h%2Ffoo%3A43:80").gd("B%40u@h/foo%3A43:80")
                .ts("s://B%40u@h%2Ffoo%3A43:80/%2F%5BB%5D:%2Fbar?q#f")
                .ta("s://B%40u@h%2Ffoo%3A43:80/%2F%5BB%5D:%2Fbar?q#f")
                .ti("s://B%40u@h%2Ffoo%3A43:80/%2F%5BB%5D:%2Fbar?q#f")
                .z().uri();
        eq(i1,i5);
        i6 = test("s://%42%40u@h%2ffoo%3a43:80/%2f%5b%42%5d%3a%2fbar?q#f").s("s")
                .p("/%2f%5b%42%5d%3a%2fbar").pd("/%2F[B]:%2Fbar")
                .u("%42%40u").ud("B@u")
                .h("h%2ffoo%3a43").hd("h/foo:43").n(80)
                .g("%42%40u@h%2ffoo%3a43:80").gd("B%40u@h/foo%3A43:80")
                .q("q").f("f")
                .ts("s://%42%40u@h%2ffoo%3a43:80/%2f%5b%42%5d%3a%2fbar?q#f")
                .ti("s://B%40u@h%2Ffoo%3A43:80/%2F%5BB%5D:%2Fbar?q#f")
                .z().uri();
        eq(i1,i6);

        header("Bidi characters and non-characters in BMP");

        Consumer<Character> bidiAndNonCharBMPTest = (c) -> {
            char ch = c;
            String s = "http", h="xn--99zt52a.example.org", p="/a" + ch + 'b';
            String u = String.format("%s://%s%s", s, h, p);
            String pe = appendEscape(new StringBuilder("/a"), ch).append('b').toString();
            String ts = String.format("%s://%s%s", s, h, pe);

            test(u).x().z();
            test(ts).s(s).h(h).p(pe).pd(pe)
                    .ts(ts).ti(ts).z();
            test(s, h, p, null)
                    .s(s).h(h).p(pe).pd(pe)
                    .ts(ts).ti(ts).z();
            test(s, "//" + h + p, null)
                    .s(s).h(h).p(pe).pd(pe).sp("//"+h+pe)
                    .ts(ts).ti(ts).z();
            test(s, h, p, null, null)
                    .s(s).h(h).p(pe).pd(pe)
                    .ts(ts).ti(ts).z();
            test(s, null, h, -1, p, null, null)
                    .s(s).h(h).p(pe).pd(pe)
                    .ts(ts).ti(ts).z();
        };

        // Build a stream that contains all bidi chars and all
        // non characters in the Basic Multilingual Plane, and
        // verify they are rejected/encoded properly
        Stream.concat(Stream.concat(BIDIS.stream(),
                Stream.iterate((char)0xFDD0,
                        (Character c) -> c <= 0xFDEF,
                        (Character c) -> (char)(c + 1))),
                Stream.of((char)0xFFFE, (char) 0xFFFF))
                .forEach(bidiAndNonCharBMPTest);

        header("Non characters in supplementary planes");

        IntConsumer nonCharsCodepointsTest = (c) -> {
            char[] ch = Character.toChars(c);
            assert ch.length == 2;
            out.println(String.format("\n-- Plane %d, codepoint: U+%H", c/0x10000, c));
            char[] pc = {'/', 'a', ch[0], ch[1], 'b'};
            String p = new String(pc);
            String s = "http", h="xn--99zt52a.example.org";
            String u = String.format("%s://%s%s", s, h, p);
            String pe = appendEscape(new StringBuilder("/a"), ch[0], ch[1])
                    .append('b').toString();
            String ts = String.format("%s://%s%s", s, h, pe);

            test(u).x().z();
            test(ts).s(s).h(h).p(pe).pd(pe)
                    .ts(ts).ti(ts).z();
            test(s, h, p, null)
                    .s(s).h(h).p(pe).pd(pe)
                    .ts(ts).ti(ts).z();
            test(s, "//" + h + p, null)
                    .s(s).h(h).p(pe).pd(pe).sp("//"+h+pe)
                    .ts(ts).ti(ts).z();
            test(s, h, p, null, null)
                    .s(s).h(h).p(pe).pd(pe)
                    .ts(ts).ti(ts).z();
            test(s, null, h, -1, p, null, null)
                    .s(s).h(h).p(pe).pd(pe)
                    .ts(ts).ti(ts).z();
        };

        // two non chars per plane
        IntStream.range(1,16) // supplementary planes 1-15
                .flatMap((i) -> IntStream.of(
                        (i << 16) + 0x00FFFE, (i << 16) + 0x00FFFF))
                .forEach(nonCharsCodepointsTest);
        IntStream.of(0x10FFFE, 0x10FFFF) // supplementary plane 16
                .forEach(nonCharsCodepointsTest);

        header("Private characters");

        IntConsumer iprivateInQueryTest = (cp) -> {
            char[] ch = Character.toChars(cp);
            out.println(String.format("\n-- Plane %d, private codepoint: U+%H", cp/0x10000, cp));
            assert ch.length <= 2; // 1 if BMP, 2 otherwise (surrogate pair)
            char[] qc = ch.length == 1
                    ? new char[] {'q', '=', 'a', ch[0], 'b'}
                    : new char[] {'q', '=', 'a', ch[0], ch[1], 'b'};
            String q = new String(qc);
            String s = "http", h="xn--99zt52a.example.org", p="/p";
            String u = String.format("%s://%s%s?%s", s, h, p, q);
            String qe = appendEscape(new StringBuilder("q=a"), ch)
                    .append('b').toString();
            String ta = String.format("%s://%s%s?%s", s, h, p, qe);

            test(u).s(s).h(h).p(p)
                    .q(q).qd(q)
                    .ts(u).ti(u).z();
            test(ta).s(s).h(h).p(p)
                    .q(qe).qd(q)
                    .ts(ta).ta(ta).ti(u).z();
            test(s, h, p, q, null)
                    .s(s).h(h).p(p)
                    .q(q).qd(q)
                    .ts(u).ti(u).ta(ta).z();
            test(s, "//" + h + p + "?" + q, null)
                    .s(s).h(h).p(p).q(q).qd(q)
                    .sp("//"+h+p+"?"+q)
                    .ts(u).ti(u).ta(ta).z();
            test(s, null, h, -1, p, q, null)
                    .s(s).h(h).p(p)
                    .q(q).qd(q)
                    .ts(u).ti(u).ta(ta).z();
        };

        IntConsumer iprivateInPathTest = (cp) -> {
            char[] ch = Character.toChars(cp);
            out.println(String.format("\n-- Plane %d, private codepoint: U+%H", cp/0x10000, cp));
            assert ch.length <= 2; // 1 if BMP, 2 otherwise (surrogate pair)
            char[] qc = ch.length == 1
                    ? new char[] {'q', '=', 'a', ch[0], 'b'}
                    : new char[] {'q', '=', 'a', ch[0], ch[1], 'b'};
            char[] pc = ch.length == 1
                    ? new char[] {'/', 'a', ch[0], 'b'}
                    : new char[] {'/', 'a', ch[0], ch[1], 'b'};
            String q = new String(qc);
            String p = new String(pc);
            String s = "http", h="xn--99zt52a.example.org";
            String u = String.format("%s://%s%s?%s", s, h, p, q);
            String qe = appendEscape(new StringBuilder("q=a"), ch)
                    .append('b').toString();
            String pe = appendEscape(new StringBuilder("/a"), ch)
                    .append('b').toString();
            String ue = String.format("%s://%s%s?%s", s, h, pe, q);
            String ta = String.format("%s://%s%s?%s", s, h, pe, qe);

            test(u).x().z();
            test(ue).s(s).h(h)
                    .p(pe).pd(p)
                    .q(q).qd(q)
                    .ts(ue).ti(ue).ta(ta).z();
            test(ta).s(s).h(h)
                    .p(pe).pd(p)
                    .q(qe).qd(q)
                    .ts(ta).ta(ta).ti(ue).z();
            test(s, h, p, q, null)
                    .s(s).h(h)
                    .p(pe).pd(p)
                    .q(q).qd(q)
                    .ts(ue).ti(ue).ta(ta).z();
            test(s, "//" + h + p + "?" + q, null)
                    .s(s).h(h).p(pe).pd(p).q(q).qd(q)
                    .sp("//"+h+pe+"?"+q)
                    .spd("//"+h+p+"?"+q)
                    .ts(ue).ti(ue).ta(ta).z();
            test(s, null, h, -1, p, q, null)
                    .s(s).h(h).p(pe).pd(p)
                    .q(q).qd(q)
                    .ts(ue).ti(ue).ta(ta).z();
        };

        IntConsumer iprivateInFragTest = (cp) -> {
            char[] ch = Character.toChars(cp);
            out.println(String.format("\n-- Plane %d, private codepoint: U+%H", cp/0x10000, cp));
            assert ch.length <= 2; // 1 if BMP, 2 otherwise (surrogate pair)
            char[] qc = ch.length == 1
                    ? new char[] {'q', '=', 'a', ch[0], 'b'}
                    : new char[] {'q', '=', 'a', ch[0], ch[1], 'b'};
            char[] fc = ch.length == 1
                    ? new char[] {'f', 'a', ch[0], 'b'}
                    : new char[] {'f', 'a', ch[0], ch[1], 'b'};
            String q = new String(qc);
            String f = new String(fc);
            String p = "/p", pe = "/p";
            String s = "http", h="xn--99zt52a.example.org";
            String u = String.format("%s://%s%s?%s#%s", s, h, pe, q, f);
            String qe = appendEscape(new StringBuilder("q=a"), ch)
                    .append('b').toString();
            String fe = appendEscape(new StringBuilder("fa"), ch)
                    .append('b').toString();
            String ue = String.format("%s://%s%s?%s#%s", s, h, pe, q, fe);
            String ta = String.format("%s://%s%s?%s#%s", s, h, pe, qe, fe);

            test(u).x().z();
            test(ue).s(s).h(h)
                    .p(pe).pd(p)
                    .q(q).qd(q)
                    .f(fe).fd(f)
                    .ts(ue).ti(ue).ta(ta).z();
            test(ta).s(s).h(h)
                    .p(pe).pd(p)
                    .q(qe).qd(q)
                    .f(fe).fd(f)
                    .ts(ta).ta(ta).ti(ue).z();
            test(s, h, p, q, f)
                    .s(s).h(h)
                    .p(pe).pd(p)
                    .f(fe).fd(f)
                    .q(q).qd(q)
                    .ts(ue).ti(ue).ta(ta).z();
            test(s, "//" + h + p + "?" + q, f)
                    .s(s).h(h).p(pe).pd(p).q(q).qd(q)
                    .sp("//"+h+pe+"?"+q)
                    .spd("//"+h+p+"?"+q)
                    .f(fe).fd(f)
                    .ts(ue).ti(ue).ta(ta).z();
            test(s, null, h, -1, p, q, f)
                    .s(s).h(h).p(pe).pd(p)
                    .q(q).qd(q)
                    .f(fe).fd(f)
                    .ts(ue).ti(ue).ta(ta).z();
        };

        IntConsumer iprivateInHostTest = (cp) -> {
            char[] ch = Character.toChars(cp);
            out.println(String.format("\n-- Plane %d, private codepoint: U+%H", cp/0x10000, cp));
            assert ch.length <= 2; // 1 if BMP, 2 otherwise (surrogate pair)
            char[] qc = ch.length == 1
                    ? new char[] {'q', '=', 'a', ch[0], 'b'}
                    : new char[] {'q', '=', 'a', ch[0], ch[1], 'b'};
            char[] hc = ch.length == 1
                    ? new char[] {'h', '.', ch[0], '.', 'b'}
                    : new char[] {'h', '.', ch[0], ch[1], '.', 'b'};
            String f = "f", fe = "f";
            String q = new String(qc);
            String h = new String(hc);
            String p = "/p", pe = "/p";
            String s = "http";
            String u = String.format("%s://%s%s?%s#%s", s, h, pe, q, fe);
            String qe = appendEscape(new StringBuilder("q=a"), ch)
                    .append('b').toString();
            String he = appendEscape(new StringBuilder("h."), ch)
                    .append(".b").toString();
            String ue = String.format("%s://%s%s?%s#%s", s, he, pe, q, fe);
            String ta = String.format("%s://%s%s?%s#%s", s, he, pe, qe, fe);

            test(u).x().z();
            test(ue).s(s)
                    .h(he).hd(h)
                    .g(he).gd(h)
                    .p(pe).pd(p)
                    .q(q).qd(q)
                    .f(fe).fd(f)
                    .ts(ue).ti(ue).ta(ta).z();
            test(ta).s(s)
                    .h(he).hd(h)
                    .g(he).gd(h)
                    .p(pe).pd(p)
                    .q(qe).qd(q)
                    .f(fe).fd(f)
                    .ts(ta).ta(ta).ti(ue).z();
            test(s, h, p, q, f).s(s)
                    .h(he).hd(h)
                    .g(he).gd(h)
                    .p(pe).pd(p)
                    .f(fe).fd(f)
                    .q(q).qd(q)
                    .ts(ue).ti(ue).ta(ta).z();
            test(s, "//" + h + p + "?" + q, f)
                    .s(s).h(he).hd(h)
                    .g(he).gd(h)
                    .p(pe).pd(p).q(q).qd(q)
                    .sp("//"+he+pe+"?"+q)
                    .spd("//"+h+p+"?"+q)
                    .f(fe).fd(f)
                    .ts(ue).ti(ue).ta(ta).z();
            test(s, null, h, -1, p, q, f)
                    .s(s).h(he).hd(h)
                    .g(he).gd(h)
                    .p(pe).pd(p)
                    .q(q).qd(q)
                    .f(fe).fd(f)
                    .ts(ue).ti(ue).ta(ta).z();
        };

        IntConsumer iprivateInUserTest = (cp) -> {
            char[] ch = Character.toChars(cp);
            out.println(String.format("\n-- Plane %d, private codepoint: U+%H", cp/0x10000, cp));
            assert ch.length <= 2; // 1 if BMP, 2 otherwise (surrogate pair)
            char[] qc = ch.length == 1
                    ? new char[] {'q', '=', 'a', ch[0], 'b'}
                    : new char[] {'q', '=', 'a', ch[0], ch[1], 'b'};
            char[] uc = ch.length == 1
                    ? new char[] {'u', '-', ch[0], '-', 'u'}
                    : new char[] {'u', '-', ch[0], ch[1], '-', 'u'};
            String f = "f", fe = "f";
            String q = new String(qc);
            String ui = new String(uc);
            String g = ui + "@h";
            String p = "/p", pe = "/p";
            String s = "http", h="h", he="h";
            String u = String.format("%s://%s%s?%s#%s", s, g, pe, q, fe);
            String qe = appendEscape(new StringBuilder("q=a"), ch)
                    .append('b').toString();
            String uie = appendEscape(new StringBuilder("u-"), ch)
                    .append("-u").toString();
            String ge = uie + "@h";
            String ue = String.format("%s://%s%s?%s#%s", s, ge, pe, q, fe);
            String ta = String.format("%s://%s%s?%s#%s", s, ge, pe, qe, fe);

            test(u).x().z();
            test(ue).s(s)
                    .h(he).hd(h)
                    .u(uie).ud(ui)
                    .g(ge).gd(g)
                    .p(pe).pd(p)
                    .q(q).qd(q)
                    .f(fe).fd(f)
                    .ts(ue).ti(ue).ta(ta).z();
            test(ta).s(s)
                    .h(he).hd(h)
                    .u(uie).ud(ui)
                    .g(ge).gd(g)
                    .p(pe).pd(p)
                    .q(qe).qd(q)
                    .f(fe).fd(f)
                    .ts(ta).ta(ta).ti(ue).z();
            test(s, g, p, q, f).s(s)
                    .h(he).hd(h)
                    .u(uie).ud(ui)
                    .g(ge).gd(g)
                    .p(pe).pd(p)
                    .f(fe).fd(f)
                    .q(q).qd(q)
                    .ts(ue).ti(ue).ta(ta).z();
            test(s, "//" + g + p + "?" + q, f)
                    .s(s).h(he).hd(h)
                    .u(uie).ud(ui)
                    .g(ge).gd(g)
                    .p(pe).pd(p).q(q).qd(q)
                    .sp("//"+ge+pe+"?"+q)
                    .spd("//"+g+p+"?"+q)
                    .f(fe).fd(f)
                    .ts(ue).ti(ue).ta(ta).z();
            test(s, ui, h, -1, p, q, f)
                    .s(s).h(he).hd(h)
                    .u(uie).ud(ui)
                    .g(ge).gd(g)
                    .p(pe).pd(p)
                    .q(q).qd(q)
                    .f(fe).fd(f)
                    .ts(ue).ti(ue).ta(ta).z();
        };

        header("Private characters in query");

        // iprivate = %xE000-F8FF / %xF0000-FFFFD / %x100000-10FFFD
        // test boundaries + one char in the middle of each interval
        IntStream.of(0xE000, 0xEFAC, 0xF8FF,
                     0xF0000, 0xFCAFE, 0xFFFFD,
                     0x100000, 0x10EFAC, 0x10FFFD)
                .forEach(iprivateInQueryTest);

        header("Private characters in path");

        IntStream.of(0xE000, 0xEFAC, 0xF8FF,
                0xF0000, 0xFCAFE, 0xFFFFD,
                0x100000, 0x10EFAC, 0x10FFFD)
                .forEach(iprivateInPathTest);

        header("Private characters in fragment");

        IntStream.of(0xE000, 0xEFAC, 0xF8FF,
                0xF0000, 0xFCAFE, 0xFFFFD,
                0x100000, 0x10EFAC, 0x10FFFD)
                .forEach(iprivateInFragTest);

        header("Private characters in host");

        IntStream.of(0xE000, 0xEFAC, 0xF8FF,
                0xF0000, 0xFCAFE, 0xFFFFD,
                0x100000, 0x10EFAC, 0x10FFFD)
                .forEach(iprivateInHostTest);

        header("Private characters in user-info");

        IntStream.of(0xE000, 0xEFAC, 0xF8FF,
                0xF0000, 0xFCAFE, 0xFFFFD,
                0x100000, 0x10EFAC, 0x10FFFD)
                .forEach(iprivateInUserTest);

    }

    static void ip() {

        header("IP addresses");

        test("http://1.2.3.4:5")
            .s("http").h("1.2.3.4").ipv4().n(5).p("").z();

        // From RFC2732

        test("http://[FEDC:BA98:7654:3210:FEDC:BA98:7654:3210]:80/index.html")
            .s("http").h("[FEDC:BA98:7654:3210:FEDC:BA98:7654:3210]")
            .ipv6().n(80).p("/index.html").z();

        test("http://[FEDC:BA98:7654:3210:FEDC:BA98:7654:10%12]:80/index.html")
            .s("http").h("[FEDC:BA98:7654:3210:FEDC:BA98:7654:10%12]")
            .ipv6().n(80).p("/index.html").z();

        test("http://[1080:0:0:0:8:800:200C:417A]/index.html")
            .s("http").h("[1080:0:0:0:8:800:200C:417A]")
            .ipv6().p("/index.html").z();

        test("http://[1080:0:0:0:8:800:200C:417A%1]/index.html")
            .s("http").h("[1080:0:0:0:8:800:200C:417A%1]")
            .ipv6().p("/index.html").z();

        test("http://[3ffe:2a00:100:7031::1]")
            .s("http").h("[3ffe:2a00:100:7031::1]")
            .ipv6().p("").z();

        test("http://[1080::8:800:200C:417A]/foo")
            .s("http").h("[1080::8:800:200C:417A]").ipv6().p("/foo").z();

        test("http://[::192.9.5.5]/ipng")
            .s("http").h("[::192.9.5.5]").ipv6().p("/ipng").z();

        test("http://[::192.9.5.5%interface]/ipng")
            .s("http").h("[::192.9.5.5%interface]").ipv6().p("/ipng").z();

        test("http://[::FFFF:129.144.52.38]:80/index.html")
            .s("http").h("[::FFFF:129.144.52.38]")
            .ipv6().n(80).p("/index.html").z();

        test("http://[2010:836B:4179::836B:4179]")
            .s("http").h("[2010:836B:4179::836B:4179]").ipv6().p("").z();

        // From RFC2373

        test("http://[FF01::101]")
            .s("http").h("[FF01::101]").ipv6().p("").z();

        test("http://[::1]")
            .s("http").h("[::1]").ipv6().p("").z();

        test("http://[::]")
            .s("http").h("[::]").ipv6().p("").z();

        test("http://[::%hme0]")
            .s("http").h("[::%hme0]").ipv6().p("").z();

        test("http://[0:0:0:0:0:0:13.1.68.3]")
            .s("http").h("[0:0:0:0:0:0:13.1.68.3]").ipv6().p("").z();

        test("http://[0:0:0:0:0:FFFF:129.144.52.38]")
            .s("http").h("[0:0:0:0:0:FFFF:129.144.52.38]").ipv6().p("").z();

        test("http://[0:0:0:0:0:FFFF:129.144.52.38%33]")
            .s("http").h("[0:0:0:0:0:FFFF:129.144.52.38%33]").ipv6().p("").z();

        test("http://[0:0:0:0:0:ffff:1.2.3.4]")
            .s("http").h("[0:0:0:0:0:ffff:1.2.3.4]").ipv6().p("").z();

        test("http://[::13.1.68.3]")
            .s("http").h("[::13.1.68.3]").ipv6().p("").z();

        // Optional IPv6 brackets in constructors

        test("s", null, "1:2:3:4:5:6:7:8", -1, null, null, null)
            .s("s").h("[1:2:3:4:5:6:7:8]").ipv6().p("").z();

        test("s", null, "[1:2:3:4:5:6:7:8]", -1, null, null, null)
            .s("s").h("[1:2:3:4:5:6:7:8]").ipv6().p("").z();

        test("s", null, "[1:2:3:4:5:6:7:8]", -1, null, null, null)
            .s("s").h("[1:2:3:4:5:6:7:8]").ipv6().p("").z();

        test("s", "1:2:3:4:5:6:7:8", null, null)
            .s("s").h("[1:2:3:4:5:6:7:8]").ipv6().p("").z();

        test("s", "1:2:3:4:5:6:7:8%hme0", null, null)
            .s("s").h("[1:2:3:4:5:6:7:8%hme0]").ipv6().p("").z();

        test("s", "1:2:3:4:5:6:7:8%1", null, null)
            .s("s").h("[1:2:3:4:5:6:7:8%1]").ipv6().p("").z();

        test("s", "[1:2:3:4:5:6:7:8]", null, null)
            .s("s").h("[1:2:3:4:5:6:7:8]").ipv6().p("").z();

        test("s", "[1:2:3:4:5:6:7:8]", null, null, null)
            .s("s").h("[1:2:3:4:5:6:7:8]").ipv6().p("").z();

        // Error cases

        test("s", "1:2:3:4:5:6:7:8", null, null, null)
                .x().z();
        test("http://[ff01:234/foo").x().z();
        test("http://[ff01:234:zzz]/foo").x().z();
        test("http://[foo]").x().z();
        test("http://[]").x().z();
        test("http://[129.33.44.55]").x().z();
        test("http://[ff:ee:dd:cc:bb::aa:9:8]").x().z();
        test("http://[fffff::1]").x().z();
        test("http://[ff::ee::8]").x().z();
        test("http://[1:2:3:4::5:6:7:8]").x().z();
        test("http://[1:2]").x().z();
        test("http://[1:2:3:4:5:6:7:8:9]").x().z();
        test("http://[1:2:3:4:5:6:7:8%]").x().z();
        test("http://[1:2:3:4:5:6:7:8%!/]").x().z();
        test("http://[::1.2.3.300]").x().z();
        test("http://[1.2.3.4:5]").x().z();
        test("http://1:2:3:4:5:6:7:8").x().z();
        test("http://[1.2.3.4]/").x().z();
        test("http://[1.2.3.4/").x().z();
        test("http://[foo]/").x().z();
        test("http://[foo/").x().z();
        test("s", "[foo]", "/", null, null)
                .s("s").h("%5Bfoo%5D").hd("[foo]")  // as per RFC3986, parses as reg-name
                .regn().p("/").z();
        test("s", "[foo", "/", null, null)
                .s("s").h("%5Bfoo").hd("[foo")      // as per RFC3986, parses as reg-name
                .regn().p("/").z();
        test("s", "[::foo", "/", null, null).x().z();

        // Test hostnames that might initially look like IPv4 addresses

        // TODO: is this really reasonable?
        test("http://1.2.3")                        // as per RFC3986, parses as reg-name
                .s("http").h("1.2.3").regn().p("").z();
        test("http://1.2.3.300")                    // as per RFC3986, parses as reg-name
                .s("http").h("1.2.3.300").regn().p("").z();
        test("http://1.2.3.4.5")                    // as per RFC3986, parses as reg-name
                .s("http").h("1.2.3.4.5").regn().p("").z();

        test("s://1.2.3.com").psa().s("s").h("1.2.3.com").dns().p("").z();
        test("s://1.2.3.4me.com").psa().s("s").h("1.2.3.4me.com").dns().p("").z();

        test("s://7up.com").psa().s("s").h("7up.com").dns().p("").z();
        test("s://7up.com/p").psa().s("s").h("7up.com").dns().p("/p").z();
        test("s://7up").psa().s("s").h("7up").p("").dns().z();
        test("s://7up/p").psa().s("s").h("7up").dns().p("/p").z();
        test("s://7up.").psa().s("s").h("7up.").dns().p("").z();
        test("s://7up./p").psa().s("s").h("7up.").dns().p("/p").z();

        // bug 6345551
        test("s", "ui@[::0000]", "/path", null, null)
                .s("s").u("ui").h("[::0000]").ipv6().p("/path").z();

        // IPvFuture = "v" 1*HEXDIG "." 1*( unreserved / sub-delims / ":" )
        test("s://[v8.a]/path").s("s").h("[v8.a]").ipvf().p("/path").z();
        test("s://[v8.a:b]/path").s("s").h("[v8.a:b]").ipvf().p("/path").z();
        test("s://[v8.a:]/path").s("s").h("[v8.a:]").ipvf().p("/path").z();
        test("s://[v8.:a]/path").s("s").h("[v8.:a]").ipvf().p("/path").z();
        test("s://[v8.2008]/path").s("s").h("[v8.2008]").ipvf().p("/path").z();
        test("s://[v8.~]/path").s("s").h("[v8.~]").ipvf().p("/path").z();
        test("s://[v8.$]/path").s("s").h("[v8.$]").ipvf().p("/path").z();
        test("s://[v8.:]/path").s("s").h("[v8.:]").ipvf().p("/path").z();
        test("s://ui@[v8.a:b]:80/path").ipvf()
                .s("s").u("ui").h("[v8.a:b]").n(80).p("/path").z();
        test("s://@[v8.a:b]:80/path").ipvf()
                .s("s").u("").h("[v8.a:b]").n(80).p("/path").z();
        test("s://ui@[v8.a:b]:/path").ipvf()
                .s("s").u("ui").h("[v8.a:b]").p("/path").z();
        test("s://@[v8.a:b]:/path").ipvf()
                .s("s").u("").h("[v8.a:b]").p("/path").z();
        test("s://[v8]/path").x().z();
        test("s://[vv.]/path").x().z();
        test("s://[v8.@]/path").x().z();
        test("s://@[vf0123456789aBcDe..::::a0-._~:!$&'()*+,;=]/path")
                .s("s").u("").ipvf()
                .h("[vf0123456789aBcDe..::::a0-._~:!$&'()*+,;=]")
                .p("/path").z();
        test("s://[v8.]/path").x().z();
        test("s://[v.]/path").x().z();
        test("s://[v]/path").x().z();

        // Non dns hosts

        test("s://100\u20AC.com/path")
                .s("s")
                .h("100\u20AC.com")
                .hd("100\u20AC.com")
                .p("/path")
                .regn()
                .z();
        test("s://100%E2%82%AC.com/path")
                .s("s")
                .h("100%E2%82%AC.com")
                .hd("100\u20AC.com")
                .p("/path")
                .regn()
                .z();
        test("s://x_z.com/path")
                .s("s")
                .h("x_z.com")
                .p("/path")
                .regn()
                .z();
        test("s://%41%42%43.com/path")
                .s("s")
                .h("%41%42%43.com")
                .hd("ABC.com")
                .p("/path")
                .dns() // The host name is a DNS name - though the raw host is not
                .z();
    }


    static void misc() throws URISyntaxException {

        IRI base = IRI.parseIRI("s://h/a/b");
        IRI rbase = IRI.parseIRI("a/b/c/d");


        header("Corner cases");

        // The empty URI parses as a relative URI with an empty path
        test("").p("").z()
                .rslv(base)
                .s("s").h("h").p("/a/b").z();     // as in RFC3986

        // Resolving solo queries and fragments
        test("#f").p("").f("f").z()
            .rslv(base).s("s").h("h").p("/a/b").f("f").z();
        test("?q").p("").q("q").z()
                .rslv(base)
                .s("s").h("h").p("/a/b").q("q").z();      // as in RFC3986

        // Fragment is not part of ssp
        test("p#f").p("p").f("f").sp("p").z();
        test("s:p#f").s("s").o("p").p("p").f("f").z();
        test("p#f")
            .rslv(base).s("s").h("h").p("/a/p").f("f").sp("//h/a/p").z();
        test("").p("").sp("").z();

        // scheme only
        test("abc:").s("abc").p("").sp("").z();
        test("abc:/").s("abc").p("/").sp("/").z();
        test("abc://").s("abc").p("").sp("//").g("").h("").z(); // as in RFC3986
        test("abc:///").s("abc").p("/").sp("///").g("").h("").z(); // as in RFC3986
        test("abc://?").s("abc").p("").sp("//?").g("").h("").q("").z(); // as in RFC3986
        test("abc://#").s("abc").p("").sp("//").g("").h("").f("").z(); // as in RFC3986


        header("Emptiness");

        // Components that may be empty
        test("//").p("").g("").h("").z();          // Authority (w/o path)
        test("///").p("/").g("").h("").z();        // Authority (w/ path)
        test("///p").p("/p").g("").h("").z();      // Authority (w/ path)
        test("//@h/p").u("").h("h").p("/p").z();   // User info
        test("//h:/p").h("h").p("/p").z();         // Port
        test("//h").h("h").p("").z();              // Path
        test("//h?q").h("h").p("").q("q").z();     // Path (w/query)
        test("//?q").p("").q("q").g("").h("").z(); // Authority (w/query)
        test("//#f").p("").f("f").g("").h("").z(); // Authority (w/fragment)
        test("p?#").p("p").q("").f("").z();        // Query & fragment

        // Components that may not be empty
        test(":").x().z();              // Scheme
        test("x:").s("x").p("").z();    // as in RFC3986

        header("Resolution, normalization, and relativization");

        // Resolving relative paths
        test("../e/f").p("../e/f").z()
            .rslv(rbase).p("a/b/e/f").z();
        test("../../../../d").p("../../../../d").z()
            .rslv(rbase).p("../d").z();
        test("../../../d:e").p("../../../d:e").z()
            .rslv(rbase).p("./d:e").z();
        test("../../../d:e/f").p("../../../d:e/f").z()
            .rslv(rbase).p("./d:e/f").z();
        IRI odd = test("s://h/p/a://b/c/d")
                .s("s").h("h").p("/p/a://b/c/d").z()
                .rtvz(IRI.of("s://h/p/"))
                .p("./a:/b/c/d").z().uri();
        test(odd.toString())
                .p("./a:/b/c/d").norm()
                .p("./a:/b/c/d").z();

        // Normalization
        test("a/./c/../d/f").p("a/./c/../d/f").z()
            .rst().norm().p("a/d/f").z();
        test("http://a/./b/c/../d?q#f")
            .s("http").h("a").p("/./b/c/../d").q("q").f("f").z()
            .rst().norm().s("http").h("a").p("/b/d").q("q").f("f").z();
        test("a/../b").p("a/../b").z()
            .rst().norm().p("b").z();
        test("a/../b:c").p("a/../b:c").z()
            .rst().norm().p("./b:c").z();

        // Normalization of already normalized URI should yield the
        // same URI
        Test t1 = test("s://h/p?/../b#/../d").s("s").h("h").p("/p").q("/../b").f("/../d").z();
        IRI u1 = t1.uri();
        IRI u2 = t1.rst().norm().s("s").h("h").p("/p").q("/../b").f("/../d").z().uri();
        eq(u1, u2);
        eqeq(u1, u2);

        // Normalization of not already normalized URI should yield
        // a different URI
        Test t2 = test("s://h/../p").s("s").h("h").p("/../p").z();  // RFC 3986 -> http://h/p
        IRI iri1 = t2.uri();
        IRI iri2 = t2.rst().norm().s("s").h("h").p("/p").z().uri();
        ne(iri1, iri2);

        // RFC 3986: normalization also removes redundant colon in authority
        test("s://:").s("s").h("").g(":").p("").ts("s://:").z()
                .rst().norm().s("s").h("").g("").p("/").ts("s:///").z();
        test("s://x:").s("s").h("x").g("x:").p("").ts("s://x:").z()
                .rst().norm().s("s").h("x").g("x").p("/").ts("s://x/").z();
        test("s://@:").s("s").u("").h("").g("@:").p("").ts("s://@:").z()
                .rst().norm().s("s").u("").h("").g("@").p("/").ts("s://@/").z();
        test("s://u@x:").s("s").u("u").h("x").g("u@x:").p("").ts("s://u@x:").z()
                .rst().norm().s("s").u("u").h("x").g("u@x").p("/").ts("s://u@x/").z();
        test("s://u@:").s("s").u("u").h("").g("u@:").p("").ts("s://u@:").z()
                .rst().norm().s("s").u("u").h("").g("u@").p("/").ts("s://u@/").z();
        test("s://:/").s("s").h("").g(":").p("/").ts("s://:/").z()
                .rst().norm().s("s").h("").g("").p("/").ts("s:///").z();
        test("s://x:/").s("s").h("x").g("x:").p("/").ts("s://x:/").z()
                .rst().norm().s("s").h("x").g("x").p("/").ts("s://x/").z();
        test("s://@:/").s("s").u("").h("").g("@:").p("/").ts("s://@:/").z()
                .rst().norm().s("s").u("").h("").g("@").p("/").ts("s://@/").z();
        test("s://u@x:/").s("s").u("u").h("x").g("u@x:").p("/").ts("s://u@x:/").z()
                .rst().norm().s("s").u("u").h("x").g("u@x").p("/").ts("s://u@x/").z();
        test("s://u@:/").s("s").u("u").h("").g("u@:").p("/").ts("s://u@:/").z()
                .rst().norm().s("s").u("u").h("").g("u@").p("/").ts("s://u@/").z();

        // normalization and paths ending with ..
        test("/..").norm().p("/").z();
        test("../../a/../").norm().p("../../").z();
        test("../../a/..").norm().p("../../").z();
        test("../..").norm().p("../..").z();
        test("..").norm().p("..").z();
        test("s:/..").norm().s("s").p("/").z();
        test("s:///..").norm().s("s").h("").g("").p("/").z();
        test("s:///a/..").norm().s("s").h("").g("").p("/").z();
        test("s:///a/b/..").norm().s("s").h("").g("").p("/a/").z();
        test("s:/a/..").norm().s("s").p("/").z();
        test("s:/a/b/..").norm().s("s").p("/a/").z();
        test("s:..").norm().s("s").p("..").z();

        // Relativization
        test("/a/b").p("/a/b").z()
            .rtvz(IRI.parseIRI("/a")).p("a/b").z();  // bug in java.net.URI
        test("/a/b").p("/a/b").z()
            .rtvz(IRI.parseIRI("/a/")).p("b").z();
        test("a/b").p("a/b").z()
            .rtvz(IRI.parseIRI("a")).p("a/b").z();   // bug in java.net.URI
        test("/a/b").p("/a/b").z()
            .rtvz(IRI.parseIRI("/a/b")).p("").z();   // Result is empty path
        test("a/../b:c/d").p("a/../b:c/d").z()
            .rtvz(IRI.parseIRI("./b:c/")).p("d").z();

        test("http://a/b/d/e?q#f")
                .s("http").h("a").p("/b/d/e").q("q").f("f").z()
                .rtvz(IRI.parseIRI("http://a/b/?r#g"))
                .p("d/e").q("q").f("f").z();

        test("http://a/b/d/e?q#f")                  // bug in java.net.URI
                .s("http").h("a").p("/b/d/e").q("q").f("f").z()
                .rtvz(IRI.parseIRI("http://a/b/g?r#g"))
                .p("d/e").q("q").f("f").z();

        // Resolution should preserve redundant colon in authority
        // RFC 3986 Section 5.2.1. says that normalization of the
        // base URI is optional, and Section 5.2.2 transfers the
        // authority unchanged.
        // The rule of least surprise suggests we should keep the
        // redundant colon if present - which can be removed by
        // calling normalize() later on if needed.

        // redundant colon in base URI authority
        test("a/b/c").p("a/b/c").z()
                .rst().rslv("s://")
                .s("s").g("").h("").p("/a/b/c").z();
        test("a/b/c").p("a/b/c").z()
                .rst().rslv("s://:")
                .s("s").g(":").h("").p("/a/b/c").z();
        test("a/b/c").p("a/b/c").z()
                .rst().rslv("s://:/")
                .s("s").g(":").h("").p("/a/b/c").z();
        test("a/b/c").p("a/b/c").z()
                .rst().rslv("s://:/x")
                .s("s").g(":").h("").p("/a/b/c").z();
        test("a/b/c").p("a/b/c").z()
                .rst().rslv("s://:/x/y")
                .s("s").g(":").h("").p("/x/a/b/c").z();
        test("a/b/c").p("a/b/c").z()
                .rst().rslv("s://@:")
                .s("s").g("@:").u("").h("").p("/a/b/c").z();
        test("a/b/c").p("a/b/c").z()
                .rst().rslv("s://@:/")
                .s("s").g("@:").u("").h("").p("/a/b/c").z();
        test("a/b/c").p("a/b/c").z()
                .rst().rslv("s://@:/x")
                .s("s").g("@:").u("").h("").p("/a/b/c").z();
        test("a/b/c").p("a/b/c").z()
                .rst().rslv("s://@:/x/y")
                .s("s").g("@:").u("").h("").p("/x/a/b/c").z();
        test("a/b/c").p("a/b/c").z()
                .rst().rslv("s://u@:")
                .s("s").g("u@:").u("u").h("").p("/a/b/c").z();
        test("a/b/c").p("a/b/c").z()
                .rst().rslv("s://u@:/")
                .s("s").g("u@:").u("u").h("").p("/a/b/c").z();
        test("a/b/c").p("a/b/c").z()
                .rst().rslv("s://u@:/x")
                .s("s").g("u@:").u("u").h("").p("/a/b/c").z();
        test("a/b/c").p("a/b/c").z()
                .rst().rslv("s://u@:/x/y")
                .s("s").g("u@:").u("u").h("").p("/x/a/b/c").z();
        test("a/b/c").p("a/b/c").z()
                .rst().rslv("s://u@h:")
                .s("s").g("u@h:").u("u").h("h").p("/a/b/c").z();
        test("a/b/c").p("a/b/c").z()
                .rst().rslv("s://u@h:/")
                .s("s").g("u@h:").u("u").h("h").p("/a/b/c").z();
        test("a/b/c").p("a/b/c").z()
                .rst().rslv("s://u@h:/x")
                .s("s").g("u@h:").u("u").h("h").p("/a/b/c").z();
        test("a/b/c").p("a/b/c").z()
                .rst().rslv("s://u@h:/x/y")
                .s("s").g("u@h:").u("u").h("h").p("/x/a/b/c").z();
        test("a/b/c").p("a/b/c").z()
                .rst().rslv("s://h:")
                .s("s").g("h:").h("h").p("/a/b/c").z();
        test("a/b/c").p("a/b/c").z()
                .rst().rslv("s://h:/")
                .s("s").g("h:").h("h").p("/a/b/c").z();
        test("a/b/c").p("a/b/c").z()
                .rst().rslv("s://h:/x")
                .s("s").g("h:").h("h").p("/a/b/c").z();
        test("a/b/c").p("a/b/c").z()
                .rst().rslv("s://h:/x/y")
                .s("s").g("h:").h("h").p("/x/a/b/c").z();

        // redundant colon in given URI authority
        test("///a/b/c").p("/a/b/c").g("").h("").z()
                .rst().rslv("s:")
                .s("s").g("").h("").p("/a/b/c").z();
        test("//:/a/b/c").p("/a/b/c").g(":").h("").z()
                .rst().rslv("s:")
                .s("s").g(":").h("").p("/a/b/c").z();
        test("//://a/b/c").p("//a/b/c").g(":").h("").z()
                .rst().rslv("s:")
                .s("s").g(":").h("").p("/a/b/c").z();
        test("//@:/a/b/c").p("/a/b/c").g("@:").u("").h("").z()
                .rst().rslv("s:")
                .s("s").g("@:").u("").h("").p("/a/b/c").z();
        test("//@://a/b/c").p("//a/b/c").g("@:").u("").h("").z()
                .rst().rslv("s:")
                .s("s").g("@:").u("").h("").p("/a/b/c").z();
        test("//u@:/a/b/c").p("/a/b/c").g("u@:").u("u").h("").z()
                .rst().rslv("s:")
                .s("s").g("u@:").u("u").h("").p("/a/b/c").z();
        test("//u@://a/b/c").p("//a/b/c").g("u@:").u("u").h("").z()
                .rst().rslv("s:")
                .s("s").g("u@:").u("u").h("").p("/a/b/c").z();
        test("//u@h:/a/b/c").p("/a/b/c").g("u@h:").u("u").h("h").z()
                .rst().rslv("s:")
                .s("s").g("u@h:").u("u").h("h").p("/a/b/c").z();
        test("//u@h://a/b/c").p("//a/b/c").g("u@h:").u("u").h("h").z()
                .rst().rslv("s:")
                .s("s").g("u@h:").u("u").h("h").p("/a/b/c").z();
        test("//h:/a/b/c").p("/a/b/c").g("h:").h("h").z()
                .rst().rslv("s:")
                .s("s").g("h:").h("h").p("/a/b/c").z();
        test("//h://a/b/c").p("//a/b/c").g("h:").h("h").z()
                .rst().rslv("s:")
                .s("s").g("h:").h("h").p("/a/b/c").z();

        // resolution against a base URI ending with ..
        test("c").rslv("s://h/a/b/..")
                .s("s").h("h").p("/a/c");
        test("c").rslv("s://h/a/b/../")
                .s("s").h("h").p("/a/c");
        test("c").rslv("..").p("../c");
        test("c").rslv("../..").p("../../c");
        test("c").rslv("../a/..").p("../c");

        // Some non intuitive corner cases. Some of these are not
        // handled correctly by java.net.URI - which is a bug.
        IRI abase = IRI.parseIRI("a");

        test("a/b").p("a/b").z().rtvz(abase).p("a/b").z();
        test("b")  .p("b")  .z().rtvz(abase).p("b")  .z();
        test("ab") .p("ab") .z().rtvz(abase).p("ab") .z();
        test("/b") .p("/b") .z().rtvz(abase).p("/b") .z();

        test("a/b").p("a/b").z().rslv(abase).p("a/b").z();
        test("b")  .p("b")  .z().rslv(abase).p("b")  .z();
        test("ab") .p("ab") .z().rslv(abase).p("ab") .z();
        test("/b") .p("/b") .z().rslv(abase).p("/b") .z();

        test("../b").p("../b").z().rtvz(abase).p("../b") .z();
        test("../b").p("../b").z().rslv(abase).p("../b") .z();
        test("../b").p("../b").z().rtvz(rbase).p("../b") .z();
        test("../b").p("../b").z().rslv(rbase).p("a/b/b").z();

        test("..").p("..").z().rtvz(abase).p("..")  .z();
        test("..").p("..").z().rslv(abase).p("..")  .z();
        test("..").p("..").z().rtvz(rbase).p("..")  .z();
        test("..").p("..").z().rslv(rbase).p("a/b/").z();

        IRI vbase1 = IRI.parseIRI("http://a/b/c/d;p?q");
        IRI vbase2 = IRI.parseIRI("a/b/c?q#f");
        IRI vbase3 = IRI.parseIRI("//a/b/c/d;p?q");

        test("//g").h("g").p("").rtvz(vbase1).h("g").p("").z();
        test("//g/x/y").h("g").p("/x/y").rtvz(vbase1).h("g").p("/x/y").z();
        test("#").f("").p("").rtvz(vbase1).p("").f("").z();
        test("").p("").rtvz(vbase1).p("").z();
        test("#s").p("").f("s").rtvz(vbase2).p("").f("s").z();
        test("http://a/b/c/d;p/../d;p?q").rtvz(vbase1).p("").q("q").z();
        test("http://a/b/c/d;p/../d?q").rtvz(vbase1).p("d").q("q").z();
        test("http://a/b/c/d;p/../d;p/b?q").rtvz(vbase1).p("d;p/b").q("q").z();
        test("http://a/b/c/d;p/../d/b?q").rtvz(vbase1).p("d/b").q("q").z();
        test("http://a/b/c/d;p/../../d/b?q").rtvz(vbase1)
                .s("http").h("a").p("/b/c/d;p/../../d/b").q("q").z();
        test("//a/b/c/d;p/../d;p/b?q").rtvz(vbase3).p("d;p/b").q("q").z();
        test("//a/b/c/d;p/../d/b?q").rtvz(vbase3).p("d/b").q("q").z();
        test("//a/b/c/d;p/../../d/b?q").rtvz(vbase3)
                .h("a").p("/b/c/d;p/../../d/b").q("q").z();

        // parseServerAuthority
        test("/a/b").psa().p("/a/b").z();
        test("s://u@h:1/p")
            .psa().s("s").u("u").h("h").n(1).p("/p").z();
        test("s://u@h:-foo/p").x().z();
        test("s://h:999999999999999999999999").x().z();
        test("s://:/b").psa().s("s").h("").p("/b").z();


        header("Constructors and factories");

        test("s", null, null, -1, "p", null, null).x().z();
        test(null, null, null, -1, null, null, null).p("").z();
        test(null, null, null, -1, "p", null, null).p("p").z();
        test(null, null, "foo%20bar", -1, null, null, null)
                .h("foo%20bar").hd("foo bar").p("").z();  // as per RFC3986
        test(null, null, "foo", -100, null, null, null).x().z();
        test("s", null, null, -1, "", null, null).s("s").p("").z();
        test("s", null, null, -1, "/p", null, null).s("s").p("/p").z();
        test("s", "u", "h", 10, "/p", "q", "f")
            .s("s").u("u").h("h").n(10).p("/p").q("q").f("f").z();
        test("s", "a:b", "/p", "q", "f").x().z();
        test("s", "h", "/p", "f")
            .s("s").h("h").p("/p").f("f").z();
        test("s", "p", "f").s("s").o("p").p("p").f("f").z();
        test("s", "/p", "f").s("s").p("/p").f("f").z();
        testCreate("s://u@h/p?q#f")
            .s("s").u("u").h("h").p("/p").q("q").f("f").z();
    }

    static void npes() throws URISyntaxException {

        header("NullPointerException");

        IRI base = IRI.of("mailto:root@foobar.com");

        out.println();

        try {
            base.resolve((IRI)null);
            throw new RuntimeException("NullPointerException not thrown");
        } catch (NullPointerException x) {
            out.println("resolve((IRI)null) -->");
            out.println("Correct exception: " + x);
        }

        out.println();

        try {
            base.resolve((String)null);
            throw new RuntimeException("NullPointerException not thrown");
        } catch (NullPointerException x) {
            out.println("resolve((String)null) -->");
            out.println("Correct exception: " + x);
        }

        out.println();

        try {
            base.relativize((IRI)null);
            throw new RuntimeException("NullPointerException not thrown");
        } catch (NullPointerException x) {
            out.println("relativize((String)null) -->");
            out.println("Correct exception: " + x);
        }

        testCount += 3;
    }

    static MethodType mt(Class<?> res, Object... args) {
        Class<?>[] cls = new Class[args.length];
        for (int i=0; i<args.length; i++) {
            // we only accept String and int as parameters.
            cls[i] = args[i] == null ? String.class : args[i].getClass();
        }
        return MethodType.methodType(res, cls).unwrap();
    }

    static MethodHandle lookupCreateHierarchical(MethodType mt) {
        try {
            return MethodHandles.lookup()
                    .findStatic(IRI.class, "createHierarchical", mt);
        } catch (Throwable t) { // should not happen
            throw new ExceptionInInitializerError(t);
        }
    }

    static final Map<MethodType, MethodHandle> createHierarchical = new ConcurrentHashMap<>();

    static IRI createHierarchical(Object... params) throws URISyntaxException {
        // Lookup either of IRI.createHierarchical methods
        MethodHandle mh = createHierarchical
                .computeIfAbsent(mt(IRI.class, params), Test::lookupCreateHierarchical);
        try {
            // Invoke IRI.createHierarchical
            return (IRI)mh.invokeWithArguments(params);
        } catch (URISyntaxException | RuntimeException | Error x) {
            throw x;
        } catch(Throwable x) {
            throw new RuntimeException("Unexpected exception: " + x, x);
        }

    }

    static void iaes() throws URISyntaxException {

        header("IllegalArgumentException");


        out.println();
        Object[][] iaes = {
                {"s", "userinfo", null, -1, "/p", null, null},
                {"s", null, null, 0, "/p", null, null},
                {"s", null, null, -1, "//p", null, null},
                {"s", null, "//p", null, null},
                {null, "userinfo", null, -1, "/p", null, null},
                {null, null, null, 0, "/p", null, null},
                {null, null, null, -1, "//p", null, null},
                {null, null, "//p", null, null},
                {null, null, null, -1, "a://b/c/d", null, null},
                {null, null, "a://b/c/d", null, null},
                {null, null, "", -1, "a://b/c/d", null, null},
                {null, "", "a://b/c/d", null, null},
                {null, "", "u@h:80/p", null, null},
                {null, "", "", -1, "u@h:80/p", null, null},
                {null, null, "", -1, "u@h:80/p", null, null},
                {"s", "", "u@h:80/p", null, null},
                {"s", "", "", -1, "u@h:80/p", null, null},
                {"s", null, "", -1, "u@h:80/p", null, null},
                {"s", "userinfo", "", -1, "a://b/c/d", null, null},
                {"s", null, "", 0, "a://b/c/d", null, null},
        };

        for (Object[] params : iaes) {
            try {
                String msg = "IRI.createHierarchical" + Arrays.asList(params).toString()
                        .replace("[","(").replace("]", ")");
                out.println(msg);
                IRI iri = createHierarchical(params);
                throw new RuntimeException(
                        "Expected IllegalArgumentException not raised for: " + msg);
            } catch (IllegalArgumentException x) {
                System.out.println("\t --> Got expected exception: " + x);
            }
            testCount++;
        }

        test("s", "a://b/c/d", null)
                .o("a://b/c/d")
                .p("a://b/c/d")
                .s("s").z();
        test("s", "", "/a://b/c/d", null)
                .p("/a://b/c/d").s("s").h("").z();
        test("s", "", "/a://b/c/d", null, null)
                .p("/a://b/c/d").s("s").h("").g("").z();
        test("s", null, "", -1, "/a://b/c/d", null, null)
                .p("/a://b/c/d").s("s").h("").g("").z();
        test(null, "", "/a://b/c/d", null)
                .p("/a://b/c/d").h("").z();
        test(null, "", "/a://b/c/d", null, null)
                .p("/a://b/c/d").h("").g("").z();
        test(null, null, "", -1, "/a://b/c/d", null, null)
                .p("/a://b/c/d").h("").g("").z();

     }


    static void chars() throws URISyntaxException {

        header("Escapes and non-US-ASCII characters");

        IRI uri;

        // Escape pairs
        test("%0a%0A%0f%0F%01%09zz")
            .p("%0a%0A%0f%0F%01%09zz").z();
        test("foo%1").x().z();
        test("foo%z").x().z();
        test("foo%9z").x().z();

        // Escapes not permitted in scheme, host
        test("s%20t://a").x().z();
        test("//a%20b").h("a%20b").p("").z();        // as in RFC3986

        // Escapes permitted in opaque part, userInfo, registry, path,
        // query, and fragment
        test("//u%20v@a").u("u%20v").h("a").p("").z();
        test("/p%20q").p("/p%20q").z();
        test("/p?q%20").p("/p").q("q%20").z();
        test("/p#%20f").p("/p").f("%20f").z();

        // Non-US-ASCII chars
        test("s\u00a7t://a").x().z();
        test("//\u00a7/b").h("\u00a7").p("/b").z();    // as in RFC3986
        test("//u\u00a7v@a").u("u\u00a7v").h("a").p("").z();
        test("/p\u00a7q").p("/p\u00a7q").z();
        test("/p?q\u00a7").p("/p").q("q\u00a7").z();
        test("/p#\u00a7f").p("/p").f("\u00a7f").z();

        // 4648111 - Escapes quoted by toString after resolution
        uri = IRI.parseIRI("http://a/b/c/d;p?q");
        test("/p%20p")
            .rslv(uri).s("http").h("a").p("/p%20p")
            .ts("http://a/p%20p")
            .ti("http://a/p%20p").z();

        test("/p%20p").rtvz(uri).p("/p%20p")
                .ts("/p%20p")
                .ti("/p%20p").z();

        test("/p%32p").rtvz(uri).p("/p%32p")
                .ts("/p%32p")
                .ti("/p2p").z();

        // 4464135: Forbid unwise characters throughout opaque part
        test("foo:x{bar").x().z();
        test("foo:{bar").x().z();

        // 4438319: Single-argument constructor requires quotation,
        //          preserves escapes
        test("//u%01@h/a/b/%02/c?q%03#f%04")
            .u("u%01").ud("u\1")
            .h("h")
            .p("/a/b/%02/c").pd("/a/b/\2/c")
            .q("q%03").qd("q\3")
            .f("f%04").fd("f\4")
            .z();
        test("/a/b c").x().z();

        // 4438319: Multi-argument constructors quote illegal chars and
        //          preserve legal non-ASCII chars
        // \uA001-\uA009 are visible characters, \u2000 is a space character
        test(null, "u\uA001\1", "h", -1,
             "/p% \uA002\2\u2000",
             "q% \uA003\3\u2000",
             "f% \uA004\4\u2000")
            .u("u\uA001%01").h("h")
            .p("/p%25%20\uA002%02%E2%80%80").pd("/p% \uA002\2\u2000")
            .q("q%25%20\uA003%03%E2%80%80").qd("q% \uA003\3\u2000")
            .f("f%25%20\uA004%04%E2%80%80").fd("f% \uA004\4\u2000").z();
        test(null, "g\uA001\1",
             "/p% \uA002\2\u2000",
             "q% \uA003\3\u2000",
             "f% \uA004\4\u2000")
            .h("g\uA001%01")                   // as in RFC3986
            .p("/p%25%20\uA002%02%E2%80%80").pd("/p% \uA002\2\u2000")
            .q("q%25%20\uA003%03%E2%80%80").qd("q% \uA003\3\u2000")
            .f("f%25%20\uA004%04%E2%80%80").fd("f% \uA004\4\u2000").z();
        test(null, null, "/p% \uA002\2\u2000", "f% \uA004\4\u2000")
            .p("/p%25%20\uA002%02%E2%80%80").pd("/p% \uA002\2\u2000")
            .f("f%25%20\uA004%04%E2%80%80").fd("f% \uA004\4\u2000").z();
        test(null, "/sp% \uA001\1\u2000", "f% \uA004\4\u2000")
            .sp("/sp%25%20\uA001%01%E2%80%80").spd("/sp% \uA001\1\u2000")
            .p("/sp%25%20\uA001%01%E2%80%80").pd("/sp% \uA001\1\u2000")
            .f("f%25%20\uA004%04%E2%80%80").fd("f% \uA004\4\u2000").z();

        // 4438319: Non-raw accessors decode all escaped octets
        test("/%25%20%E2%82%AC%E2%80%80")
            .p("/%25%20%E2%82%AC%E2%80%80").pd("/% \u20AC\u2000").z();

        // 4438319: toASCIIString
        test("/\uCAFE\uBABE")
            .p("/\uCAFE\uBABE").ta("/%EC%AB%BE%EB%AA%BE").z();

        // 4991359 and 4866303: bad quoting by defineSchemeSpecificPart()
        IRI base = IRI.parseIRI("http://host/foo%20bar/a/b/c/d");
        test ("resolve")
            .rslv(base).spd("//host/foo bar/a/b/c/resolve")
            .sp("//host/foo%20bar/a/b/c/resolve").s("http")
            .pd("/foo bar/a/b/c/resolve").h("host")
            .p("/foo%20bar/a/b/c/resolve").z();

        // 6773270: java.net.URI fails to escape u0000
        test("s", "a", "/\u0000", null)
            .s("s").p("/%00").h("a")
            .ta("s://a/%00").z();
    }


    static void eq0(IRI u, IRI v) throws URISyntaxException {
        testCount++;
        if (!u.equals(v))
            throw new RuntimeException("Not equal: " + u + " " + v);
        int uh = u.hashCode();
        int vh = v.hashCode();
        if (uh != vh)
            throw new RuntimeException("Hash codes not equal: "
                                       + u + " " + Integer.toHexString(uh) + " "
                                       + v + " " + Integer.toHexString(vh));
        out.println();
        out.println(u + " == " + v
                    + "  [" + Integer.toHexString(uh) + "]");
    }

    static void cmp0(IRI u, IRI v, boolean same)
        throws URISyntaxException
    {
        int c = u.compareTo(v);
        if ((c == 0) != same)
            throw new RuntimeException("Comparison inconsistent: " + u + " " + v
                                       + " " + c);
    }

    static void eq(IRI u, IRI v) throws URISyntaxException {
        eq0(u, v);
        cmp0(u, v, true);
    }

    static void eq(String expected, String actual) {
        if (expected == null && actual == null) {
            return;
        }
        if (expected != null && expected.equals(actual)) {
            return;
        }
        throw new AssertionError(String.format(
                "Strings are not equal: '%s', '%s'", expected, actual));
    }

    static void eqeq(IRI u, IRI v) {
        testCount++;
        if (u != v)
            throw new RuntimeException("Not ==: " + u + " " + v);
    }

    static void ne0(IRI u, IRI v) throws URISyntaxException {
        testCount++;
        if (u.equals(v))
            throw new RuntimeException("Equal: " + u + " " + v);
        out.println();
        out.println(u + " != " + v
                    + "  [" + Integer.toHexString(u.hashCode())
                    + " " + Integer.toHexString(v.hashCode())
                    + "]");
    }

    static void ne(IRI u, IRI v) throws URISyntaxException {
        ne0(u, v);
        cmp0(u, v, false);
    }

    static void lt(IRI u, IRI v) throws URISyntaxException {
        ne0(u, v);
        int c = u.compareTo(v);
        if (c >= 0) {
            show(u);
            show(v);
            throw new RuntimeException("Not less than: " + u + " " + v
                                       + " " + c);
        }
        out.println(u + " < " + v);
    }

    static void lt(String s, String t) throws URISyntaxException {
        lt(IRI.parseIRI(s), IRI.parseIRI(t));
    }

   static void gt(IRI u, IRI v) throws URISyntaxException {
        lt(v, u);
    }

    static void eqHashComp() throws URISyntaxException {

        header("Equality, hashing, and comparison");

        IRI o = IRI.parseIRI("mailto:foo@bar.com");
        IRI r = IRI.parseIRI("reg://some%20registry/b/c/d?q#f");
        IRI s = IRI.parseIRI("http://jag:cafebabe@java.sun.com:94/b/c/d?q#f");
        eq(o, o);
        lt(o, r);
        lt(s, o);
        lt(s, r);
        eq(o, IRI.parseIRI("MaILto:foo@bar.com"));
        gt(o, IRI.parseIRI("mailto:foo@bar.COM"));
        eq(r, IRI.parseIRI("rEg://some%20registry/b/c/d?q#f"));
        eq(r, IRI.parseIRI("reg://Some%20Registry/b/c/d?q#f"));   // as in RFC3986
        gt(r, IRI.parseIRI("reg://some%20registry/b/c/D?q#f"));
        eq(s, IRI.parseIRI("hTtP://jag:cafebabe@Java.Sun.COM:94/b/c/d?q#f"));
        gt(s, IRI.parseIRI("http://jag:CafeBabe@java.sun.com:94/b/c/d?q#f"));
        lt(s, IRI.parseIRI("http://jag:cafebabe@java.sun.com:94/b/c/d?r#f"));
        lt(s, IRI.parseIRI("http://jag:cafebabe@java.sun.com:94/b/c/d?q#g"));
        eq(IRI.parseIRI("http://host/a%00bcd"), IRI.parseIRI("http://host/a%00bcd"));
        ne(IRI.parseIRI("http://host/a%00bcd"), IRI.parseIRI("http://host/aZ00bcd"));
        eq0(IRI.parseIRI("http://host/abc%e2def%C3ghi"),
            IRI.parseIRI("http://host/abc%E2def%c3ghi"));

        lt("p", "s:p");
        lt("s:p", "T:p");
        lt("S:p", "t:p");
        lt("s:/p", "s:p");
        lt("s:p", "s:q");
        lt("s:p#f", "s:p#g");
        lt("s://u@h:1", "s://v@h:1");
        lt("s://u@h:1", "s://u@i:1");
        lt("s://u@h:1", "s://v@h:2");
        lt("s://a%20b", "s://a%20c");
        lt("s://a%20b", "s://aab");
        lt("s://A_", "s://AA");   // as in RFC3986
        lt("s:/p", "s:/q");
        lt("s:/p?q", "s:/p?r");
        lt("s:/p#f", "s:/p#g");

        lt("s://h", "s://h/p");
        lt("s://h/p", "s://h/p?q");

        // unnecessary percent-encoded octet
        IRI u1 = IRI.of("http://jag:cafebabe@java.sun.com:94/d%75rst/c/d?q#f");
        IRI u2 = IRI.createHierarchical(u1.getScheme(), u1.getUserInfo(),
                                        u1.getHostString(), u1.getPort(), u1.getPath(),
                                        u1.getQuery(), u1.getFragment());
        eq(u1, u2);

        // because IRI.comapreTo() is always called after IRI.equals() in eq(),
        // lt(), or gt(), this unit test fails to capture regression reported
        // by 6348515. So call cmp0() directly here.
        IRI uu1 = IRI.parseIRI("h://a/p");
        IRI uu2 = IRI.parseIRI("h://a/p");
        cmp0(uu1, uu2, true);

    }


    static void serial(IRI u) throws IOException, URISyntaxException {

        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        ObjectOutputStream oo = new ObjectOutputStream(bo);

        oo.writeObject(u);
        oo.close();

        ByteArrayInputStream bi = new ByteArrayInputStream(bo.toByteArray());
        ObjectInputStream oi = new ObjectInputStream(bi);
        try {
            Object o = oi.readObject();
            eq(u, (IRI)o);
        } catch (ClassNotFoundException x) {
            x.printStackTrace();
            throw new RuntimeException(x.toString());
        }

        testCount++;
    }

    static void serial() throws IOException, URISyntaxException {
        header("Serialization");

        serial(IRI.of("http://java.sun.com/jdk/1.4?release#beta"));
        serial(IRI.of("s://h/p").resolve("/long%20path/"));
    }


    static void urls() throws URISyntaxException {

        header("URLs");

        IRI uri;
        URL url;
        boolean caught = false;

        out.println();
        uri = IRI.parseIRI("http://a/p?q#f");
        try {
            url = uri.toURL();
        } catch (MalformedURLException x) {
            throw new RuntimeException(x.toString());
        }
        if (!url.toString().equals("http://a/p?q#f"))
            throw new RuntimeException("Incorrect URL: " + url);
        out.println(uri + " url --> " + url);

        out.println();
        uri = IRI.parseIRI("a/b");
        try {
            out.println(uri + " url --> ");
            url = uri.toURL();
        } catch (IllegalArgumentException x) {
            caught = true;
            out.println("Correct exception: " + x);
        } catch (MalformedURLException x) {
            caught = true;
            throw new RuntimeException("Incorrect exception: " + x);
        }
        if (!caught)
            throw new RuntimeException("Incorrect URL: " + url);

        out.println();
        uri = IRI.parseIRI("foo://bar/baz");
        caught = false;
        try {
            out.println(uri + " url --> ");
            url = uri.toURL();
        } catch (MalformedURLException x) {
            caught = true;
            out.println("Correct exception: " + x);
        } catch (IllegalArgumentException x) {
            caught = true;
            throw new RuntimeException("Incorrect exception: " + x);
        }
        if (!caught)
            throw new RuntimeException("Incorrect URL: " + url);

        testCount += 3;
    }

    static void utils() throws URISyntaxException {
        final String[] quoteTests = new String[] {
                // unquoted         quoted
                "%",                "%",
                "%2",               "%2",
                "%25",              "%2525",
                "%2x",              "%2x",
                "aaaaa%2x%2",       "aaaaa%2x%2",
                "aaa%aa%2x%2",      "aaa%25aa%2x%2",
                "aaa%Ba%2x%2%20%a", "aaa%25Ba%2x%2%2520%a",
                "%C0%AF",           "%25C0%25AF",
                "%EF%BF%BD",        "%25EF%25BF%25BD",
        };

        header("IRI.quoteEncodedOctets");

        out.println();
        for (int i=0; i<quoteTests.length; i+=2) {
            out.println("quoteEncodedOctets(\"" + quoteTests[i]
                    + "\") -> \"" + quoteTests[i + 1] + "\"");
            String quoted = IRI.quoteEncodedOctets(quoteTests[i]);
            if (!quoted.equals(quoteTests[i+1])) {
                out.println("    failed! produced \"" + quoted +"\"");
                throw new RuntimeException("IRI.quoteEncodedOctets failed for \""
                        + quoteTests[i] +"\"");
            }
            String unquoted = IRI.unquoteEncodedOctets(quoted, true);
            if (!unquoted.equals(quoteTests[i])) {
                out.println("    failed! unquoting produced \"" + unquoted +"\"");
                throw new RuntimeException("IRI.quoteEncodedOctets failed to unquote for \""
                        + quoteTests[i] +"\"");
            }
        }

        final String[] unquoteTests = new String[] {
                // quoted      unquoted   requoted       replaced
                "%25",         "%",       "%",           "%",
                "x%25",        "x%",      "x%",          "x%",
                "%25x",        "%x",      "%x",          "%x",
                "%2540",       "%40",     "%2540",       "%40",
                "%C0%AF",      "%C0%AF",  "%25C0%25AF",  "\ufffd\ufffd",
                "%C0%AF%40",   "%C0%AF@", "%25C0%25AF@", "\ufffd\ufffd@",
                "%41%40%42%x", "A@B%x",   "A@B%x",       "A@B%x",
                "%EF%BF%BD",   "\ufffd",  "\ufffd",      "\ufffd"
        };

        header("IRI.unquoteEncodedOctets");
        out.println();
        for (int i=0; i<unquoteTests.length; i+=4) {
            out.println("unquoteEncoded(\"" + unquoteTests[i] + "\") -> \""
                    + unquoteTests[i + 1] + "\" [\"" + unquoteTests[i+3] + "\"]");
            String unquoted = IRI.unquoteEncodedOctets(unquoteTests[i], false);
            if (!unquoted.equals(unquoteTests[i+1])) {
                out.println("    failed! produced \"" + unquoted +"\"");
                throw new RuntimeException("IRI.unquoteEncodedOctets failed for \""
                        + unquoteTests[i] +"\"");
            }
            String quoted = IRI.quoteEncodedOctets(unquoted);
            if (!quoted.equals(unquoteTests[i+2])) {
                out.println("    failed! quoting produced \"" + quoted +"\"");
                throw new RuntimeException("IRI.unquoteEncodedOctets failed to quote for \""
                        + unquoteTests[i] +"\"");
            }
            String dequoted = IRI.unquoteEncodedOctets(quoted, false);
            if (!dequoted.equals(unquoteTests[i+1])) {
                out.println("    failed! dequoting quoted produced \"" + dequoted +"\"");
                throw new RuntimeException("IRI.unquoteEncodedOctets failed to dequote for \""
                        + unquoteTests[i] +"\"");
            }
            String ident = IRI.unquoteEncodedOctets(IRI.quoteEncodedOctets(unquoteTests[i]), true);
            if (!ident.equals(unquoteTests[i])) {
                out.println("    failed! ident produced \"" + ident +"\"");
                throw new RuntimeException("IRI.unquoteEncodedOctets failed ident for \""
                        + unquoteTests[i] +"\"");
            }
            String replaced = IRI.unquoteEncodedOctets(unquoteTests[i], true);
            if (!replaced.equals(unquoteTests[i+3])) {
                out.println("    failed! replaced produced \"" + replaced +"\"");
                throw new RuntimeException("IRI.unquoteEncodedOctets failed replaced for \""
                        + unquoteTests[i] +"\"");
            }
        }
    }

    static String auth(String u, String h, int port) {
        StringBuilder sb = new StringBuilder();
        if (u != null) sb.append(u).append('@');
        if (h != null) sb.append(h);
        if (port >= 0) sb.append(':').append(port);
        return sb.toString();
    }

    static void lenient() {
        header("Lenient parsing (tests for IRI.parseLenient)");

        for (char c : "|<> {}^`\"\\".toCharArray()) {
            String ld = String.valueOf(c);
            String l = appendEscape(new StringBuilder(), c).toString();
            String s = "s", u="u", ue = "u" + l, ud = "u" + ld;
            String h = "h", he = h + l, hd = h + ld;
            int port = 80;
            String p = "/p", pe = p + l, pd = p + ld;
            String q = "q=q", qe = q + l, qd = q + ld;
            String f = "f", fe = f+ l, fd = f + ld;
            String a = auth(u, h, port);

            String ae = auth(ue, h, port), ad = auth(ud, h, port);
            String in = String.format("%s://%s%s?%s#%s", s, ad, p, q, f);

            lenient(in).s("s")
                    .u(ue).ud(ud).h(h).hd(h).n(port)
                    .g(ae).gd(ad)
                    .p(p).pd(p)
                    .q(q).qd(q).f(f).fd(f)
                    .tl(in)
                    .z();

            ae = auth(u, he, port); ad = auth(u, hd, port);
            in = String.format("%s://%s%s?%s#%s", s, ad, p, q, f);

            lenient(in).s("s")
                    .u(u).ud(u).h(he).hd(hd).n(port)
                    .g(ae).gd(ad)
                    .p(p).pd(p)
                    .q(q).qd(q).f(f).fd(f)
                    .tl(in)
                    .z();

            in = String.format("%s://%s%s?%s#%s", s, a, pd, q, f);

            lenient(in).s("s")
                    .u(u).ud(u).h(h).hd(h).n(port)
                    .g(a).gd(a)
                    .p(pe).pd(pd)
                    .q(q).qd(q).f(f).fd(f)
                    .tl(in)
                    .z();

            in = String.format("%s://%s%s?%s#%s", s, a, p, qd, f);

            lenient(in).s("s")
                    .u(u).ud(u).h(h).hd(h).n(port)
                    .g(a).gd(a)
                    .p(p).pd(p)
                    .q(qe).qd(qd).f(f).fd(f)
                    .tl(in)
                    .z();

            in = String.format("%s://%s%s?%s#%s", s, a, p, q, fd);

            lenient(in).s("s")
                    .u(u).ud(u).h(h).hd(h).n(port)
                    .g(a).gd(a)
                    .p(p).pd(p)
                    .q(q).qd(q).f(fe).fd(fd)
                    .tl(in)
                    .z();

            ae = auth(ue, he, port); ad = auth(ud, hd, port);
            in = String.format("%s://%s%s?%s#%s", s, ad, pd, qd, fd);

            lenient(in).s("s")
                    .u(ue).ud(ud).h(he).hd(hd).n(port)
                    .g(ae).gd(ad)
                    .p(pe).pd(pd)
                    .q(qe).qd(qd).f(fe).fd(fd)
                    .tl(in)
                    .z();

            s = "mailto"; p="d$$.$$f@o$$.c";
            pe=p.replace("$$", l); pd=p.replace("$$", ld);
            in = String.format("%s:%s", s, pd);

            lenient(in).s(s).o(pd).sp(pe).p(pe).pd(pd).tl(in).z();

        }

        String in = "s://h/p?q=%41|B&%25%34%33";
        lenient(in).s("s").h("h").p("/p")
                .q("q=%41%7CB&%25%34%33")
                .qd("q=A|B&%43")
                .tl("s://h/p?q=%41|B&%25%34%33").z();

        // no funny characters allowed in scheme, even when parsed
        // leniently...
        lenient("s|s://h/p").x().z();

    }

    // ... the printable characters in US-ASCII that are not allowed in
    // URIs, namely {@code '<'}, {@code '>'}, {@code '"'}, space,
    // {@code '{'}, {@code '}'}, {@code '|'}, {@code '\'}, {@code '^'},
    // and {@code '`'}.
    static final String[][] IPUAC = {
            { "<",  "%3C" },
            { ">",  "%3E" },
            { "\"", "%22" },
            { " ",  "%20" },
            { "{",  "%7B" },
            { "}",  "%7D" },
            { "|",  "%7C" },
            { "\\", "%5C" },
            { "^",  "%5E" },
            { "`",  "%60" } };

    // A number of prefixes and suffixes to test IPUAC's with
    static final String[][] PRE_SUF_FIXES = {
            { "",    ""    },
            { "a",   ""    },
            { "",    "b"   },
            { "a",   "b"   },
            { "%",   ""    },
            { "" ,   "%"   },
            { "%",   "%"   },
            { "%X",  ""    },
            { "" ,   "%Y"  },
            { "%Z",  "%Z"  },
            { "%XX", ""    },
            { "" ,   "%YY" },
            { "%ZZ", "%ZZ" },
            { "%41", ""    },
            { "" ,   "%42" },
            { "%43", "%43" },
    };

    static void lenientUtilities() {
        header("Lenient utilities");

        eq(null, IRI.quoteLenient(null));
        eq(null, IRI.unquoteLenient(null));
        eq("", IRI.quoteLenient(""));
        eq("", IRI.unquoteLenient(""));

        for (String[] values : IPUAC) {
            for (String[] preSuf : PRE_SUF_FIXES) {
                String raw = preSuf[0] + values[0] + preSuf[1];
                String encoded = preSuf[0] + values[1] + preSuf[1];

                out.format("raw: %8s, encoded: %s%n", raw, encoded);

                eq(encoded, IRI.quoteLenient(raw));
                eq(raw, IRI.unquoteLenient(encoded));
                eq(raw, IRI.unquoteLenient(IRI.quoteLenient(raw)));
            }
        }
    }

    static void specials() {
        header("Special chars U+FFF0-U+FFFD");
        for (char c='\ufff0'; c <= '\ufffd'; c++) {
            String hex = appendEscape(new StringBuilder(9), c)
                    .toString();
            header("char U+FFF" + hexDigits[c & 0x000F]);
            test("s://h/p?q="+c).x().z();
            test("s" + c, "h", "/p", null, null)
                    .x().z();
            test("s", "h", "/p", "q=q", "f"+c)
                    .s("s").h("h").p("/p").q("q=q")
                    .fd("f" + c)
                    .f("f" + hex)
                    .z();
            test("s", "h", "/p", "q=" + c, null)
                    .s("s").h("h").p("/p")
                    .qd("q=" + c)
                    .q("q=" + hex)
                    .z();
            test("s", "h" + c, "/p", null, null)
                    .s("s").hd("h" + c)
                    .h("h" + hex)
                    .p("/p")
                    .z();
            test("s", "h", "/p" + c, null, null)
                    .s("s").h("h")
                    .p("/p" + hex)
                    .pd("/p" + c)
                    .z();
        }
    }

    static void tests() throws IOException, URISyntaxException {
        iaes();
        rfc2396();
        rfc3986();
        ip();
        misc();
        chars();
        eqHashComp();
        serial();
        urls();
        npes();
        bugs();
        utils();
        specials();
        lenient();
        lenientUtilities();
    }


    // -- Command-line invocation --

    static void usage() {
        out.println("Usage:");
        out.println("  java Test               --  Runs all tests in this file");
        out.println("  java Test <uri>         --  Parses uri, shows components");
        out.println("  java Test <base> <uri>  --  Parses uri and base, then resolves");
        out.println("                              uri against base");
    }

    static void clargs(String base, String uri) {
        IRI b = null, u;
        try {
            if (base != null) {
                b = IRI.parseIRI(base);
                out.println(base);
                show(b);
            }
            u = IRI.parseIRI(uri);
            out.println(uri);
            show(u);
            if (base != null) {
                IRI r = b.resolve(u);
                out.println(r);
                show(r);
            }
        } catch (URISyntaxException x) {
            show("ERROR", x);
            x.printStackTrace(out);
        }
    }


    // miscellaneous bugs/rfes that don't fit in with the test framework

    static void bugs() {
        b7023363();
        b6339649();
        b6933879();
        b8037396();
    }

    // 6339649 - include detail message from nested exception
    private static void b6339649() {
        try {
            IRI uri = IRI.of("http://nowhere.net/should not be permitted");
        } catch (IllegalArgumentException e) {
            if ("".equals(e.getMessage()) || e.getMessage() == null) {
                throw new RuntimeException ("No detail message");
            }
        }
    }

    //  7023363: URI("ftp", "[www.abc.com]", "/dir1/dir2", "query", "frag")
    //           should throw URISyntaxException
    private static void b7023363() {
        // [www.abc.com] is not a legal IPv6 litteral.
        test("ftp://[www.abc.com]/dir1/dir2?query#frag")
                .x().z();

        // as per RFC 3986 "%5Bwww.abc.com%5D" is a legal hostname.
        test("ftp", "[www.abc.com]", "/dir1/dir2", "query", "frag")
                .s("ftp").hd("[www.abc.com]").h("%5Bwww.abc.com%5D")
                .p("/dir1/dir2").q("query").f("frag").z();

        // If there is a colon enclosed in the [ ... ] then this will be interpreted
        // as an IPv6 literal and an exception will be raised.
        // This is arguable.
        test("ftp", "[www.a:bc.com]", "/dir1/dir2", "query", "frag")
                .x().z();
    }

    // 6933879 - check that "." and "_" characters are allowed in IPv6 scope_id.
    private static void b6933879() {
        final String HOST = "fe80::c00:16fe:cebe:3214%eth1.12_55";
        IRI uri;
        try {
            uri = IRI.createHierarchical("http", null, HOST, 10, "/", null, null);
        } catch (URISyntaxException ex) {
            throw new AssertionError("Should not happen", ex);
        }
        eq("[" + HOST + "]", uri.getHostString());
    }

    private static void b8037396() {

        // primary checks:

        IRI u;
        try {
            u = IRI.createHierarchical("http", "example.org", "/[a b]", "[a b]", "[a b]");
        } catch (URISyntaxException e) {
            throw new AssertionError("shouldn't ever happen", e);
        }
        eq("/[a b]", u.getPath());
        eq("[a b]", u.getQuery());
        eq("[a b]", u.getFragment());

        // additional checks:
        //  *   '%' symbols are still decoded outside square brackets
        //  *   the getRawXXX() functionality left intact

        try {
            u = IRI.createHierarchical("http", "example.org", "/a b[c d]", "a b[c d]", "a b[c d]");
        } catch (URISyntaxException e) {
            throw new AssertionError("shouldn't ever happen", e);
        }

        eq("/a b[c d]", u.getPath());
        eq("a b[c d]", u.getQuery());
        eq("a b[c d]", u.getFragment());

        eq("/a%20b%5Bc%20d%5D", u.getRawPath());

        // RFC3986: [ ] are gen-delim characters and should be escaped
        // in query & fragment too
        eq("a%20b%5Bc%20d%5D", u.getRawQuery());
        eq("a%20b%5Bc%20d%5D", u.getRawFragment());
    }

    public static void main(String[] args) throws Exception {
        switch (args.length) {

        case 0:
            tests();
            out.println();
            out.println("Test cases: " + testCount);
            break;

        case 1:
            if (args[0].equals("-help")) {
                usage();
                break;
            }
            clargs(null, args[0]);
            break;

        case 2:
            clargs(args[0], args[1]);
            break;

        default:
            usage();
            break;

        }
    }

}
