package org.xbib.net.resource;
/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 */

import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class BuilderTest {

    static final boolean DEBUG = false; // additional verbose info if true

    // The component constants are defined in the main class rather than
    // in the component class for conciseness purposes.
    public static final Component<String> SCHEME =
            new Component<>("scheme", IRI::getScheme, IRI::getScheme, IRI.Builder::scheme);
    public static final Component<String> AUTHORITY =
            new Component<>("authority", IRI::getRawAuthority, IRI::getAuthority, IRI.Builder::authority,
                    null, "host", "user", "port");
    public static final Component<String> USER =
            new Component<>("user", IRI::getRawUserInfo, IRI::getUserInfo, IRI.Builder::userinfo,
                    null, "authority");
    public static final Component<String> HOST =
            new Component<>("host", IRI::getRawHostString, IRI::getHostString, IRI.Builder::host,
                    null, "authority");
    public static final Component<Integer> PORT =
            new Component<Integer>("port", IRI::getPort, IRI::getPort, IRI.Builder::port,
                    -1, "authority");
    public static final Component<String> PATH =
            new Component<>("path", IRI::getRawPath, IRI::getPath, IRI.Builder::path,
                    null, "opaque");
    // Opaque is defined as having path as an alias, because if opaque was supplied then
    // IRI::getPath will return it. We also have special getter so that OPAQUE::get will
    // return null if the IRI is not opaque.
    public static final Component<String> OPAQUE =
            new Component<>("opaque", PATH,
                    Component::rawOpaque, Component::opaque, IRI.Builder::opaque,
                    null, "authority", "host", "user", "port", "path");
    public static final Component<String> QUERY =
            new Component<>("query", IRI::getRawQuery, IRI::getQuery, IRI.Builder::query);
    public static final Component<String> FRAGMENT =
            new Component<>("fragment", IRI::getRawFragment, IRI::getFragment, IRI.Builder::fragment);

    public static Test test(long id) { return new Test(id); }
    public static Test test(long id, String iri) {
        return new Test(id, () -> IRI.of(iri).with(IRI.Builder.DEFAULT_CAPABILITY));
    }
    public static Test quoting(long id) { return new Test(id, BuilderTest::quotingBuilder); }
    public static Test quoting(long id, String iri) {
        return new Test(id, () -> IRI.of(iri).with(IRI.Builder.QUOTE_ENCODED_CAPABILITY));
    }
    public static IRI.Builder quotingBuilder() {
        return IRI.newBuilder(IRI.Builder.QUOTE_ENCODED_CAPABILITY);
    }

    static final Test[] positive  = {
            test(0).set(SCHEME, "s").set(HOST, "h").set(PATH, "/p")
                    .check(AUTHORITY, "h")
                    .expect("s://h/p"),
            test(1).set(SCHEME, "s").set(HOST, "h").set(PATH, "/p")
                    .set(QUERY, "").set(FRAGMENT, "")
                    .check(AUTHORITY, "h")
                    .expect("s://h/p?#"),
            test(2).set(SCHEME, "s").set(HOST, "h").set(USER, "u").set(PORT, 42).set(PATH, "/p")
                    .set(QUERY, "q").set(FRAGMENT, "f")
                    .check(AUTHORITY, "u@h:42")
                    .expect("s://u@h:42/p?q#f"),
            test(3).set(SCHEME, "s").set(HOST, "h").set(USER, "u").set(PORT, 42).set(PATH, "/p")
                    .check(AUTHORITY, "u@h:42")
                    .expect("s://u@h:42/p"),
            test(4).set(SCHEME, "s").set(HOST, "h").set(PATH, "/p")
                    .set(AUTHORITY, "ha") // erase host, user, port
                    .check(HOST, "ha")
                    .expect("s://ha/p"), // authority has 'replaced' host
            test(5).set(SCHEME, "s").set(HOST, "h").set(PATH, "/p")
                    .set(USER, "u").set(PORT, 0)
                    .set(AUTHORITY, "ha") // erase host, user, port
                    .check(HOST, "ha") // authority has 'replaced' host
                    .expect("s://ha/p"),
            test(6).set(SCHEME, "s").set(HOST, "h").set(PATH, "/p")
                    .set(AUTHORITY, "u@ha:0")  // erase host, user, port
                    .check(HOST, "ha")   // authority has 'replaced' host
                    .check(PORT, 0)      // authority has 'replaced' port
                    .check(USER, "u")    // authority has 'replaced' user
                    .expect("s://u@ha:0/p"),
            test(7).set(HOST, "h").set(SCHEME, "s").set(PATH, "/p").set(AUTHORITY, "u@ha:0")
                    .set(OPAQUE, "blah")  // erase host, port, user, authority, path
                    .expect("s:blah"),
            test(8).set(HOST, "h").set(SCHEME, "s").set(PATH, "/p").set(AUTHORITY, "u@ha:0")
                    .set(OPAQUE, "blah") // erase host, port, user, authority, path
                    .check(PATH, "blah")  // needless: should already be tested...
                    .expect("s:blah"),
            test(9).set(SCHEME, "s").set(HOST, "h").set(USER, "I").set(PORT, 42).set(PATH, "/p")
                    .set(AUTHORITY, "u@ha:0")  // erase host, user, port
                    .check(HOST, "ha")   // authority has 'replaced' host
                    .check(PORT, 0)      // authority has 'replaced' port
                    .check(USER, "u")    // authority has 'replaced' user
                    .expect("s://u@ha:0/p"),
            test(10).set(SCHEME, "s1").set(HOST, "h").set(USER, "I").set(PORT, 42).set(PATH, "/p")
                  .set(SCHEME, "s").set(HOST, "h1")
                  .check(AUTHORITY, "I@h1:42")
                  .expect("s://I@h1:42/p"),
            test(11).set(SCHEME, "s1").set(HOST, "h").set(USER, "I").set(PORT, 42).set(PATH, "/p")
                    .set(SCHEME, "s").set(HOST, "h1").set(PORT, -1)
                    .check(AUTHORITY, "I@h1")
                    .expect("s://I@h1/p"),
            test(12).set(SCHEME, "s1").set(HOST, "h").set(USER, "I").set(PORT, 42).set(PATH, "/p")
                    .set(QUERY, null)
                    .set(SCHEME, "s").set(HOST, "h1").set(PORT, -1)
                    .check(AUTHORITY, "I@h1")
                    .expect("s://I@h1/p"),
            test(13).set(SCHEME, "s1").set(HOST, "h").set(USER, "I").set(PORT, 42).set(PATH, "/p")
                    .set(QUERY, "q")
                    .set(SCHEME, "s").set(HOST, "h1").set(PORT, -1)
                    .check(AUTHORITY, "I@h1")
                    .expect("s://I@h1/p?q"),
            test(14).set(SCHEME, "s1").set(HOST, "h").set(USER, "I").set(PORT, 42).set(PATH, "/p")
                    .set(QUERY, "q")
                    .set(SCHEME, "s").set(HOST, "h1").set(PORT, -1)
                    .set(QUERY, null).set(FRAGMENT, "f")
                    .check(AUTHORITY, "I@h1")
                    .expect("s://I@h1/p#f"),
            test(15).set(SCHEME, "s1").set(HOST, "h").set(USER, "I").set(PORT, 42).set(PATH, "/p")
                    .set(QUERY, "q").set(FRAGMENT, "f")
                    .set(SCHEME, "s").set(HOST, "h1").set(PORT, -1)
                    .set(QUERY, null).set(FRAGMENT, null)
                    .check(AUTHORITY, "I@h1")
                    .expect("s://I@h1/p"),
            test(16).set(SCHEME, "s").set(HOST, "h").set(USER, "u").set(PORT, 42).set(PATH, "/p")
                    .set(QUERY, "q").set(FRAGMENT, "f")
                    .set(SCHEME, null).set(HOST, null).set(USER, null).set(PORT, -1).set(PATH, null)
                    .set(QUERY, null).set(FRAGMENT, null)
                    .check(PATH, "")
                    .expect(""),
            test(17).set(SCHEME, "s").set(QUERY, "q").set(FRAGMENT, "f")
                    .set(OPAQUE, "blah")
                    .expect("s:blah?q#f"),
            test(18).set(SCHEME, "s").set(QUERY, "q").set(FRAGMENT, "f")
                    .set(OPAQUE, "blah?r")
                    .raw(OPAQUE, "blah%3Fr")
                    .expect("s:blah%3Fr?q#f")
                    .ti("s:blah%3Fr?q#f"),

            // encoded
            test(19).set(SCHEME, "s").set(HOST, "h:%3a%42").set(USER, "u@%41").set(PORT, 42)
                    .set(PATH, "/p%2f%42").set(QUERY, "q[%40]q").set(FRAGMENT, "f[%42]f")
                    .raw(USER, "u%40%41").check(USER, "u@A")
                    .raw(HOST, "h%3A%3a%42").check(HOST, "h::B")
                    .raw(PATH, "/p%2f%42").check(PATH, "/p%2FB")
                    .raw(AUTHORITY, "u%40%41@h%3A%3a%42:42")
                    .check(AUTHORITY, "u%40A@h%3A%3AB:42")
                    .raw(QUERY, "q%5B%40%5Dq").check(QUERY, "q[@]q")
                    .raw(FRAGMENT, "f%5B%42%5Df").check(FRAGMENT, "f[B]f")
                    .expect("s://u%40%41@h%3A%3a%42:42/p%2f%42?q%5B%40%5Dq#f%5B%42%5Df"),

            // encoded: edge case in host name: %25%34%31 => %41 => A
            // we check that getHost() will have %41 for the sequence %25%34%31,
            // but authority and toIRIString should have %2541 - that is the % character
            // will remain encoded there to avoid the %41 sequence to be further
            // decoded into A at the next iteration - the preserving the toIRIString
            // invariant.
            test(20).set(SCHEME, "s").set(HOST, "h:%3a%25%34%31%42").set(USER, "u@%41").set(PORT, 42)
                    .set(PATH, "/p%2f%42").set(QUERY, "q[%40]q").set(FRAGMENT, "f[%42]f")
                    .raw(USER, "u%40%41").check(USER, "u@A")
                    .raw(HOST, "h%3A%3a%25%34%31%42").check(HOST, "h::%41B")
                    .raw(PATH, "/p%2f%42").check(PATH, "/p%2FB")
                    .raw(AUTHORITY, "u%40%41@h%3A%3a%25%34%31%42:42")
                    .check(AUTHORITY, "u%40A@h%3A%3A%2541B:42")
                    .raw(QUERY, "q%5B%40%5Dq").check(QUERY, "q[@]q")
                    .raw(FRAGMENT, "f%5B%42%5Df").check(FRAGMENT, "f[B]f")
                    .expect("s://u%40%41@h%3A%3a%25%34%31%42:42/p%2f%42?q%5B%40%5Dq#f%5B%42%5Df")
                    .ti("s://u%40A@h%3A%3A%2541B:42/p%2FB?q%5B@%5Dq#f%5BB%5Df"),

            // check invalid sequences in the middle of valid sequences.
            // check that %25%34%31 translate into %2541 in the IRI string, not as %41
            test(21, "http://foo.com/with%25%34%31%C0%AF%25%34%310xC0AF%EC%82%AF\uc0af")
                    .check(SCHEME, "http")
                    .check(HOST, "foo.com")
                    .check(AUTHORITY, "foo.com")
                    .check(PATH, "/with%41%C0%AF%410xC0AF\uc0af\uc0af")
                    .raw(PATH, "/with%25%34%31%C0%AF%25%34%310xC0AF%EC%82%AF\uc0af")
                    .ti("http://foo.com/with%2541%C0%AF%25410xC0AF\uc0af\uc0af"),

            // encoded with quoting
            quoting(22).set(SCHEME, "s").set(HOST, "h:%3a%42").set(USER, "u@%41").set(PORT, 42)
                    .set(PATH, "/p%2f%42").set(QUERY, "q[%40]q").set(FRAGMENT, "f[%42]f")
                    .raw(USER, "u%40%2541").check(USER, "u@%41")
                    .raw(HOST, "h%3A%253a%2542").check(HOST, "h:%3a%42")
                    .raw(PATH, "/p%252f%2542").check(PATH, "/p%2f%42")
                    .raw(AUTHORITY, "u%40%2541@h%3A%253a%2542:42")
                    .check(AUTHORITY, "u%40%2541@h%3A%253a%2542:42")
                    .raw(QUERY, "q%5B%2540%5Dq").check(QUERY, "q[%40]q")
                    .raw(FRAGMENT, "f%5B%2542%5Df").check(FRAGMENT, "f[%42]f")
                    .expect("s://u%40%2541@h%3A%253a%2542:42/p%252f%2542?q%5B%2540%5Dq#f%5B%2542%5Df"),

            // build from IRI, don't change any value, check nothing changed.
            quoting(23,"s://u%40%41@h%3A%3a%42:42/p%2f%42?q%5B%40%5Dq#f%5B%42%5Df")
                    .check(SCHEME, "s")
                    .check(PORT, 42)
                    .raw(USER, "u%40%41").check(USER, "u@A")
                    .raw(HOST, "h%3A%3a%42").check(HOST, "h::B")
                    .raw(PATH, "/p%2f%42").check(PATH, "/p%2FB")
                    .raw(AUTHORITY, "u%40%41@h%3A%3a%42:42")
                    .check(AUTHORITY, "u%40A@h%3A%3AB:42")
                    .raw(QUERY, "q%5B%40%5Dq").check(QUERY, "q[@]q")
                    .raw(FRAGMENT, "f%5B%42%5Df").check(FRAGMENT, "f[B]f")
                    .expect("s://u%40%41@h%3A%3a%42:42/p%2f%42?q%5B%40%5Dq#f%5B%42%5Df")
                    .ti("s://u%40A@h%3A%3AB:42/p%2FB?q%5B@%5Dq#f%5BB%5Df"),

            // Same as case 23, but change host
            quoting(24,"s://u%40%41@h%3A%3a%42:42/p%2f%42?q%5B%40%5Dq#f%5B%42%5Df")
                    .set(HOST, "h:%3a%42")  // change host => host will be quoted
                    .check(PORT, 42)
                    .check(SCHEME, "s")
                    .raw(USER, "u%40%41").check(USER, "u@A")
                    .raw(HOST, "h%3A%253a%2542").check(HOST, "h:%3a%42")
                    .raw(PATH, "/p%2f%42").check(PATH, "/p%2FB")
                    .raw(AUTHORITY, "u%40%41@h%3A%253a%2542:42")
                    .check(AUTHORITY, "u%40A@h%3A%253a%2542:42")
                    .raw(QUERY, "q%5B%40%5Dq").check(QUERY, "q[@]q")
                    .raw(FRAGMENT, "f%5B%42%5Df").check(FRAGMENT, "f[B]f")
                    .expect("s://u%40%41@h%3A%253a%2542:42/p%2f%42?q%5B%40%5Dq#f%5B%42%5Df")
                    .ti("s://u%40A@h%3A%253a%2542:42/p%2FB?q%5B@%5Dq#f%5BB%5Df"),

            // Same as case 23, but change user
            quoting(25,"s://u%40%41@h%3A%3a%42:42/p%2f%42?q%5B%40%5Dq#f%5B%42%5Df")
                    .set(USER, "u%40@%41")  // change user => user will be quoted
                    .check(SCHEME, "s")
                    .check(PORT, 42)
                    .raw(USER, "u%2540%40%2541").check(USER, "u%40@%41")
                    .raw(HOST, "h%3A%3a%42").check(HOST, "h::B")
                    .raw(PATH, "/p%2f%42").check(PATH, "/p%2FB")
                    .raw(AUTHORITY, "u%2540%40%2541@h%3A%3a%42:42")
                    .check(AUTHORITY, "u%2540%40%2541@h%3A%3AB:42") // TODO: is that OK?
                    .raw(QUERY, "q%5B%40%5Dq").check(QUERY, "q[@]q")
                    .raw(FRAGMENT, "f%5B%42%5Df").check(FRAGMENT, "f[B]f")
                    .expect("s://u%2540%40%2541@h%3A%3a%42:42/p%2f%42?q%5B%40%5Dq#f%5B%42%5Df")
                    .ti("s://u%2540%40%2541@h%3A%3AB:42/p%2FB?q%5B@%5Dq#f%5BB%5Df"),

            // Same as case 23, but change path
            quoting(26,"s://u%40%41@h%3A%3a%42:42/p%2f%42?q%5B%40%5Dq#f%5B%42%5Df")
                    .set(PATH, "/p%2f%42")  // change path => path will be quoted
                    .check(SCHEME, "s")
                    .check(PORT, 42)
                    .raw(USER, "u%40%41").check(USER, "u@A")
                    .raw(HOST, "h%3A%3a%42").check(HOST, "h::B")
                    .raw(PATH, "/p%252f%2542").check(PATH, "/p%2f%42")
                    .raw(AUTHORITY, "u%40%41@h%3A%3a%42:42")
                    .check(AUTHORITY, "u%40A@h%3A%3AB:42")
                    .raw(QUERY, "q%5B%40%5Dq").check(QUERY, "q[@]q")
                    .raw(FRAGMENT, "f%5B%42%5Df").check(FRAGMENT, "f[B]f")
                    .expect("s://u%40%41@h%3A%3a%42:42/p%252f%2542?q%5B%40%5Dq#f%5B%42%5Df")
                    .ti("s://u%40A@h%3A%3AB:42/p%252f%2542?q%5B@%5Dq#f%5BB%5Df"),

            // Same as case 23, but change query
            quoting(27,"s://u%40%41@h%3A%3a%42:42/p%2f%42?q%5B%40%5Dq#f%5B%42%5Df")
                    .set(QUERY, "q%5B[%40@%41]%5Dq")  // change query => query will be quoted
                    .check(SCHEME, "s")
                    .check(PORT, 42)
                    .raw(USER, "u%40%41").check(USER, "u@A")
                    .raw(HOST, "h%3A%3a%42").check(HOST, "h::B")
                    .raw(PATH, "/p%2f%42").check(PATH, "/p%2FB")
                    .raw(AUTHORITY, "u%40%41@h%3A%3a%42:42")
                    .check(AUTHORITY, "u%40A@h%3A%3AB:42")
                    .raw(QUERY, "q%255B%5B%2540@%2541%5D%255Dq")
                    .check(QUERY, "q%5B[%40@%41]%5Dq")
                    .raw(FRAGMENT, "f%5B%42%5Df").check(FRAGMENT, "f[B]f")
                    .expect("s://u%40%41@h%3A%3a%42:42/p%2f%42?q%255B%5B%2540@%2541%5D%255Dq#f%5B%42%5Df")
                    .ti("s://u%40A@h%3A%3AB:42/p%2FB?q%255B%5B%2540@%2541%5D%255Dq#f%5BB%5Df"),

            // Same as case 23, but change fragment
            quoting(28,"s://u%40%41@h%3A%3a%42:42/p%2f%42?q%5B%40%5Dq#f%5B%42%5Df")
                    .set(FRAGMENT, "f%5B[%2541%41]%5Df") // change fragment => fragment will be quoted
                    .check(SCHEME, "s")
                    .check(PORT, 42)
                    .raw(USER, "u%40%41").check(USER, "u@A")
                    .raw(HOST, "h%3A%3a%42").check(HOST, "h::B")
                    .raw(PATH, "/p%2f%42").check(PATH, "/p%2FB")
                    .raw(AUTHORITY, "u%40%41@h%3A%3a%42:42")
                    .check(AUTHORITY, "u%40A@h%3A%3AB:42")
                    .raw(QUERY, "q%5B%40%5Dq").check(QUERY, "q[@]q")
                    .raw(FRAGMENT, "f%255B%5B%252541%2541%5D%255Df")
                    .check(FRAGMENT, "f%5B[%2541%41]%5Df")
                    .expect("s://u%40%41@h%3A%3a%42:42/p%2f%42?q%5B%40%5Dq#f%255B%5B%252541%2541%5D%255Df")
                    .ti("s://u%40A@h%3A%3AB:42/p%2FB?q%5B@%5Dq#f%255B%5B%252541%2541%5D%255Df"),

            // Same as case 23, but change authority
            quoting(29,"s://u%40%41@h%3A%3a%42:42/p%2f%42?q%5B%40%5Dq#f%5B%42%5Df")
                    .set(AUTHORITY, "u%40%41@h%3A%3a%42:43")  // change authority => authority will be quoted
                    .check(PORT, 43)
                    .check(SCHEME, "s")
                    .raw(USER, "u%2540%2541").check(USER, "u%40%41")
                    .raw(HOST, "h%253A%253a%2542").check(HOST, "h%3A%3a%42")
                    .raw(PATH, "/p%2f%42").check(PATH, "/p%2FB")
                    .raw(AUTHORITY, "u%2540%2541@h%253A%253a%2542:43")
                    .check(AUTHORITY, "u%2540%2541@h%253A%253a%2542:43")
                    .raw(QUERY, "q%5B%40%5Dq").check(QUERY, "q[@]q")
                    .raw(FRAGMENT, "f%5B%42%5Df").check(FRAGMENT, "f[B]f")
                    .expect("s://u%2540%2541@h%253A%253a%2542:43/p%2f%42?q%5B%40%5Dq#f%5B%42%5Df")
                    .ti("s://u%2540%2541@h%253A%253a%2542:43/p%2FB?q%5B@%5Dq#f%5BB%5Df"),

            // Same as case 23, but change scheme
            quoting(30,"s://u%40%41@h%3A%3a%42:42/p%2f%42?q%5B%40%5Dq#f%5B%42%5Df")
                    .set(SCHEME, "s2")  // change scheme
                    .check(SCHEME, "s2")
                    .check(PORT, 42)
                    .raw(USER, "u%40%41").check(USER, "u@A")
                    .raw(HOST, "h%3A%3a%42").check(HOST, "h::B")
                    .raw(PATH, "/p%2f%42").check(PATH, "/p%2FB")
                    .raw(AUTHORITY, "u%40%41@h%3A%3a%42:42")
                    .check(AUTHORITY, "u%40A@h%3A%3AB:42")
                    .raw(QUERY, "q%5B%40%5Dq").check(QUERY, "q[@]q")
                    .raw(FRAGMENT, "f%5B%42%5Df").check(FRAGMENT, "f[B]f")
                    .expect("s2://u%40%41@h%3A%3a%42:42/p%2f%42?q%5B%40%5Dq#f%5B%42%5Df")
                    .ti("s2://u%40A@h%3A%3AB:42/p%2FB?q%5B@%5Dq#f%5BB%5Df"),

            // Same as case 23, but change port
            quoting(31,"s://u%40%41@h%3A%3a%42:42/p%2f%42?q%5B%40%5Dq#f%5B%42%5Df")
                    .set(PORT, 43) // change port
                    .check(SCHEME, "s")
                    .check(PORT, 43)
                    .raw(USER, "u%40%41").check(USER, "u@A")
                    .raw(HOST, "h%3A%3a%42").check(HOST, "h::B")
                    .raw(PATH, "/p%2f%42").check(PATH, "/p%2FB")
                    .raw(AUTHORITY, "u%40%41@h%3A%3a%42:43")
                    .check(AUTHORITY, "u%40A@h%3A%3AB:43")
                    .raw(QUERY, "q%5B%40%5Dq").check(QUERY, "q[@]q")
                    .raw(FRAGMENT, "f%5B%42%5Df").check(FRAGMENT, "f[B]f")
                    .expect("s://u%40%41@h%3A%3a%42:43/p%2f%42?q%5B%40%5Dq#f%5B%42%5Df")
                    .ti("s://u%40A@h%3A%3AB:43/p%2FB?q%5B@%5Dq#f%5BB%5Df"),
    };


    static final Test[] negative = {
            test(1).set(SCHEME, "a%b")
                    .expectFailure(BuilderTest::uriSyntaxException),
            test(2).set(SCHEME, "a%b")
                    .expectUncheckedFailure(BuilderTest::asIllegalArgumentException),
            test(3).set(OPAQUE, "opaque")
                    .expectFailure(BuilderTest::illegalArgumentException),
            test(4).set(OPAQUE, "opaque")
                    .expectUncheckedFailure(BuilderTest::illegalArgumentException),
            test(5).set(SCHEME, "s").set(PATH, "p")
                    .expectFailure(BuilderTest::illegalArgumentException),
            test(6).set(SCHEME, "s").set(PATH, "p")
                    .expectUncheckedFailure(BuilderTest::illegalArgumentException),
            quoting(7).set(SCHEME, "a%b")
                    .expectFailure(BuilderTest::uriSyntaxException),
            quoting(8).set(SCHEME, "a%b")
                    .expectUncheckedFailure(BuilderTest::asIllegalArgumentException),
            quoting(9).set(OPAQUE, "opaque")
                    .expectFailure(BuilderTest::illegalArgumentException),
            quoting(10).set(OPAQUE, "opaque")
                    .expectUncheckedFailure(BuilderTest::illegalArgumentException),
            quoting(11).set(SCHEME, "s").set(PATH, "p")
                    .expectFailure(BuilderTest::illegalArgumentException),
            quoting(12).set(SCHEME, "s").set(PATH, "p")
                    .expectUncheckedFailure(BuilderTest::illegalArgumentException),

            // IAE in createHierarchical
            test(13).set(USER, "u")
                    .expectFailure(BuilderTest::illegalArgumentException),
            test(14).set(PORT, 0)
                    .expectFailure(BuilderTest::illegalArgumentException),
            test(15).set(SCHEME, "s").set(USER, "u")
                    .expectFailure(BuilderTest::illegalArgumentException),
            test(16).set(SCHEME, "s").set(PORT, 0)
                    .expectFailure(BuilderTest::illegalArgumentException),
            test(17).set(PATH, "//p")
                    .expectFailure(BuilderTest::illegalArgumentException),
            test(18).set(SCHEME, "s").set(PATH, "//p")
                    .expectFailure(BuilderTest::illegalArgumentException),
            quoting(19).set(USER, "u")
                    .expectFailure(BuilderTest::illegalArgumentException),
            quoting(20).set(PORT, 0)
                    .expectFailure(BuilderTest::illegalArgumentException),
            quoting(21).set(SCHEME, "s").set(USER, "u")
                    .expectFailure(BuilderTest::illegalArgumentException),
            quoting(22).set(SCHEME, "s").set(PORT, 0)
                    .expectFailure(BuilderTest::illegalArgumentException),
            quoting(23).set(PATH, "//p")
                    .expectFailure(BuilderTest::illegalArgumentException),
            quoting(24).set(SCHEME, "s").set(PATH, "//p")
                    .expectFailure(BuilderTest::illegalArgumentException),
            test(25).set(USER, "u")
                    .expectUncheckedFailure(BuilderTest::illegalArgumentException),
            test(26).set(PORT, 0)
                    .expectUncheckedFailure(BuilderTest::illegalArgumentException),
            test(27).set(SCHEME, "s").set(USER, "u")
                    .expectUncheckedFailure(BuilderTest::illegalArgumentException),
            test(28).set(SCHEME, "s").set(PORT, 0)
                    .expectUncheckedFailure(BuilderTest::illegalArgumentException),
            test(29).set(PATH, "//p")
                    .expectUncheckedFailure(BuilderTest::illegalArgumentException),
            test(30).set(SCHEME, "s").set(PATH, "//p")
                    .expectUncheckedFailure(BuilderTest::illegalArgumentException),
            quoting(31).set(USER, "u")
                    .expectUncheckedFailure(BuilderTest::illegalArgumentException),
            quoting(32).set(PORT, 0)
                    .expectUncheckedFailure(BuilderTest::illegalArgumentException),
            quoting(33).set(SCHEME, "s").set(USER, "u")
                    .expectUncheckedFailure(BuilderTest::illegalArgumentException),
            quoting(34).set(SCHEME, "s").set(PORT, 0)
                    .expectUncheckedFailure(BuilderTest::illegalArgumentException),
            quoting(35).set(PATH, "//p")
                    .expectUncheckedFailure(BuilderTest::illegalArgumentException),
            quoting(36).set(SCHEME, "s").set(PATH, "//p")
                    .expectUncheckedFailure(BuilderTest::illegalArgumentException),

            // IAE trying to stuff scheme in path
            test(37).set(PATH, "s://a/b/c/d")
                    .expectFailure(BuilderTest::illegalArgumentException),
            test(38).set(PATH, "s://a/b/c/d")
                    .expectUncheckedFailure(BuilderTest::illegalArgumentException),
            quoting(39).set(PATH, "s://a/b/c/d")
                    .expectFailure(BuilderTest::illegalArgumentException),
            quoting(40).set(PATH, "s://a/b/c/d")
                    .expectUncheckedFailure(BuilderTest::illegalArgumentException),

            // u@h:80/p
            test(41).set(PATH, "u@h:80/p").set(HOST, "")
                    .expectFailure(BuilderTest::illegalArgumentException),
            test(42).set(PATH, "u@h:80/p").set(HOST, "")
                    .expectUncheckedFailure(BuilderTest::illegalArgumentException),
            quoting(43).set(PATH, "u@h:80/p").set(HOST, "")
                    .expectFailure(BuilderTest::illegalArgumentException),
            quoting(44).set(PATH, "u@h:80/p").set(HOST, "")
                    .expectUncheckedFailure(BuilderTest::illegalArgumentException),
            test(45).set(PATH, "u@h:80/p").set(AUTHORITY, "")
                    .expectFailure(BuilderTest::illegalArgumentException),
            test(46).set(PATH, "u@h:80/p").set(AUTHORITY, "")
                    .expectUncheckedFailure(BuilderTest::illegalArgumentException),
            quoting(47).set(PATH, "u@h:80/p").set(AUTHORITY, "")
                    .expectFailure(BuilderTest::illegalArgumentException),
            quoting(48).set(PATH, "u@h:80/p").set(AUTHORITY, "")
                    .expectUncheckedFailure(BuilderTest::illegalArgumentException),
            test(49).set(PATH, "u@h:80/p").set(HOST, "").set(SCHEME, "s")
                    .expectFailure(BuilderTest::illegalArgumentException),
            test(50).set(PATH, "u@h:80/p").set(HOST, "").set(SCHEME, "s")
                    .expectUncheckedFailure(BuilderTest::illegalArgumentException),
            quoting(51).set(PATH, "u@h:80/p").set(HOST, "").set(SCHEME, "s")
                    .expectFailure(BuilderTest::illegalArgumentException),
            quoting(52).set(PATH, "u@h:80/p").set(HOST, "").set(SCHEME, "s")
                    .expectUncheckedFailure(BuilderTest::illegalArgumentException),
            test(53).set(PATH, "u@h:80/p").set(AUTHORITY, "").set(SCHEME, "s")
                    .expectFailure(BuilderTest::illegalArgumentException),
            test(54).set(PATH, "u@h:80/p").set(AUTHORITY, "").set(SCHEME, "s")
                    .expectUncheckedFailure(BuilderTest::illegalArgumentException),
            quoting(55).set(PATH, "u@h:80/p").set(AUTHORITY, "").set(SCHEME, "s")
                    .expectFailure(BuilderTest::illegalArgumentException),
            quoting(56).set(PATH, "u@h:80/p").set(AUTHORITY, "").set(SCHEME, "s")
                    .expectUncheckedFailure(BuilderTest::illegalArgumentException),

    };


    public static void positive() throws URISyntaxException {
        for (Test test : positive) {
            System.out.println("positive["+ test.id +"]: " + test.inputValues);
            test.build().verify().store()
                    .buildUnchecked().verify().same();
            IRI iri = test.iri();
            test.expect(iri.toIRIString()).verify();
        }
    }

    public static void negative() throws URISyntaxException {
        for (Test test : negative) {
            System.out.println("negative["+ test.id +"]: " + test.inputValues);
            test.fail();
        }
    }

    public static void main(String args[]) throws Exception {
        identities();
        positive();
        negative();
    }

    public static void identities() throws Exception {

        URL classUrl = new URL("jrt:/java.base/java/lang/Object.class");

        String[] uris = {
                        "mailto:xyz@abc.de",
                        "file:xyz#ab",
                        "http:abc/xyz/pqr",
                        "http:abc/xyz/pqr?id=x%0a&ca=true",
                        "file:/C:/v700/dev/unitTesting/tests/apiUtil/uri",
                        "http:///p",
                        "file:/C:/v700/dev/unitTesting/tests/apiUtil/uri",
                        "file:/C:/v700/dev%20src/unitTesting/tests/apiUtil/uri",
                        "file:/C:/v700/dev%20src/./unitTesting/./tests/apiUtil/uri",
                        "http://localhost:80/abc/./xyz/../pqr?id=x%0a&ca=true",
                        "file:./test/./x",
                        "file:./././%20#i=3",
                        "file:?hmm",
                        "file:.#hmm",
                        "foo",
                        "foo/bar",
                        "./foo/bar#there",
                        "http://foo.com/with%25%34%31%C0%AF%25%34%310xC0AF%EC%82%AF\uc0af",
                        classUrl.toExternalForm(),

                        };
        for (String s : uris) {
            System.out.println("identities: " + s);
            IRI i1 = IRI.parseIRI(s);
            IRI i2 = i1.with(0).build();
            IRI i3 = i1.with(0).buildUnchecked();
            if (!i1.equals(i2)) {
                String msg = String.format("Identity failed for: %s\n\t built: %s", i1, i2);
                System.out.println(msg);
                throw new RuntimeException(msg);
            }
            if (!i1.equals(i3)) {
                String msg = String.format("Identity failed for: %s\n\t built: %s", i1, i3);
                System.out.println(msg);
                throw new RuntimeException(msg);
            }
            if (!i1.toIRIString().equals(i2.toIRIString())) {
                String msg = String.format("Identity failed for: %s\n\t built: %s",
                        i1.toIRIString(), i2.toIRIString());
                System.out.println(msg);
                throw new RuntimeException(msg);
            }
            if (!i1.toIRIString().equals(i3.toIRIString())) {
                String msg = String.format("Identity failed for: %s\n\t built: %s",
                        i1.toIRIString(), i3.toIRIString());
                System.out.println(msg);
                throw new RuntimeException(msg);
            }
        }

    }

    // ================================================================================ //

    static RuntimeException uriSyntaxException(Throwable t) {
        if (t instanceof URISyntaxException) return null;
        return new RuntimeException("Expected URISyntaxException, got: " + t);
    }

    static RuntimeException illegalArgumentException(Throwable t) {
        if (t instanceof IllegalArgumentException) return null;
        return new RuntimeException("Expected IllegalArgumentException, got: " + t);
    }

    static RuntimeException asIllegalArgumentException(Throwable t) {
        RuntimeException failed = illegalArgumentException(t);
        if (failed != null) return failed;
        return uriSyntaxException(t.getCause());
    }

    static final class Test {
        final long id;
        Throwable failed;
        Supplier<IRI.Builder> builderFactory;
        IRI.Builder builder;
        IRI iri;
        IRI expected;
        String expectedIRIString;
        final List<Test> stores = new ArrayList<>();

        // for negative tests
        BuildMethod build;
        String buildName;
        Function<? super Throwable, RuntimeException> checker;

        // The list of values to set on the builder. Order is important.
        List<InputValue<?>> inputValues = new ArrayList<>();
        // The set of values to check on IRI.
        Map<Component<?>, Value<?>> valueMap = new HashMap<>();
        // The set of raw values to check on IRI. If no raw value is
        // given, the raw value is assumed to be the same as the decoded
        // value.
        Map<Component<?>, RawValue<?>> rawValueMap = new HashMap<>();

        interface BuildMethod {
            IRI build(IRI.Builder builder) throws URISyntaxException;
        }

        Test() {
            this(-1);
        }

        Test(long id) {
            this(id, IRI::newBuilder);
        }

        Test(long id, Supplier<IRI.Builder> builders) {
            this.id = id;
            builderFactory = builders;
        }

        Test store() {
            Test stored = new Test(id, builderFactory);
            stored.inputValues = new ArrayList<>(inputValues);
            stored.valueMap = new HashMap<>(valueMap);
            stored.rawValueMap = new HashMap<>(rawValueMap);
            stored.builder = builder;
            stored.iri = iri;
            stored.failed = failed;
            stores.add(stored);
            return this;
        }

        <V> Test set(Component<V> component, V value) {
            inputValues.add(component.input(value));
            valueMap.put(component, component.value(value));
            component.erasing().forEach(c -> valueMap.put(c, c.none()));
            Component alias = component.alias();
            if (alias != null && !Objects.equals(value, component.none().get())) {
                valueMap.put(alias, alias.value(value));
            }
            return this;
        }

        <V> Test check(Component<V> component, V value) {
            valueMap.put(component, component.value(value));
            Component alias = component.alias();
            if (alias != null) valueMap.put(alias, alias.value(value));
            return this;
        }

        <V> Test raw(Component<V> component, V value) {
            rawValueMap.put(component, component.raw(value));
            Component alias = component.alias();
            if (alias != null) rawValueMap.put(alias, alias.raw(value));
            return this;
        }

        Test builder(Supplier<IRI.Builder> builder) {
            this.builderFactory = builder;
            return this;
        }

        Test expectFailure(Function<? super Throwable, RuntimeException> checker) {
            return expectFailure("", IRI.Builder::build, checker);
        }

        Test expectUncheckedFailure(Function<? super Throwable, RuntimeException> checker) {
            return expectFailure("", IRI.Builder::buildUnchecked, checker);
        }

        Test expectFailure(String buildName, BuildMethod build,
                           Function<? super Throwable, RuntimeException> checker) {
            this.buildName = buildName;
            this.build = build;
            this.checker = checker;
            return this;
        }


        Test fail() {
            System.out.print("Building" + buildName + ": " + inputValues);
            Throwable failed = null;
            try {
                this.builder = builderFactory.get();
                inputValues.stream().forEach(v -> v.set(builder));
                this.iri = build.build(builder);
            } catch (Throwable t) {
                failed = t;
            }
            try {
                if (failed == null) {
                    System.out.print(" -> Should have failed: " + String.valueOf(iri));
                    throw new RuntimeException("Test " + id + " " + inputValues + " should have failed!");
                }
                RuntimeException checkFailed = checker.apply(failed);
                if (checkFailed == null) {
                    System.out.print(" -> Got expected exception\n\t" + failed);
                } else {
                    System.out.print(" -> Got unexpected exception\n\t" + failed);
                    throw checkFailed;
                }
            } finally {
                System.out.println();
            }

            return this;
        }

        Test build() throws URISyntaxException {
            System.out.print("Building: " + inputValues);
            try {
                this.builder = builderFactory.get();
                inputValues.stream().forEach(v -> v.set(builder));
                this.iri = builder.build();
            } finally {
                System.out.println(" -> " + (iri == null ? "Failed" : iri.toString()));
            }
            return this;
        }

        Test buildUnchecked() {
            System.out.print("Building (unchecked): " + inputValues);
            try {
                this.builder = builderFactory.get();
                inputValues.stream().forEach(v -> v.set(builder));
                this.iri = builder.buildUnchecked();
            } finally {
                System.out.println(" -> " + (iri == null ? "Failed" : iri.toString()));
            }
            return this;
        }

        Test verify() {
            for (Component<?> c : Component.COMPONENTS.values()) {
                valueMap.putIfAbsent(c, c.none());
                rawValueMap.putIfAbsent(c, valueMap.get(c).raw());
                valueMap.get(c).check(iri);
                rawValueMap.get(c).check(iri);
            }
            if (expected != null) {
                if (!Objects.equals(iri(), expected)) {
                    String msg = String.format(
                            "IRIs differ from expected %s:\n\t expected %s\n\t actual   %s",
                            inputValues, expected, iri());
                    System.out.println(msg);
                    throw new RuntimeException(msg);
                }
            }
            if (expectedIRIString != null) {
                if (!Objects.equals(iri().toIRIString(), expectedIRIString)) {
                    String msg = String.format(
                            "IRI.toIRIString() differ from expected:\n\t expected %s\n\t actual   %s",
                            expectedIRIString, iri().toIRIString());
                    System.out.println(msg);
                    throw new RuntimeException(msg);
                }
            }
            String toIRI;
            if (!iri().toIRIString().equals(toIRI = IRI.of(iri().toIRIString()).toIRIString())) {
                String msg = String.format(
                        "IRI.create(iri.toIRIString()).toIRIString() differ from expected:\n\t expected %s\n\t actual   %s",
                        iri().toIRIString(), toIRI);
                System.out.println(msg);
                throw new RuntimeException(msg);
            }
            return this;
        }

        Test same() {
            stores.forEach(t -> {
                if (!Objects.equals(iri, t.iri())) {
                    String msg = String.format("IRIs differ %s:\n\t %s\n\t %s",
                            inputValues, iri, t.iri());
                    System.out.println(msg);
                    throw new RuntimeException(msg);
                }
            });
            return this;
        }

        Test expect(String iri) {
            return expect(IRI.of(iri));
        }

        Test expect(IRI iri) {
            expected = iri;
            return this;
        }

        IRI iri() {
            return iri;
        }

        Test ti(String iriString) {
            expectedIRIString = iriString;
            return this;
        }
    }

    // A component value to be set on the Builder.
    static final class InputValue<T> {
        final T value;
        final String name;
        final BiFunction<IRI.Builder, T, IRI.Builder> set;

        private InputValue(T value, ValueAccessor<T> invoker) {
            this.value = value;
            this.name = invoker.name();
            this.set = invoker::set;
        }

        private InputValue(ValueAccessor<T> invoker) {
            this.value = null;
            this.name = invoker.name();
            this.set = null;
        }

        public T get() {
            return value;
        }

        public boolean isEmpty() {
            return set == null;
        }

        public IRI.Builder set(IRI.Builder builder) {
            if (set == null) return builder;
            return set.apply(builder, value);
        }

        @Override
        public String toString() {
            return String.format("%s: %s", name, value);
        }

        public static <T> InputValue<T> of(T value, ValueAccessor<T> invoker) {
            return new InputValue<T>(value, invoker);
        }

        public static <T> InputValue<T> empty(ValueAccessor<T> invoker) {
            return new InputValue<>(invoker);
        }

    }

    // A component value to be checked against the built
    // IRI. May be a decoded value (Value<T>) or a raw
    // value (RawValue<T>).
    abstract static class CheckValue<T> {
        final T value;
        final Function<IRI, T> get;
        final ValueAccessor<T> invoker;

        CheckValue(ValueAccessor<T> invoker, T value, Function<IRI, T> get) {
            this.invoker = Objects.requireNonNull(invoker);
            this.value = value;
            this.get = Objects.requireNonNull(get);
        }

        public final T get() {
            return value;
        }

        public final T get(IRI iri) {
            return get.apply(iri);
        }

        T check(String kind, IRI iri) {
            T v = get(iri);
            String n = invoker.name();
            if (!Objects.equals(v, value)) {
                String msg = String.format("check failed for %s %s: expected %s, got %s",
                        kind, n, value, v);
                System.out.println(msg);
                throw new RuntimeException(msg);
            } else {
                if (DEBUG) {
                    String msg = String.format("IRI has expected %s %s: %s", kind, n, v);
                    System.out.println(msg);
                }
            }
            return v;
        }

        @Override
        public String toString() {
            return String.format("%s: %s", invoker.name(), value);
        }

        RawValue<T> raw() {
            return new RawValue<>(value, invoker);
        }

        Value<T> value() {
            return new Value<>(value, invoker);
        }
    }

    // A component raw value to be checked against the built
    // IRI.
    static final class RawValue<T> extends CheckValue<T> {

        private RawValue(T value, ValueAccessor<T> invoker) {
            super(invoker, value, invoker::getRaw);
        }

        public T check(IRI iri) {
            return super.check("raw", iri);
        }

        @Override
        public String toString() {
            return String.format("raw %s: %s", invoker.name(), value);
        }

        public static <T> RawValue<T> of(T value, ValueAccessor<T> invoker) {
            return new RawValue<T>(value, invoker);
        }
    }

    // A decoded component value to be checked against the built
    // IRI.
    static final class Value<T> extends CheckValue<T> {
        private Value(T value, ValueAccessor<T> invoker) {
            super(invoker, value, invoker::get);
        }

        public T check(IRI iri) {
            return super.check("decoded", iri);
        }

        public static <T> Value<T> of(T value, ValueAccessor<T> invoker) {
            return new Value<T>(value, invoker);
        }
    }

    // An object that provides methods to set a component
    // value on a builder, and get it from an IRI.
    interface ValueAccessor<V> {
        IRI.Builder set(IRI.Builder b, V v);
        V get(IRI iri);
        V getRaw(IRI iri);
        String name();
    }

    // Models an IRI component: scheme, authority, host, user info, port,
    // path, opaque path, query, fragment.
    private static final class Component<V> implements ValueAccessor<V> {
        static final Map<String, Component<?>> COMPONENTS
                = new LinkedHashMap<>();
        final String name;
        final Component<V> alias; // only used for OPAQUE
        final Function<IRI, V> getRaw;
        final Function<IRI, V> get;
        final BiFunction<IRI.Builder, V, IRI.Builder> set;
        final boolean isNullable;
        final List<String> erasing;
        final V none;

        private Component(String name,
                          Function<IRI, V> getRaw,
                          Function<IRI, V> get,
                          BiFunction<IRI.Builder, V, IRI.Builder> set) {
            this(name, getRaw, get, set, null);
        }

        private Component(String name,
                          Function<IRI, V> getRaw,
                          Function<IRI, V> get,
                          BiFunction<IRI.Builder, V, IRI.Builder> set,
                          V none,
                          String... erasing) {
            this(name, null, getRaw, get, set, none, erasing);
        }

        private Component(String name,
                    Component<V> alias,
                    Function<IRI, V> getRaw,
                    Function<IRI, V> get,
                    BiFunction<IRI.Builder, V, IRI.Builder> set,
                    V none,
                    String... erasing) {
            this.name = Objects.requireNonNull(name);
            this.alias = alias; // only used for OPAQUE
            this.getRaw = Objects.requireNonNull(getRaw, "getRaw");
            this.get =  Objects.requireNonNull(get, "get");;
            this.set = Objects.requireNonNull(set, "set");;
            this.isNullable = (none == null);
            this.none = none;
            if (COMPONENTS.put(name, this) != null) {
                assert false : name;
            }
            this.erasing = List.of(erasing);
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public IRI.Builder set(IRI.Builder b, V v) {
            return set.apply(b, v);
        }
        @Override
        public V get(IRI iri) {
            return get.apply(iri);
        }
        @Override
        public V getRaw(IRI iri) {
            return getRaw.apply(iri);
        }

        public InputValue<V> input(V value) {
            if (!isNullable) {
                Objects.requireNonNull(value);
            }
            return InputValue.of(value, this);
        }

        public InputValue<V> unspecified() {
            return InputValue.empty(this);
        }

        public Value<V> none() {
            return value(none);
        }

        public RawValue<V> noraw() {
            return raw(none);
        }

        public RawValue<V> raw(V value) {
            return RawValue.of(value, this);
        }

        public RawValue<V> raw(CheckValue<V> value) {
            return RawValue.of(value.get(), this);
        }

        public Value<V> value(V value) {
            return Value.of(value, this);
        }

        public Component alias() {
            return alias;
        }

        public Stream<Component<?>> erasing() {
            return erasing.stream().map(Component::find);
        }

        public static Component<?> find(String name) {
            return COMPONENTS.get(name);
        }

        // special case for opaque path
        static String opaque(IRI iri) {
            return iri.isOpaque() ? iri.getPath() : null;
        }
        static String rawOpaque(IRI iri) {
            return iri.isOpaque() ? iri.getRawPath() : null;
        }
    }

}
