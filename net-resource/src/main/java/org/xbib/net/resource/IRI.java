/*
 * Copyright (c) 2000, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package org.xbib.net.resource;

import java.io.File;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.net.IDN;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.text.Normalizer;

/**
 * Represents a Uniform Resource Identifier (URI) reference or an
 * Internationalized Resource Identifier (IRI) reference.
 *
 * <p>Every <i>Uniform Resource Identifier</i> ( a URI specified
 * by <a href="http://www.ietf.org/rfc/rfc3986.txt">RFC&nbsp;3986</a> )
 * is by definition an IRI. This class, and the operations within, can
 * therefore be used generally to represent and manipulate a <i>Uniform
 * Resource Identifier</i>, as specified by RFC 3986.
 * This class should be used in preference to {@link java.net.URI
 * java.net.URI} where possible, even if the resource identifier does not
 * contain internationalized characters.
 * <p>This class, and its operations, supersede that of {@link java.net.URI
 * java.net.URI}
 *
 * <p> Aside from some minor deviations noted below, an instance of this
 * class represents a URI reference as defined by
 * <a href="http://www.ietf.org/rfc/rfc3986.txt"><i>RFC&nbsp;3986: Uniform
 * Resource Identifiers (URI): Generic Syntax</i></a>, or an IRI reference
 * as defined by <a href="http://www.ietf.org/rfc/rfc3987.txt">
 * <i>RFC&nbsp;3987: Internationalized Resource Identifiers (IRIs)</i></a>.
 * IRIs are defined similarly to URIs, except that the permitted characters
 * have been extended by adding the characters of the
 * <i>UCS (Universal Character Set, ISO10646)</i>.
 *
 * This class provides factory methods for creating IRI instances from
 * their components or by parsing their string forms, methods for accessing the
 * various components of an instance, and methods for normalizing, resolving,
 * and relativizing IRI instances. Instances of this class are immutable.
 * For convenience, a lightweight {@linkplain Builder builder} is also provided.
 *
 * <h3> URI/IRI syntax and components </h3>
 *
 * At the highest level a URI (or IRI) reference (hereinafter simply "URI") in string
 * form has the syntax
 *
 * <blockquote>
 * [<i>scheme</i><b>{@code :}</b>][<b>{@code //}</b><i>authority</i>]<i>path</i>[<b>{@code ?}</b><i>query</i>][<b>{@code #}</b><i>fragment</i>]
 * </blockquote>
 *
 * where square brackets [...] delineate optional components and the characters
 * <b>{@code :}</b>, <b>{@code /}</b>, <b>{@code ?}</b> and <b>{@code #}</b>
 * stand for themselves.
 *
 * <p>Some examples of URIs and their component parts are:
 * <blockquote><pre>
 * http://example.com:8042/over/there?name=ferret#nose
 * \_/    \______________/\_________/ \________/  \__/
 *  |             |            |          |         |
 * scheme        authority    path       query     fragment
 *  |   _______________________|
 * / \ /             \
 * urn:isbn:0439784549
 * </pre></blockquote>
 *
 * <p>A URI is said to be <i>absolute</i> if it specifies a scheme.
 * Otherwise it is a <i>relative</i> URI reference.
 *
 * <p> The authority component of a URI parses according to the following syntax
 *
 * <blockquote>
 * [<i>user-info</i><b>{@code @}</b>]<i>host</i>[<b>{@code :}</b><i>port</i>]
 * </blockquote>
 *
 * where the characters <b>{@code @}</b> and <b>{@code :}</b> stand for
 * themselves. The host component can be an IP-literal (a bracket enclosed
 * IPv6address or IPvFuture), an IPv4address, or just a name.
 *
 * <p> The path component of a URI can be either <i>hierarchical</i>
 * or <i>opaque to hierarchy</i>. An absolute URI that doesn't specify
 * any authority component may have a path opaque to hierarchy.
 *
 * <p> A hierarchical path component is itself said to be <i>absolute</i>
 * if it begins with a slash character ({@code '/'}); otherwise it is
 * <i>relative</i>.
 * The path of a URI that specifies an authority component, if non empty,
 * is always absolute.
 *
 * <p> A URI has a path that is <i>opaque to hierarchy</i> if the URI
 * has a scheme but no authority component, and its path isn't empty
 * and doesn't start with {@code '/'}.
 *
 * <p> For simplification this
 * document designates URIs with a hierarchical path as
 * <i>hierarchical URIs</i> and URIs with a path opaque to hierarchy as
 * <i>opaque URIs</i>.
 *
 * <p>Some examples of opaque URIs are:
 *
 * <blockquote><ul style="list-style-type:none">
 * <li>{@code mailto:java-net@www.example.com}</li>
 * <li>{@code news:comp.lang.java}</li>
 * <li>{@code urn:isbn:096139210x}</li>
 * </ul></blockquote>
 *
 * <p> As stated above, a URI has a path that is <i>hierarchical</i>
 * if it has an authority component, or it has no scheme, or its
 * path is empty or starts with a {'/'}.
 * Some examples of hierarchical URIs are:
 *
 * <blockquote><ul style="list-style-type:none">
 * <li>{@code http://example.com/languages/java/}</li>
 * <li>{@code sample/a/index.html#28}</li>
 * <li>{@code ../../demo/b/index.html}</li>
 * <li>{@code file:///~/calendar}</li>
 * <li>{@code file:/usr/var/tmp/}</li>
 * </ul></blockquote>
 *
 * (Note that a URI of the form {@code "file:foo"} is considered opaque,
 *  since it has a scheme and its path isn't empty and does not start
 *  with {@code '/'}).
 *
 * <p> All told, then, an IRI instance has the following eight components:
 *
 * <table class="striped" style="margin-left:2em">
 * <caption style="display:none">Describes the components of a URI:scheme,authority,user-info,host,port,path,query,fragment</caption>
 * <thead>
 * <tr><th scope="col">Component</th><th scope="col">Type</th></tr>
 * </thead>
 * <tbody style="text-align:left">
 * <tr><th scope="row">scheme</th><td>{@code String}</td></tr>
 * <tr><th scope="row">authority</th><td>{@code String}</td></tr>
 * <tr><th scope="row">user-info</th><td>{@code String}</td></tr>
 * <tr><th scope="row">host</th><td>{@code String}</td></tr>
 * <tr><th scope="row">port</th><td>{@code int}</td></tr>
 * <tr><th scope="row">path</th><td>{@code String}</td></tr>
 * <tr><th scope="row">query</th><td>{@code String}</td></tr>
 * <tr><th scope="row">fragment</th><td>{@code String}</td></tr>
 * </tbody>
 * </table>
 *
 * In a given instance any particular component is either <i>undefined</i> or
 * <i>defined</i> with a distinct value.  Undefined string components are
 * represented by {@code null}, while undefined integer components are
 * represented by {@code -1}.  A string component may be defined to have the
 * empty string as its value; this is not equivalent to that component being
 * undefined. For instance, an authority component can either be undefined,
 * as in {@code "file:/path"} or defined and empty, as in {@code "file:///path"},
 * or defined and non-empty, as in {@code "http://localhost:8080/path"}. Note
 * that if an authority component is defined, then the host component will be
 * defined as well (though it may be empty), and conversely. This is in
 * difference with {@link java.net.URI java.net.URI} where an empty authority
 * component was considered undefined. An other difference with
 * {@code java.net.URI} is that the path component as returned by
 * {@link #getPath()} or {@link #getRawPath()} is never {@code null}.
 *
 *
 * <h4> Operations on IRI instances </h4>
 *
 * The key operations supported by this class are those of
 * <i>normalization</i>, <i>resolution</i>, and <i>relativization</i>.
 * These operations are implemented in a manner which is consistent with
 * RFC&nbsp;3986, and therefore their behavior may be different than
 * what was implemented in {@link java.net.URI java.net.URI}.
 *
 * <p> <i>Normalization</i> is the process of removing unnecessary {@code "."}
 * and {@code ".."} segments from the path component of a hierarchical URI.
 * Each {@code "."} segment is simply removed.  A {@code ".."} segment is
 * removed only if it is preceded by a non-{@code ".."} segment, unless the
 * path is absolute and the preceding segment is the root of the path
 * ({@code "/"}).
 * If the path is absolute, it will contain no {@code ".."} segment after
 * normalization. If the path is relative, it may contain leading
 * {@code ".."} segments.<br>
 * Normalization has no effect upon opaque URIs.
 *
 * <p> <i>Resolution</i> is the process of resolving one URI against another
 * <i>base</i> URI.  The resulting URI is constructed from components of both
 * URIs in the manner specified by RFC&nbsp;3986, taking components from the
 * base URI for those not specified in the original.  For hierarchical URIs,
 * the path of the original is resolved against the path of the base and then
 * normalized.  The result, for example, of resolving
 *
 * <blockquote>
 * {@code sample/a/index.html#28}
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
 * &nbsp;&nbsp;&nbsp;&nbsp;(1)
 * </blockquote>
 *
 * against the base URI {@code http://example.com/languages/java/} is the result
 * URI
 *
 * <blockquote>
 * {@code http://example.com/languages/java/sample/a/index.html#28}
 * </blockquote>
 *
 * Resolving the relative URI
 *
 * <blockquote>
 * {@code ../../demo/b/index.html}&nbsp;&nbsp;&nbsp;&nbsp;(2)
 * </blockquote>
 *
 * against this result yields, in turn,
 *
 * <blockquote>
 * {@code http://example.com/languages/java/demo/b/index.html}
 * </blockquote>
 *
 * Resolution of both absolute and relative URIs, and of both absolute and
 * relative paths in the case of hierarchical URIs, is supported.  Resolving
 * the URI {@code file:///~calendar} against any other URI simply yields the
 * original URI, since it is absolute.  Resolving the relative URI (2) above
 * against the relative base URI (1) yields the normalized, but still relative,
 * URI:
 *
 * <blockquote>
 * {@code demo/b/index.html}
 * </blockquote>
 *
 * <p> <i>Relativization</i>, finally, is the inverse of resolution: For any
 * two normalized hierarchical URIs <i>u</i> and&nbsp;<i>v</i>,
 *
 * <blockquote>
 *   <i>u</i>{@code .relativize(}<i>u</i>{@code .resolve(}<i>v</i>{@code )).equals(}<i>v</i>{@code )}&nbsp;&nbsp;and<br>
 *   <i>u</i>{@code .resolve(}<i>u</i>{@code .relativize(}<i>v</i>{@code )).equals(}<i>v</i>{@code )}&nbsp;&nbsp;.<br>
 * </blockquote>
 *
 * Note that the assertions above are only guaranteed to hold
 * if <i>v</i> is absolute, or the normalised path of <i>v</i> doesn't
 * start with {@code ../}, isn't {@code ..}, and isn't empty or absolute.
 * The general intent, corner cases excluded, is that the first assertion
 * will hold if <i>v</i> is a well formed relative URI with a non empty path
 * referencing some sub-path of <i>u</i>, and the second will hold if
 * if <i>v</i> is some absolute URI whose scheme and authority match
 * those of <i>u</i> and whose path is referencing some sub-path of
 * <i>u</i>.
 *
 * <p> This operation is often useful when constructing a document containing URIs
 * that must be made relative to the base URI of the document wherever
 * possible.  For example, relativizing the URI
 *
 * <blockquote>
 * {@code http://example.com/languages/java/sample/a/index.html#28}
 * </blockquote>
 *
 * against the base URI
 *
 * <blockquote>
 * {@code http://example.com/languages/java/}
 * </blockquote>
 *
 * yields the relative URI {@code sample/a/index.html#28}.
 *
 *
 * <h4> Character categories </h4>
 *
 * RFC&nbsp;3986, combined with RFC&nbsp;3987, specifies precisely which
 * characters are permitted in the various components of a URI reference.
 * The following categories, most of which are taken from that specification,
 * are used below to describe these constraints:
 *
 * <table class="striped" style="margin-left:2em">
 * <caption style="display:none">Describes categories alpha, digit, alphanum, unreserved,
 *          sub-delims, gen-delims, reserved, percent-encoded, private and other</caption>
 *   <thead>
 *   <tr><th scope="col">Category</th><th scope="col">Description</th></tr>
 *   </thead>
 *   <tbody style="text-align:left">
 *   <tr><th scope="row" style="vertical-align:top">alpha</th>
 *       <td>The US-ASCII alphabetic characters,
 *        {@code 'A'}&nbsp;through&nbsp;{@code 'Z'}
 *        and {@code 'a'}&nbsp;through&nbsp;{@code 'z'}</td></tr>
 *   <tr><th scope="row" style="vertical-align:top">digit</th>
 *       <td>The US-ASCII decimal digit characters,
 *       {@code '0'}&nbsp;through&nbsp;{@code '9'}</td></tr>
 *   <tr><th scope="row" style="vertical-align:top">alphanum</th>
 *       <td>All <i>alpha</i> and <i>digit</i> characters</td></tr>
 *   <tr><th scope="row" style="vertical-align:top">unreserved</th>
 *       <td>All <i>alphanum</i> characters together with those in the string
 *        {@code "_-.~"}</td></tr>
 *   <tr><th scope="row" style="vertical-align:top">sub-delims</th>
 *       <td>The characters in the string {@code "!$&'()*+,;="}</td></tr>
 *   <tr><th scope="row" style="vertical-align:top">gen-delims</th>
 *       <td>The characters in the string {@code ":/?#[]@"}</td></tr>
 *   <tr><th scope="row" style="vertical-align:top">reserved</th>
 *       <td>All <i>sub-delims</i> characters and all
 *           <i>gen-delims</i> characters</td></tr>
 *   <tr><th scope="row" style="vertical-align:top;white-space:nowrap">percent-encoded</th>
 *       <td>Percent-encoded octets, that is, triplets consisting of the percent
 *           character ({@code '%'}) followed by two hexadecimal digits
 *           ({@code '0'}-{@code '9'}, {@code 'A'}-{@code 'F'}, and
 *           {@code 'a'}-{@code 'f'})</td></tr>
 *   <tr><th scope="row" style="vertical-align:top">private</th>
 *       <td>Unicode characters in the range
 *       {@code U+E000-U+F8FF / U+F0000-U+FFFFD / U+100000-U+10FFFD}.
 *       These characters are defined in the <i>iprivate</i>
 *       rule of RFC 3987, and correspond to
 *       <a href="http://www.unicode.org/glossary/#private_use_code_point">
 *           Private Use Code Points</a> in the Unicode Character Set.</td></tr>
 *   <tr><th scope="row" style="vertical-align:top">other</th>
 *       <td>The Unicode characters that are not in the US-ASCII character set,
 *           are not control characters (according to the {@link
 *           Character#isISOControl(char) Character.isISOControl}
 *           method), are not space characters (according to the {@link
 *           Character#isSpaceChar(char) Character.isSpaceChar} method), are
 *           not special characters in the range {@code U+FFF0}-{@code U+FFFD},
 *           are not <a href="http://www.unicode.org/glossary/#noncharacter">
 *           non-characters</a>, and are not
 *           <a href="http://www.unicode.org/glossary/#private_use_code_point">
 *               private use characters</a>.
 *           </td></tr>
 * </tbody>
 * </table>
 *
 * <p><a id="legal-chars"></a> The set of all legal URI characters consists of
 * the <i>unreserved</i>, <i>reserved</i>, <i>percent-encoded</i>, <i>private</i>
 * and <i>other</i> characters.
 *
 * <p><i>Deviation from RFC 3987:</i> This implementation
 * considers non-US-ASCII space characters as defined by
 * {@link Character#isSpaceChar(char) Character.isSpaceChar} as illegal
 * and will quote them. </p>
 *
 * <h4> Percent-encoded octets, quotation, encoding, and decoding </h4>
 *
 * RFC 3986 allows percent-encoded octets to appear in the user-info, host, path,
 * query, and fragment components.  Percent-encoding serves two purposes in URIs:
 *
 * <ul>
 *
 *   <li><p> To <i>encode</i> non-US-ASCII characters when a URI is required to
 *   conform strictly to RFC&nbsp;3986 by not containing any <i>other</i>
 *   or <i>private</i> characters.  </p></li>
 *
 *   <li><p> To <i>quote</i> characters that are otherwise illegal in a
 *   component.  The user-info, host, path, query, and fragment components differ
 *   slightly in terms of which characters are considered legal and illegal.
 *   </p></li>
 *
 * </ul>
 *
 * These purposes are served in this class by three related operations:
 *
 * <ul>
 *
 *   <li><p><a id="encode"></a> A character is <i>encoded</i> by replacing it
 *   with the sequence of percent-encoded octets that represent that character
 *   in the UTF-8 character set.  The Euro currency symbol ({@code '\u005Cu20AC'}),
 *   for example, is encoded as {@code "%E2%82%AC"}.</p></li>
 *
 *   <li><p><a id="quote"></a> An illegal character is <i>quoted</i> simply by
 *   encoding it.  The space character, for example, is quoted by replacing it
 *   with {@code "%20"}.  UTF-8 contains US-ASCII, hence for US-ASCII
 *   characters this transformation has exactly the effect required by
 *   RFC&nbsp;3986. </p></li>
 *
 *   <li><p><a id="decode"></a>
 *   A sequence of percent-encoded octets is <i>decoded</i> by
 *   replacing it with the sequence of characters that it represents in the
 *   UTF-8 character set.  UTF-8 contains US-ASCII, hence decoding has the
 *   effect of de-quoting any quoted US-ASCII characters as well as that of
 *   decoding any encoded non-US-ASCII characters.  If a <a
 *   href="../nio/charset/CharsetDecoder.html#ce">decoding error</a> occurs
 *   when decoding the percent-encoded octets, e.g. the octet is not part of
 *   a strictly legal UTF-8 octet sequence, then the erroneous octets are
 *   copied to the result without decoding. If the octets represent
 *   <a href="http://www.unicode.org/glossary/#noncharacter">non-characters</a>,
 *   or are not appropriate according to RFC 3987, e.g. the octets represent
 *   bidirectional formatting characters as defined in the table below,
 *   they are also copied to the result without decoding.</p>
 *     <table class="striped" style="margin-left:2em">
 *     <caption style="display:none">Bidirectional Formatting Characters</caption>
 *     <thead>
 *     <tr><th scope="col">Bidi char</th>
 *         <th scope="col">Value&nbsp;&nbsp;&nbsp;&nbsp;</th>
 *         <th scope="col">UTF-8 sequence</th>
 *     </tr>
 *     </thead>
 *     <tbody style="text-align:left">
 *     <tr><th scope="row">LRM</th><td>{@code \u005Cu200E}</td><td>{@code %E2%80%8E}</td></tr>
 *     <tr><th scope="row">RLM</th><td>{@code \u005Cu200F}</td><td>{@code %E2%80%8F}</td></tr>
 *     <tr><th scope="row">LRE</th><td>{@code \u005Cu202A}</td><td>{@code %E2%80%AA}</td></tr>
 *     <tr><th scope="row">RLE</th><td>{@code \u005Cu202B}</td><td>{@code %E2%80%AB}</td></tr>
 *     <tr><th scope="row">PDF</th><td>{@code \u005Cu202C}</td><td>{@code %E2%80%AC}</td></tr>
 *     <tr><th scope="row">LRO</th><td>{@code \u005Cu202D}</td><td>{@code %E2%80%AD}</td></tr>
 *     <tr><th scope="row">RLO</th><td>{@code \u005Cu202E}</td><td>{@code %E2%80%AE}</td></tr>
 *     </tbody></table>
 *     <p>Eagerly decoding every percent-encoded octet could sometime lead to confusion in
 *        the interpretation of the decoded string.
 *        The {@code decode} operation may therefore leave some percent-encoded octet
 *        in percent-encoded form depending on <a href="#decodemethods">which component</a>
 *        of the URI the decode operation is applied to.</p>
 *   </li>
 *
 * </ul>
 *
 * These operations are exposed in the methods of this class as follows:
 *
 * <ul>
 *
 *   <li><p> The {@linkplain IRI#parseIRI(String) parseIRI factory} requires
 *   any illegal characters in its argument to be quoted and preserves any
 *   percent-encoded octets and <i>other</i> characters that are present.
 *   It also preserves <i>private</i> characters in the query part of
 *   the IRI.</p></li>
 *
 *   <li><p> The {@linkplain IRI#parseLenient(String) parseLenient factory}
 *   will leniently accept and {@linkplain #quoteLenient(String) quote
 *   illegal US-ASCII printable characters}, such as {@code '<'}, {@code '>'},
 *   {@code '"'}, space, {@code '{'}, {@code '}'}, {@code '|'}, {@code '\'},
 *   {@code '^'}, and {@code '`'}, but requires any other illegal characters
 *   in its argument to be quoted.
 *   It also preserves any percent-encoded octets and <i>other</i> characters
 *   that are present, as well as <i>private</i> characters, if found in the
 *   query part of the IRI.</p></li>
 *
 *   <li><p><a id="multiargs"></a> The {@linkplain #newBuilder() default
 *   lightweight builders} and {@linkplain
 *   IRI#createHierarchical(String,String,String,int,String,String,String)
 *   multi-argument factories} quote illegal characters as
 *   required by the components in which they appear.  A character
 *   triplet, consisting of the percent character ({@code '%'}) followed by
 *   two hexadecimal digits, is always considered as already percent-encoded.
 *   The percent character ({@code '%'}) in a percent-encoded triplet is not
 *   quoted any more; otherwise, if not followed by two hexadecimal digits,
 *   it is always quoted by these factories.
 *   This makes the encoding performed by these factories idempotent, as they
 *   will never double percent-encode an already percent-encoded triplet
 *   sequence. Any <i>other</i> characters are preserved. Private Use characters
 *   (<i>private</i>) are also preserved when they appear in the
 *   query string.</p></li>
 *
 *   <li><p><a id="rawgetters"></a> The {@link #getRawUserInfo() getRawUserInfo},
 *   {@link #getRawHostString() getRawHostString}, {@link #getRawPath() getRawPath},
 *   {@link #getRawQuery() getRawQuery}, {@link #getRawFragment() getRawFragment} and
 *   {@link #getRawAuthority() getRawAuthority} methods (also referred as
 *   <i>raw getters</i>) return the values of their corresponding components in
 *   raw form, without interpreting any percent-encoded octets.
 *   The strings returned by these methods may contain both percent-encoded
 *   octets and <i>other</i> characters and will not contain any illegal characters.
 *   In addition the string returned by {@code getRawQuery} may also contain
 *   <i>private</i> characters.</p></li>
 *
 *   <li><p><a id="decodemethods"></a> The {@link #getUserInfo() getUserInfo},
 *   {@link #getHostString() getHostString}, {@link #getPath() getPath},
 *   {@link #getQuery() getQuery}, {@link #getFragment() getFragment},
 *   and {@link #getAuthority() getAuthority} methods (also referred as
 *   <i>decoding getters</i>) decode any percent-encoded octets in their corresponding
 *   components, with exceptions listed below. The strings returned by these methods may
 *   contain both <i>other</i> characters, <i>private</i> characters, and illegal characters.
 *   They may also contain percent-encoded octets, but only if invalid octet sequences,
 *   bidirectional formatting characters, or a contextual subset of <i>gen-delims</i>
 *   percent-encoded octets whose decoded form would lead to confusion in
 *   the interpretation of the decoded string, are found in the raw form of the
 *   component, as detailed below:  </p>
 *   <ul>
 *       <li>All methods above will leave invalid octet
 *       sequences and bidirectional formatting characters in their percent-encoded
 *       form, if those are found in the raw form of the corresponding component.
 *       </li>
 *       <li>In addition, the {@link #getPath() getPath}
 *       method will also preserve and normalise the percent-encoded form of the forward slash
 *       character {@code %2F} (percent-encoded form of the {@code '/'} character),
 *       if it is found in percent-encoded form in the raw form of the component.
 *       For instance, a raw path of the form  {@code "/%41%2f%42/C"}
 *       as found in {@code "http://host/%41%2f%42/C"} will be decoded as {@code "/A%2FB/C"},
 *       thus preserving the structure of the original URI path.
 *       </li>
 *       <li>Similarly, the {@link #getAuthority() getAuthority} method will preserve
 *       and normalise the percent-encoded form of those characters which once decoded
 *       might change the interpretation of the authority string: {@code %40} (encoded
 *       form of the {@code '@'} character), {@code %5B} (encoded
 *       form of the {@code '['} character), {@code %5D} (encoded
 *       form of the {@code ']'} character), {@code %25} (encoded
 *       form of the {@code '%'} character), when it is followed by two hexadecimal
 *       digits, and  {@code %3A} (encoded form of the {@code ':'} character),
 *       if those are found in percent encoded form in the raw authority component.
 *       For instance, a raw authority of the form
 *       {@code "%41%2540%40%42b@foo"} as found in {@code "http://%41%2540%40%42b@foo/A/B/C"}
 *       will be decoded as {@code "A%2540%40B@foo"}, thus preserving the structure of the
 *       original authority and allowing its further decomposition in
 *       <i>userinfo</i>{@code @}<i>reg-name</i>.
 *       </li>
 *       <li> As with {@code java.net.URI}, {@link #getQuery()} decodes all valid
 *       percent-encoded triplets. Applications that use percent-encoding to
 *       build query strings in a specific query string format should therefore
 *       prefer using {@link #getRawQuery()} and split the raw
 *       query string into its specific sub-components before trying to decode
 *       any percent-encoded octets.
 *       </li>
 *   </ul>
 *   </li>
 *
 * </ul>
 *
 * <p> Note that these transformations also ensure that
 * <i>u</i>{@code .equals(IRI.parseIRI(}<i>u</i>{@code .toIRIString()))} is
 * always true. </p>
 *
 * <h4> Identities </h4>
 *
 * For any IRI <i>u</i>, it is always the case that
 *
 * <blockquote>
 * {@code IRI.parseIRI(}<i>u</i>{@code .toString()).equals(}<i>u</i>{@code )}&nbsp;.
 * </blockquote>
 * and
 * <pre>
 *     <i>u</i>.toBuilder().build().equals(<i>u</i>)</pre>
 *
 * In addition,
 * <pre>
 *     IRI.createOpaque(<i>u</i>.getScheme(),
 *             <i>u</i>.getRawPath(),
 *             <i>u</i>.getRawQuery(),
 *             <i>u</i>.getRawFragment())
 *     .equals(<i>u</i>)</pre>
 * if <i>u</i> has a scheme, no authority, and its path doesn't start
 * with {@code '/'} and is not empty (in other words, if it is
 * opaque to hierarchy),
 * <pre>
 *     IRI.createHierarchical(<i>u</i>.getScheme(),
 *             <i>u</i>.getRawAuthority(),
 *             <i>u</i>.getRawPath(),
 *             <i>u</i>.getRawQuery(),
 *             <i>u</i>.getRawFragment())
 *     .equals(<i>u</i>)</pre>
 * and
 * <pre>
 *     IRI.createHierarchical(<i>u</i>.getScheme(),
 *             <i>u</i>.getRawUserInfo(), <i>u</i>.getRawHostString(), <i>u</i>.getPort(),
 *             <i>u</i>.getRawPath(), <i>u</i>.getRawQuery(),
 *             <i>u</i>.getRawFragment())
 *     .equals(<i>u</i>)</pre>
 * if <i>u</i> is hierarchical.
 *
 * For any IRI <i>u</i> that does not contain double encoding syntax such
 * that one of the decoded components contains a percent-encoded triplet
 * sequence corresponding to a legal character for that component (such as e.g.
 * {@code "%E2%82%AC"} which corresponds to the UTF-8 encoding of the Euro
 * currency symbol {@code "\u20ac"}), the following identities also hold:
 * <pre>
 *     IRI.createOpaque(<i>u</i>.getScheme(),
 *             <i>u</i>.getPath(),
 *             <i>u</i>.getQuery(),
 *             <i>u</i>.getFragment())
 *     .equals(<i>u</i>)</pre>
 * if <i>u</i> has a scheme, no authority, and its path doesn't start
 * with {@code '/'} and is not empty (in other words, if it is
 * opaque to hierarchy),
 * <pre>
 *     IRI.createHierarchical(<i>u</i>.getScheme(),
 *             <i>u</i>.getAuthority(),
 *             <i>u</i>.getPath(),
 *             <i>u</i>.getQuery(),
 *             <i>u</i>.getFragment())
 *     .equals(<i>u</i>)</pre>
 * and
 * <pre>
 *     IRI.createHierarchical(<i>u</i>.getScheme(),
 *             <i>u</i>.getUserInfo(), <i>u</i>.getHostString(), <i>u</i>.getPort(),
 *             <i>u</i>.getPath(), <i>u</i>.getQuery(),
 *             <i>u</i>.getFragment())
 *     .equals(<i>u</i>)</pre>
 * if <i>u</i> is hierarchical.
 *
 *
 * <h4> Internationalized Resource Identifiers (IRIs) </h4>
 *
 * An instance of this class represents an IRI whenever it contains non-US-ASCII,
 * <i>other</i>, or <i>private</i> characters. The following methods perform the
 * operations and conversions as described in RFC 3987:
 *
 * <ul>
 * <li>{@link #toASCIIString()} converts an IRI to a URI literal string, i.e. any
 * <i>reserved</i>, <i>private</i>, and <i>other</i> characters are all percent-encoded
 * and the resulting (UTF-8 encoded) string can be used wherever a URI is required.</li>
 *
 * <li>{@link #toString()} outputs an IRI in its original Unicode form.</li>
 *
 * <li>{@link #toIRIString()} converts an IRI which may contain percent-encoded
 * <i>other</i> and <i>private</i> characters to an IRI string which does not contain
 * unnecessary percent-encoded characters. Note that if the IRI has a query
 * string formatted by the application in a specific query string syntax which
 * requires using percent-encoding to quote specific query string syntax delimiters,
 * this may prevent the application to parse back the query string in that syntax.</li>
 * </ul>
 *
 *
 * <h4> IRIs, URIs, URLs, and URNs </h4>
 *
 * An IRI is an <i>Internationalized Resource Identifier</i> which contains
 * characters from the <i>Universal Character Set (Unicode/ISO 10646)</i>,
 * which encompass the US-ASCII character set. Hence every URI is an IRI,
 * abstractly speaking, and every IRI can be mapped to a URI string
 * conforming to RFC&nbsp;3986. Instances of the
 * {@link java.net.URI java.net.URI} class represent a Uniform Resource
 * Identifier (URI) reference as defined by the obsolete RFC&nbsp;2396.
 * That specification has been superseded by RFCs 3986 and 3987 which
 * define URIs and IRIs, respectively. Both of these entities are now
 * represented by {@link IRI java.net.IRI}, which should be preferred
 * over {@code java.net.URI}.  Because {@link java.net.URI java.net.URI}
 * is not fully compliant with RFC&nbsp;3986 or RFC&nbsp;3987, but
 * is based on RFC&nbsp;2396, with deviations that brings it close to
 * RFC&nbsp;3986 and RFC&nbsp;3987, most instances of {@code java.net.IRI}
 * can be converted to instances of {@code java.net.URI}, to the exception
 * of some corner case URIs such as {@code "about:"}, {@code "http://"},
 * or {@code "//"}, that {@code java.net.URI} is unable to parse.
 *
 * <p> In addition, a URI or IRI is a uniform resource <i>identifier</i>
 * while a URL is a uniform resource <i>locator</i>.
 * Hence every URL is a URI, abstractly speaking, but
 * not every URI is a URL.  This is because there is another subcategory of
 * URIs, uniform resource <i>names</i> (URNs), which name resources but do not
 * specify how to locate them.  The {@code mailto}, {@code news}, and
 * {@code isbn} URIs shown above are examples of URNs.
 *
 * <p> The conceptual distinction between URIs and URLs is reflected in the
 * differences between this class and the {@link URL} class.
 *
 * <p> An instance of this class represents a URI reference in the syntactic
 * sense defined by RFC&nbsp;3986 and RFC&nbsp;3987.
 * A URI may be either absolute or relative.
 * A URI string is parsed according to the generic syntax without regard to the
 * scheme, if any, that it specifies.  No lookup of the host, if any, is
 * performed, and no scheme-dependent stream handler is constructed.  Equality,
 * hashing, and comparison are defined strictly in terms of the character
 * content of the instance.  In other words, a URI instance is little more than
 * a structured string that supports the syntactic, scheme-independent
 * operations of comparison, normalization, resolution, and relativization.
 *
 * <p> An instance of the {@link URL} class, by contrast, represents the
 * syntactic components of a URL together with some of the information required
 * to access the resource that it describes.  A URL must be absolute, that is,
 * it must always specify a scheme.  A URL string is parsed according to its
 * scheme.  A stream handler is always established for a URL, and in fact it is
 * impossible to create a URL instance for a scheme for which no handler is
 * available.  Equality and hashing depend upon both the scheme and the
 * Internet address of the host, if any; comparison is not defined.  In other
 * words, a URL is a structured string that supports the syntactic operation of
 * resolution as well as the network I/O operations of looking up the host and
 * opening a connection to the specified resource. Applications should
 * refrain from creating instances of {@code java.net.URL} directly, and should
 * preferably use {@link IRI#toURL() IRI.toURL()} to do so.
 *
 * <h4> RFC 3986 and RFC 2396: compatibility with java.net.URI</h4>
 *
 * <p>In <a href="http://www.ietf.org/rfc/rfc2396.txt"><i>RFC&nbsp;2396</i></a>,
 * i.e. the previous specification of URI syntax, such as supported by
 * {@link java.net.URI java.net.URI}, a URI had the syntax
 *
 * <blockquote>
 * [<i>scheme</i><b>{@code :}</b>]<i>scheme-specific-part</i>[<b>{@code #}</b><i>fragment</i>]
 * </blockquote>
 *
 * at the highest level. URIs were also classified according to whether they
 * were <i>opaque</i> or <i>hierarchical</i>.
 * An <i>opaque</i> URI was defined as an absolute URI whose scheme-specific
 * part does not begin with a slash character ({@code '/'}). Opaque URIs were
 * not subject to further parsing.
 *
 * <p>As explained earlier in this document, in
 * <a href="http://www.ietf.org/rfc/rfc3986.txt"><i>RFC&nbsp;3986</i></a> this
 * terminology has been replaced by allowing a URI to have a path component
 * which may be opaque to hierarchy.
 *
 * <p> These differences may seem innocuous but they do have an impact
 * on behavioral differences between {@link java.net.URI java.net.URI}
 * and this class. For instance, an opaque URI as parsed by {@code
 * java.net.URI} will have a {@code null} path component, whereas as parsed
 * by this class, it will simply have an opaque path component.
 * Similarly, an opaque URI as modelled with {@code java.net.URI} has
 * no query component: the query component is included in the opaque
 * <i>scheme-specific-part</i> of the URI. With {@code java.net.IRI}, an
 * opaque URI may now have a query component.
 *
 * <p> The raw <i>scheme-specific-part</i> of a {@code java.net.URI}
 * may be reconstructed by concatenating the raw authority, path, and query
 * components of a corresponding IRI instance using the following rule:
 * <i>{@code ["//" authority] path ["?" query]}</i>
 * where parts enclosed in brackets are omitted when their corresponding
 * raw component is null.
 *
 * <p> Another difference brought by
 * <a href="http://www.ietf.org/rfc/rfc3986.txt"><i>RFC&nbsp;3986</i></a>
 * is in the parsing of the authority component. RFC 2396 used to make
 * the distinction between a <i>host</i> name and <i>reg_name</i>.
 * RFC 3986 does not.
 * The consequence is that with {@code java.net.URI}, a host name that
 * did not strictly abide with the syntax for DNS names was parsed as
 * a <i>reg_name</i>, and {@link URI#getHost() java.net.URI::getHost}
 * would return {@code null}. Not so for {@code java.net.IRI} where
 * the host <b>is</b> the <i>reg-name</i>. For instance, in
 * {@code "http://x_y.z.com:80/example"}, {@code java.net.URI} would
 * report a {@code null} host, while {@code java.net.IRI} will parse
 * the host string as {@code "x_y.z.com"}.
 * As a consequence the {@linkplain #getRawHostString()
 * raw host} component, being a potential <i>reg-name</i>, is no longer
 * guaranteed to be either a
 * syntactically valid IP-literal address or to conform to the DNS syntax.
 * To prevent misinterpretation when the base {@link ResourceIdentifier}
 * class is used, this class provides a {@link #getHost()} method that
 * behaves as {@link URI#getHost()}. That is, {@link #getHost()}
 * will return null if the decoded host component does not syntactically
 * correspond to an IPv4 literal address, and IPv6 literal address, or a
 * reg-name that conforms to the DNS syntax.
 * APIs that need to deal with internationalized host names are encouraged
 * to use the uninterpreted {@linkplain #getRawHostString() raw host string}
 * or {@linkplain #getHostString() decoded host string} as they see fit, and can
 * use the {@link #getHostType(String)} method to make informed decisions
 * on how to handle that host part of the authority component.
 *
 * <p> Finally, there are also other differences in behaviors for the
 * {@code normalize} and {@code resolve} methods, as well as in
 * the corresponding {@code relativize} method.</p>
 *
 * <p> Note that above definitions, e.g. scheme-specific-part component and
 * opaque vs. hierarchical, are all subject to RFC 2396. They're deprecated
 * in RFC 3986.
 *
 * @apiNote
 *
 * Applications working with file paths and file URIs should take great
 * care to use the appropriate methods to convert between the two.
 * The {@link Path#of(URI)} factory method and the {@link File#File(URI)}
 * constructor can be used to create {@link Path} or {@link File}
 * objects from a file URI. {@link Path#toUri()} and {@link File#toURI()}
 * can be used to create a {@link URI} from a file path, which can
 * be converted to an IRI for further manipulation using {@link
 * IRI#of(ResourceIdentifier)}.
 * Applications should never try to {@linkplain
 * #createHierarchical(String, String, String, String)
 * construct}, {@linkplain #parseIRI(String) parse}, or
 * {@linkplain #resolve(String) resolve} an {@code IRI}
 * from the direct string representation of a {@code File} or {@code Path}
 * instance.
 * <p>
 * Some components of a URI or IRI, such as <i>userinfo</i>, may
 * be abused to construct misleading URLs or URIs. Applications
 * that deal with URIs or IRIs should take into account
 * the recommendations advised in <a
 * href="https://tools.ietf.org/html/rfc3986#section-7">RFC3986,
 * Section 7, Security Considerations</a>.
 *
 * @see <a href="http://ietf.org/rfc/rfc3986.txt"><i>RFC&nbsp;3986: Uniform
 * Resource Identifier (URI): Generic Syntax</i></a>, <br><a
 * href="http://ietf.org/rfc/rfc3987.txt"><i>RFC&nbsp;3987: Internationalized
 * Resource Identifiers (IRIs)</i></a>, <br><a
 * href="http://www.ietf.org/rfc/rfc3513.txt"><i>RFC&nbsp;3513: Internet
 * Protocol Version 6 (IPv6) Addressing Architecture</i></a>, <br><a
 * href="URISyntaxException.html">URISyntaxException</a>
 */

public final class IRI extends ResourceIdentifier
    implements Comparable<IRI>, Serializable
{

    // Note: Comments containing the word "ASSERT" indicate places where a
    // throw of an InternalError should be replaced by an appropriate assertion
    // statement once asserts are enabled in the build.

    static final long serialVersionUID = -6052424284110960213L;


    // -- Properties and components of this instance --

    // Components of all URIs: [<scheme>:]<scheme-specific-part>[#<fragment>]
    private final transient String scheme;            // null ==> relative URI
    private final transient String fragment;

    // Hierarchical URI components: [//<authority>]<path>[?<query>]
    private final transient String authority;         // opaque => null authority

    // Authority: [<userInfo>@][<host>][:<port>]
    private final transient String userInfo;          // null if absent
    private final transient String host;              // null if authority is null
    private final transient int port;                 // -1 ==> undefined

    // Remaining components of hierarchical URIs
    private final transient String path;
    private final transient String query;

    // The remaining fields may be computed on demand, which is safe even in
    // the face of multiple threads racing to initialize them
    private transient int hash;                       // Zero ==> undefined

    private transient String decodedUserInfo;
    private transient String decodedAuthority;
    private transient String decodedHost;
    private transient String decodedPath;
    private transient String decodedQuery;
    private transient String decodedFragment;

    /**
     * The string form of this URI.
     *
     * @serial
     */
    private volatile String string;             // The only serializable field

    // The IRI string form of this URI, i.e. there are no
    // unnecessarily percent-encoded characters in it according to RFC 3987
    private volatile transient String iriString;


    // -- Constructors and factories --

    private IRI(String input,
                String scheme,
                String authority,
                String userInfo,
                String host,
                int port,
                String path,
                String query,
                String fragment) {
        assert host != null || authority == null;
        assert path != null;
        this.string = input;
        this.scheme = scheme;
        this.authority = authority;
        this.userInfo = userInfo;
        this.host = host;
        this.port = port;
        this.path = path;
        this.query = query;
        this.fragment = fragment;
    }


    /**
     * Creates an IRI by parsing the given string.
     *
     * <p> This method parses the given string exactly as specified by the
     * grammar in <a
     * href="http://www.ietf.org/rfc/rfc3987.txt">RFC&nbsp;3987</a>,
     * Section&nbsp;2.2.&nbsp;ABNF&nbsp;for&nbsp;IRI&nbsp;References&nbsp;and&nbsp;IRIs </p>
     *
     * @param  encodedString   The string to be parsed into an IRI
     * @return the IRI
     *
     * @throws  NullPointerException
     *          If {@code str} is {@code null}
     *
     * @throws  URISyntaxException
     *          If the given string violates RFC&nbsp;3986 or RFC&nbsp;3987
     */
    public static IRI parseIRI(String encodedString)
        throws URISyntaxException
    {
        return new Parser(encodedString).parse(true);
    }

    /**
     * Creates an IRI by leniently parsing the given string.
     *
     * <p> This method implements a lenient parsing of the
     * given IRI string, which will first percent-encode the printable
     * characters in US-ASCII that are not allowed in URIs, but which
     * are sometime used in queries (such as the pipe ({@code '|'})
     * character), as specified by the {@linkplain #quoteLenient(String)}
     * method. The resulting string is then parsed as if by calling
     * {@link IRI#parseIRI(String)}.  </p>
     *
     * @implSpec Calling this method is equivalent to calling {@code
     * IRI.parseIRI(IRI.quoteLenient(iriString))}.
     *
     * @param  iriString   The string to be parsed into an IRI
     * @return the IRI
     *
     * @throws  NullPointerException
     *          If {@code iriString} is {@code null}
     *
     * @throws  URISyntaxException
     *          If the given string, after percent-encoding the
     *          printable characters in US-ASCII that are not
     *          allowed in URIs, still violates RFC&nbsp;3986 or
     *          RFC&nbsp;3987
     *
     * @see #quoteLenient(String)
     * @see #unquoteLenient(String)
     * @see #toLenientString()
     *
     */
    public static IRI parseLenient(String iriString)
            throws URISyntaxException
    {
        return new Parser(quote(iriString, L_LENIENT, H_LENIENT, NonASCII.NOQUOTES))
                .parse(true);
    }

    /**
     * Creates a hierarchical IRI from the given components.
     *
     * <p> If a scheme is given then the path, if also given, must either be
     * empty or begin with a slash character ({@code '/'}).  Otherwise a
     * component of the new URI may be left undefined by passing {@code null}
     * for the corresponding parameter or, in the case of the {@code port}
     * parameter, by passing {@code -1}.
     *
     * <p> This method first builds a URI string from the given components
     * according to the rules specified in <a
     * href="http://www.ietf.org/rfc/rfc3986.txt">RFC&nbsp;3986</a>,
     * section&nbsp;5.3: </p>
     *
     * <ol>
     *
     *   <li><p> Initially, the result string is empty. </p></li>
     *
     *   <li><p> If a scheme is given then it is appended to the result,
     *   followed by a colon character ({@code ':'}).  </p></li>
     *
     *   <li><p> If user information, a host, or a port are given then the
     *   string {@code "//"} is appended.  </p></li>
     *
     *   <li><p> If user information is given then it is appended, followed by
     *   a commercial-at character ({@code '@'}).  Any character not in the
     *   <i>unreserved</i>, <i>sub-delims</i>, <i>percent-encoded</i>,
     *   or <i>other</i> categories, and not equal to the colon character
     *   ({@code ':'}), is <a href="#quote">quoted</a>.  </p></li>
     *
     *   <li><p> If a host is given then it is appended.  If the host is a
     *   literal IPv6 address but is not enclosed in square brackets
     *   ({@code '['} and {@code ']'}) then the square brackets are added.
     *   Otherwise if the host is a reg-name, then any character not in the
     *   <i>unreserved</i>, <i>sub-delims</i> or <i>percent-encoded</i>
     *   or <i>other</i> categories is <a href="#quote">quoted</a>.
     *   </p></li>
     *
     *   <li><p> If a port number is given then a colon character
     *   ({@code ':'}) is appended, followed by the port number in decimal.
     *   </p></li>
     *
     *   <li><p> If a path is given then it is appended.  Any character not in
     *   the <i>unreserved</i>, <i>sub-delims</i>, <i>percent-encoded</i>,
     *   or <i>other</i> categories, and not equal to the slash character
     *   ({@code '/'}), the colon character ({@code ':'}), or the
     *   commercial-at character ({@code '@'}), is quoted.  </p></li>
     *
     *   <li><p> If a query is given then a question-mark character
     *   ({@code '?'}) is appended, followed by the query. Any character not in
     *   the <i>unreserved</i>, <i>sub-delims</i>, <i>percent-encoded</i>,
     *   <i>other</i> or <i>private</i> categories, and not equal to the slash
     *   character ({@code '/'}), the colon character ({@code ':'}), the
     *   commercial-at character ({@code '@'}) or the question-mark
     *   character ({@code '?')}, is quoted.
     *   </p></li>
     *
     *   <li><p> Finally, if a fragment is given then a hash character
     *   ({@code '#'}) is appended, followed by the fragment.  Any character not in
     *   the <i>unreserved</i>, <i>sub-delims</i>, <i>percent-encoded</i>,
     *   or <i>other</i> categories, and not equal to the slash
     *   character ({@code '/'}), the colon character ({@code ':'}), the
     *   commercial-at character ({@code '@'}) or the question-mark
     *   character ({@code '?')}, is quoted.  </p></li>
     *
     * </ol>
     *
     * @param   scheme    Scheme name
     * @param   userInfo  User name and authorization information
     * @param   host      Host name
     * @param   port      Port number
     * @param   path      Path
     * @param   query     Query
     * @param   fragment  Fragment
     * @return  The new IRI
     *
     * @throws URISyntaxException
     *         If the URI string constructed from the given components violates
     *         RFC&nbsp;3986 or RFC&nbsp;3987
     * @throws IllegalArgumentException If the given arguments are not
     *         consistent. For instance,
     *         if both a scheme and a path are given but the path is relative;
     *         or if {@code host} is {@code null} but {@code userInfo} is not
     *         {@code null} or port is not {@code -1}; or if {@code host} is
     *         {@code null} and the {@code path} component starts with
     *         {@code "//"} (see RFC&nbsp;3986 section 3), or contains
     *         {@code ':'} before the first {@code '/'}; or if {@code host}
     *         is not {@code null}, {@code scheme} is {@code null}, {@code path}
     *         is not empty and does not start with {@code '/'}.
     *
     */
    public static IRI createHierarchical(String scheme,
                                         String userInfo,
                                         String host,
                                         int port,
                                         String path,
                                         String query,
                                         String fragment)
        throws URISyntaxException
    {
        if (host == null && (userInfo != null || port != -1)) {
            throw new IllegalArgumentException(
                    "host can not be null when authority is present (userinfo or port are defined)");
        }
        path = checkHierarchicalPath(scheme, null, userInfo, host, port, path, true);
        String s = toString(scheme,
                            null, userInfo, host, port,
                            path, query, fragment);
        return new Parser(s).parse(true);
    }

    /**
     * Creates a hierarchical IRI from the given components.
     *
     * <p> If a scheme is given then the path, if also given, must either be
     * empty or begin with a slash character ({@code '/'}).  Otherwise a
     * component of the new IRI may be left undefined by passing {@code null}
     * for the corresponding parameter.
     *
     * <p> This method first builds an IRI string from the given components
     * according to the rules specified in <a
     * href="http://www.ietf.org/rfc/rfc3986.txt">RFC&nbsp;3986</a>,
     * section&nbsp;5.3: </p>
     *
     * <ol>
     *
     *   <li><p> Initially, the result string is empty.  </p></li>
     *
     *   <li><p> If a scheme is given then it is appended to the result,
     *   followed by a colon character ({@code ':'}).  </p></li>
     *
     *   <li><p> If an authority is given then the string {@code "//"} is
     *   appended, followed by the authority.  If the authority contains a
     *   literal IPv6 address then the address must be enclosed in square
     *   brackets ({@code '['} and {@code ']'}).  Any character not in the
     *   <i>unreserved</i>, <i>sub-delims</i>, <i>percent-encoded</i>,
     *   or <i>other</i> categories, and not equal to the commercial-at character
     *   ({@code '@'}) or the colon character ({@code ':'}),
     *   is <a href="#quote">quoted</a>.  </p></li>
     *
     *   <li><p> If a path is given then it is appended.  Any character not in
     *   the <i>unreserved</i>, <i>sub-delims</i>, <i>percent-encoded</i>,
     *   or <i>other</i> categories, and not equal to the slash character
     *   ({@code '/'}), the colon character ({@code ':'}), or the
     *   commercial-at character ({@code '@'}), is quoted.  </p></li>
     *
     *   <li><p> If a query is given then a question-mark character
     *   ({@code '?'}) is appended, followed by the query. Any character not in
     *   the <i>unreserved</i>, <i>sub-delims</i>, <i>percent-encoded</i>,
     *   <i>other</i> or <i>private</i> categories, and not equal to the slash
     *   character ({@code '/'}), the colon character ({@code ':'}), the
     *   commercial-at character ({@code '@'}) or the question-mark
     *   character ({@code '?')}, is quoted.
     *   </p></li>
     *
     *   <li><p> Finally, if a fragment is given then a hash character
     *   ({@code '#'}) is appended, followed by the fragment.  Any character not in
     *   the <i>unreserved</i>, <i>sub-delims</i>, <i>percent-encoded</i>,
     *   or <i>other</i> categories, and not equal to the slash
     *   character ({@code '/'}), the colon character ({@code ':'}), the
     *   commercial-at character ({@code '@'}) or the question-mark
     *   character ({@code '?')}, is quoted.  </p></li>
     *
     * </ol>
     *
     * @param   scheme     Scheme name
     * @param   authority  Authority
     * @param   path       Path
     * @param   query      Query
     * @param   fragment   Fragment
     * @return  The new IRI
     *
     * @throws URISyntaxException
     *         If the URI string constructed from the given components violates
     *         RFC&nbsp;3986 or RFC&nbsp;3987
     * @throws IllegalArgumentException if the given arguments are not
     *          consistent. For instance,
     *          if both a scheme and a path are given but the path is relative;
     *          or if no scheme and authority are provided and {@code path} starts with
     *          {@code "//"} or contains {@code ':'} before the first {@code '/'};
     *          or if {@code host} is not {@code null}, {@code scheme} is null,
     *          {@code path} is not empty and does not start with {@code '/'}.
     *
     */
    public static IRI createHierarchical(String scheme,
                                         String authority,
                                         String path,
                                         String query,
                                         String fragment)
        throws URISyntaxException
    {
        path = checkHierarchicalPath(scheme, authority, null, null, -1, path, true);
        String s = toString(scheme,
                            authority, null, null, -1,
                            path, query, fragment);
        return new Parser(s).parse(true);
    }

    /**
     * Creates a hierarchical IRI from the given components.
     *
     * <p> A component may be left undefined by passing {@code null}.
     *
     * <p> This convenience factory method works as if by invoking the
     * seven-argument factory method as follows:
     *
     * <blockquote>
     * {@code new} {@link IRI#createHierarchical(String,String,String,int,String,String,String)
     * IRI.createHierarchical}{@code (scheme, null, host, port, path, null, fragment);}
     * </blockquote>
     *
     * @param   scheme    Scheme name
     * @param   host      Host name
     * @param   port      Port number
     * @param   path      Path
     * @param   fragment  Fragment
     * @return  The new IRI
     *
     * @throws  URISyntaxException
     *          If the URI string constructed from the given components
     *          violates RFC&nbsp;3986 or RFC&nbsp;3987.
     */
    public static IRI createHierarchical(String scheme,
                                         String host,
                                         int port,
                                         String path,
                                         String fragment)
        throws URISyntaxException
    {
        return createHierarchical(scheme, null, host, port, path, null, fragment);
    }

    /**
     * Creates a hierarchical IRI from the given components.
     *
     * <p> A component may be left undefined by passing {@code null}.
     *
     * <p> This convenience factory method works as if by invoking the
     * seven-argument factory method as follows:
     *
     * <blockquote>
     * {@code new} {@link IRI#createHierarchical(String,String,String,int,String,String,String)
     * IRI.createHierarchical}{@code (scheme, null, host, -1, path, null, fragment);}
     * </blockquote>
     *
     * @param   scheme    Scheme name
     * @param   host      Host name
     * @param   path      Path
     * @param   fragment  Fragment
     * @return  The new IRI
     *
     * @throws  URISyntaxException
     *          If the URI string constructed from the given components
     *          violates RFC&nbsp;3986 or RFC&nbsp;3987
     */
    public static IRI createHierarchical(String scheme,
                                         String host,
                                         String path,
                                         String fragment)
        throws URISyntaxException
    {
        return createHierarchical(scheme, null, host, -1, path, null, fragment);
    }

    /**
     * Creates an opaque IRI from the given components.
     *
     * <p> A component may be left undefined by passing {@code null}.
     *
     * <p> This method first builds a URI in string form using the given
     * components as follows:  </p>
     *
     * <ol>
     *
     *   <li><p> Initially, the result string is empty.  </p></li>
     *
     *   <li><p> If a scheme is given then it is appended to the result,
     *   followed by a colon character ({@code ':'}).  </p></li>
     *
     *   <li><p> If an opaque path is given then it is appended.  Any character
     *   not in he <i>unreserved</i>, <i>sub-delims</i>, <i>percent-encoded</i>,
     *   or <i>other</i> categories, and not equal to the slash character
     *   ({@code '/'}), the colon character ({@code ':'}), or the
     *   commercial-at character ({@code '@'}), is quoted.  </p></li>
     *
     *   <li><p> If a query is given then a question-mark character
     *   ({@code '?'}) is appended, followed by the query. Any character not in
     *   the <i>unreserved</i>, <i>sub-delims</i>, <i>percent-encoded</i>,
     *   <i>other</i> or <i>private</i> categories, and not equal to the slash
     *   character ({@code '/'}), the colon character ({@code ':'}), the
     *   commercial-at character ({@code '@'}) or the question-mark
     *   character ({@code '?')}, is quoted.
     *   </p></li>
     *
     *   <li><p> Finally, if a fragment is given then a hash character
     *   ({@code '#'}) is appended, followed by the fragment.  Any character not in
     *   the <i>unreserved</i>, <i>sub-delims</i>, <i>percent-encoded</i>,
     *   or <i>other</i> categories, and not equal to the slash
     *   character ({@code '/'}), the colon character ({@code ':'}), the
     *   commercial-at character ({@code '@'}) or the question-mark
     *   character ({@code '?')}, is quoted.  </p></li>
     *
     * </ol>
     *
     * <p> The resulting URI string is then parsed in order to create the new
     * IRI instance as if by invoking the {@link IRI#parseIRI(String)}
     * factory method; this may cause a {@link URISyntaxException} to be thrown.
     *
     * @param   scheme    Scheme name
     * @param   opaque    Opaque path
     * @param   query     Query
     * @param   fragment  Fragment
     * @return  The new IRI
     *
     * @throws  IllegalArgumentException if {@code scheme} is {@code null}.
     * @throws  URISyntaxException
     *          If the URI string constructed from the given components
     *          violates RFC&nbsp;3986 or RFC&nbsp;3987, or if its path
     *          is not opaque to hierarchy.
     */
    public static IRI createOpaque(String scheme, String opaque, String query, String fragment)
            throws URISyntaxException
    {
        if (scheme == null) {
            throw new IllegalArgumentException("A scheme is required to build an opaque URI");
        }
        IRI iri =  new Parser(toString(scheme, null, null, null,
                -1,  opaque, query, fragment))
                .parse(true);
        if (!iri.isOpaque()) {
            throw new URISyntaxException(iri.toString(), "URI is not opaque");
        }
        return iri;
    }

    /**
     * Converts the given string into an {@code IRI}.
     *
     * <p> This convenience factory method works as if by invoking the
     * {@linkplain IRI#parseIRI(String) parseIRI factory};
     * any {@link URISyntaxException} thrown by {@code parseIRI}
     * is caught and wrapped in a new {@link IllegalArgumentException} object,
     * which is then thrown.
     *
     * <p> This method is provided for use in situations where it is known that
     * the given string is a legal URI, for example with constant IRI literals
     * declared within a program, and so it would be considered a programming
     * error for the string not to parse as such.  The factory methods, which
     * throw {@link URISyntaxException} directly, should be used in situations
     * where a URI is being created from user input or from some other source
     * that may be prone to errors.
     *
     * @param  str   The string to be parsed into an {@code IRI}
     * @return The new {@code IRI}
     *
     * @throws  NullPointerException
     *          If {@code str} is {@code null}
     *
     * @throws  IllegalArgumentException
     *          If the given string violates RFC&nbsp;3986 or RFC&nbsp;3987
     */
    public static IRI of(String str) {
        try {
            return parseIRI(str);
        } catch (URISyntaxException x) {
            throw new IllegalArgumentException(x.getMessage(), x);
        }
    }


    /**
     * Converts the given {@code ResourceIdentifier} into an {@code IRI}.
     *
     * <p> If the given {@link ResourceIdentifier} {@code ri} is an
     * {@code IRI}, returns {@code ri}. Otherwise, returns a new {@code IRI}
     * constructed as if by calling {@link #of(String)
     * IRI.of(ri.toString())}. </p>
     *
     * @apiNote
     * <p> This method is provided for use in situations where an API
     * accepts an abstract {@link ResourceIdentifier} as input, on
     * the condition that it must be convertible to a concrete instance
     * of {@code IRI} for further processing. </p>
     *
     * @param  ri   The {@code ResourceIdentifier} to be converted into
     *              an {@code IRI}
     * @return An {@code IRI} converted from the given {@code ResourceIdentifier}
     *
     * @throws  NullPointerException
     *          If {@code ri} is {@code null}
     *
     * @throws  IllegalArgumentException
     *          If the given {@code ResourceIdentifier} violates RFC&nbsp;3986
     *          or RFC&nbsp;3987 and cannot be converted to an {@code IRI}
     */
    public static IRI of(ResourceIdentifier ri) {
        return (ri instanceof IRI) ? (IRI)ri : IRI.of(ri.toString());
    }



    // -- Operations --

    /**
     * Normalizes this URI's path.
     *
     * <p> If this URI is opaque, or if its path is already in normal form,
     * and its authority is either {@code null} or doesn't end with {@code ':'},
     * then this URI is returned.  Otherwise a new URI is constructed that is
     * identical to this URI except that its authority is normalized by removing
     * the superfluous colon, if any, and its path is computed by normalizing
     * this URI's path in a manner consistent with <a
     * href="http://www.ietf.org/rfc/rfc3986.txt">RFC&nbsp;3986</a>,
     * section&nbsp;5.2.4; that is:
     * </p>
     *
     * <ol>
     *
     *   <li><p> All {@code "."} segments are removed. </p></li>
     *
     *   <li><p> If a {@code ".."} segment is preceded by a non-{@code ".."}
     *   segment then both of these segments are removed.  This step is
     *   repeated until it is no longer applicable. </p></li>
     *
     *   <li><p> If the path is relative, and if its first segment contains a
     *   colon character ({@code ':'}), then a {@code "."} segment is
     *   prepended.  This prevents a relative URI with a path such as
     *   {@code "a:b/c/d"} from later being re-parsed as an opaque URI with a
     *   scheme of {@code "a"} and a scheme-specific part of {@code "b/c/d"}.
     *   <b><i>(Deviation from RFC&nbsp;3986)</i></b> </p></li>
     *
     * </ol>
     *
     * <p> A normalized path will begin with one or more {@code ".."} segments
     * if the URI is relative and there were insufficient non-{@code ".."}
     * segments preceding them to allow their removal.
     * A normalized path will begin with a {@code "."}
     * segment if one was inserted by step 3 above.  Otherwise, a normalized
     * path will not contain any {@code "."} or {@code ".."} segments. </p>
     *
     * @return  An IRI equivalent to this IRI,
     *          but whose path and authority are in normal form
     */
    public IRI normalize() {
        return normalize(this);
    }

    /**
     * Resolves the given IRI against this IRI.
     *
     * <p> If this IRI or the given IRI are not
     * hierarchical, then the given IRI is returned. </p>
     *
     * <p> Otherwise this method constructs a new hierarchical URI in a manner
     * consistent with <a
     * href="http://www.ietf.org/rfc/rfc3986.txt">RFC&nbsp;3986</a>,
     * section&nbsp;5.2; that is: </p>
     *
     * <p> If the given URI's scheme is defined, then a URI with
     * the normalized given path and with all other components equal to
     * those of the given URI is returned. The returned IRI path is
     * normalized as if by invoking the {@link #normalize() normalize}
     * method.</p>
     *
     * <p> Otherwise, a new IRI is constructed with this URI's scheme and the given
     *   URI's fragment component: </p>
     *
     * <ol>
     *
     *   <li><p> If the given URI has an authority component then the new URI's
     *   authority, path and query are taken from the given URI. The returned
     *   IRI path is normalized as if by invoking the {@link
     *   #normalize() normalize} method. </p> </li>
     *
     *   <li><p> Otherwise the new URI's authority component is copied from
     *   this URI, then its path is computed as follows: </p>
     *
     *     <ol>
     *
     *       <li><p> If the given URI's path is absolute then the new URI's path
     *       is taken from the given URI and is normalized as if by invoking the
     *       {@link #normalize() normalize} method. </p></li>
     *
     *       <li><p> Otherwise the given URI's path is relative, and so the new
     *       URI's path is computed by resolving the path of the given URI
     *       against the base path of this URI. If this URI has an authority and
     *       its path is empty then {@code "/"} is taken for the base path.
     *       If this URI path is {@code ".."} or ends with {@code "/.."}
     *       then this URI path appended with {@code "/"} is taken for
     *       the base path. Otherwise the base path is this URI path.
     *       All but the last segment of the base path are then concatenated,
     *       if any, with the given URI's path and the result is then normalized
     *       as if by invoking the {@link #normalize() normalize} method. </p></li>
     *
     *     </ol>
     *
     *     <p> and then its query is computed as: </p>
     *
     *     <ol>
     *
     *       <li><p> If the given URI's path is not empty then the new URI's
     *       query component is always taken from the given URI's query. </p></li>
     *
     *       <li><p> If the given URI's path is empty and query is defined,
     *       the new URI's query component is taken from the given URI's query.
     *       Otherwise, it is taken from the base URI's query. </p></li>
     *
     *     </ol>
     *
     *   </li>
     *
     * </ol>
     *
     * <p> The result of this method is absolute if, and only if, either this
     * IRI is absolute or the given IRI is absolute.  </p>
     *
     * @param  uri  The IRI to be resolved against this IRI
     * @return The resulting IRI
     *
     * @throws  NullPointerException
     *          If {@code uri} is {@code null}
     */
    public IRI resolve(IRI uri) {
        return resolve(this, uri);
    }

    /**
     * Constructs a new IRI by parsing the given string and then resolving it
     * against this IRI.
     *
     * <p> This convenience method works as if invoking it were equivalent to
     * evaluating the expression {@link #resolve(IRI)
     * resolve}{@code (IRI.}{@link #of(String) of}{@code (str))}. </p>
     *
     * @param  str   The string to be parsed into an IRI
     * @return The resulting IRI
     *
     * @throws  NullPointerException
     *          If {@code str} is {@code null}
     *
     * @throws  IllegalArgumentException
     *          If the given string violates RFC&nbsp;3986 or RFC&nbsp;3987
     */
    public IRI resolve(String str) {
        return resolve(IRI.of(str));
    }

    /**
     * Relativizes the given IRI against this IRI.
     *
     * <p> The relativization of the given IRI against this IRI is computed as
     * follows: </p>
     *
     * <ol>
     *
     *   <li><p> If either this IRI or the given IRI are non hierarchical,
     *   or if the scheme and authority components of the two IRIs are not
     *   identical, or if the path of this IRI is not a prefix of the path
     *   of the given IRI, then the given IRI is returned. </p></li>
     *
     *   <li><p> Otherwise a new relative hierarchical IRI is constructed with
     *   query and fragment components taken from the given IRI and with a path
     *   component computed by removing this URI's path from the beginning of
     *   the given URI's path. </p></li>
     *
     *   <li><p> If the resulting URI is relative, has no authority component,
     *   and if the first segment of its path contains a
     *   colon character ({@code ':'}), then a {@code "."} segment is
     *   prepended.  This prevents a relative URI with a path such as
     *   {@code "a:b/c/d"} from later being re-parsed as an opaque URI with a
     *   scheme of {@code "a"} and a scheme-specific part of {@code "b/c/d"}.
     *   </p></li>
     *
     * </ol>
     *
     * @apiNote
     *
     *   In more general terms, if both URIs are hierarchical, their scheme and
     *   authority components are identical, the normalized path of this URI
     *   ( the base URI ) is a prefix of the normalized path of the given URI,
     *   then this method returns a relative-URI that, when resolved against
     *   the base URI, yields the normalized form of the given URI. Otherwise,
     *   it returns the given URI.
     *
     * @param  uri  The IRI to be relativized against this IRI
     * @return The resulting IRI
     *
     * @throws  NullPointerException
     *          If {@code uri} is {@code null}
     */
    public IRI relativize(IRI uri) {
        return relativize(this, uri);
    }

    /**
     * Constructs a URL from this IRI.
     *
     * <p> This convenience method works as if invoking it were equivalent to
     * evaluating the expression {@code new URL(this.toString())} after
     * first checking that this IRI is absolute. </p>
     *
     * @return  A URL constructed from this IRI
     *
     * @throws  IllegalArgumentException
     *          If this URL is not absolute
     *
     * @throws  MalformedURLException
     *          If a protocol handler for the URL could not be found,
     *          or if some other error occurred while constructing the URL
     */
    public URL toURL() throws MalformedURLException {
        if (!isAbsolute()) {
            throw new IllegalArgumentException("URI is not absolute");
        }
        String protocol = getScheme();
        // In general we need to go via Handler.parseURL, but for the jrt
        // protocol we enforce that the Handler is not overrideable and can
        // optimize URI to URL conversion.
        // Case-sensitive comparison for performance; malformed protocols will
        // be handled correctly by the slow path.
        if (protocol.equals("jrt") && !isOpaque()
                && getRawFragment() == null) {
            String query = getRawQuery();
            String path = getRawPath();
            String file = (query == null) ? path : path + "?" + query;
            // URL represent undefined host as empty string while URI use null
            String host = getHost();
            if (host == null) {
                host = "";
            }
            int port = getPort();
            return URI.create("jrt://" + host + ":" + port + "/" + file).toURL();
        } else {
            return URI.create(toString()).toURL();
        }
    }

    // -- Component access methods --

    /**
     * Returns the scheme component of this IRI.
     *
     * <p> The scheme component of a URI, if defined, only contains characters
     * in the <i>alphanum</i> category and in the string {@code "-.+"}.  A
     * scheme always starts with an <i>alpha</i> character. <p>
     *
     * The scheme component of a URI cannot contain percent-encoded octets,
     * hence this method does not perform any decoding.
     *
     * @return  The scheme component of this IRI,
     *          or {@code null} if the scheme is undefined
     */
    @Override
    public String getScheme() {
        return scheme;
    }

    /**
     * Tells whether or not this IRI is absolute.
     *
     * <p> A URI is absolute if, and only if, it has a scheme component. </p>
     *
     * @return  {@code true} if, and only if, this URI is absolute
     */
    @Override
    public boolean isAbsolute() {
        return scheme != null;
    }

    /**
     * Tells whether or not this IRI has a path which is opaque
     * to hierarchy.
     *
     * @implSpec
     * <p> The concept of opaque URI is defined in RFC&nbsp;2396.
     * In RFC&nbsp;3986, the definition of such URI has been replaced with
     * a better description of how the path component may be opaque to
     * hierarchy, i.e. path-rootless rule of RFC&nbsp;3986.
     * This implementation follows the rules of RFC&nbsp;3986 and
     * this method returns {@code true} if this IRI has a scheme, but
     * has no authority and its path isn't empty and doesn't
     * start with {@code '/'}. </p>
     *
     * @return  {@code true} if, and only if, this IRI is has a path
     *          which is opaque to hierarchy.
     */
    @Override
    public boolean isOpaque() {
        // TODO can path be null now? I don't think so...
        return (isAbsolute() && authority == null &&
                !path.equals("") && !path.startsWith("/"));
    }

    /**
     * Returns the raw authority component of this IRI.
     *
     * <p> The authority component of a URI, if defined, only contains the
     * commercial-at character ({@code '@'}) or the colon character
     * ({@code ':'}) and characters in the <i>unreserved</i>, <i>sub-delims</i>,
     * <i>percent-encoded</i>, and <i>other</i> categories.  The authority
     * is further constrained to have valid syntax for its
     * user-information, host, and port components, if present. </p>
     *
     * @return  The raw authority component of this IRI,
     *          or {@code null} if the authority is undefined
     */
    @Override
    public String getRawAuthority() {
        return authority;
    }

    /**
     * Returns the decoded authority component of this IRI.
     *
     * <p> The string returned by this method is equal to that returned by the
     * {@link #getRawAuthority() getRawAuthority} method except that all
     * sequences of percent-encoded octets are <a href="#decode">decoded</a>
     * as specified <a href="#decodemethods">earlier</a>
     * in this document.
     * </p>
     *
     * @return  The decoded authority component of this IRI,
     *          or {@code null} if the authority is undefined
     */
    @Override
    public String getAuthority() {
        String auth = decodedAuthority;
        if ((auth == null) && (authority != null)) {
            decodedAuthority = auth = decode(authority, DecodeInfo.AUTH);
        }
        return auth;
    }

    /**
     * Returns the raw user-information component of this IRI.
     *
     * <p> The user-information component of a URI, if defined, only contains
     * characters in the <i>unreserved</i>, <i>sub-delims</i>, <i>percent-encoded</i>,
     * and <i>other</i> categories, or the colon character ({@code ':'}). </p>
     *
     * @return  The raw user-information component of this IRI,
     *          or {@code null} if the user information is undefined
     */
    @Override
    public String getRawUserInfo() {
        return userInfo;
    }

    /**
     * Returns the decoded user-information component of this IRI.
     *
     * <p> The string returned by this method is equal to that returned by the
     * {@link #getRawUserInfo() getRawUserInfo} method except that all
     * sequences of percent-encoded octets are <a href="#decode">decoded</a>
     * as specified <a href="#decodemethods">earlier</a>
     * in this document.
     * </p>
     *
     * @return  The decoded user-information component of this IRI,
     *          or {@code null} if the user information is undefined
     */
    @Override
    public String getUserInfo() {
        String user = decodedUserInfo;
        if ((user == null) && (userInfo != null)) {
            decodedUserInfo = user = decode(userInfo, DecodeInfo.USER);
        }
        return user;
    }

    /**
     * Returns the raw <i>host</i> component of this IRI.
     *
     * <p> The host component of an IRI, if defined, will have one of the
     * following forms: </p>
     *
     * <ul>
     *
     *   <li><p> A name consisting of characters in the <i>unreserved</i>,
     *   <i>sub-delims</i>, <i>percent-encoded</i>, and <i>other</i> categories.
     *   </p></li>
     *
     *   <li><p> A dotted-quad IPv4 address of the form
     *   <i>digit</i>{@code +.}<i>digit</i>{@code +.}<i>digit</i>{@code +.}<i>digit</i>{@code +},
     *   where no <i>digit</i> sequence is longer than three characters and no
     *   sequence has a value larger than 255. </p></li>
     *
     *   <li><p> An IPv6 address enclosed in square brackets ({@code '['} and
     *   {@code ']'}) and consisting of hexadecimal digits, colon characters
     *   ({@code ':'}), and possibly an embedded IPv4 address.  The full
     *   syntax of IPv6 addresses is specified in <a
     *   href="http://www.ietf.org/rfc/rfc2373.txt"><i>RFC&nbsp;2373: IPv6
     *   Addressing Architecture</i></a>.  </p></li>
     *
     *   <li><p> An IPvFuture address enclosed in square brackets
     *   ({@code '['} and {@code ']'}) and of the form :
     *   <blockquote><pre>
     *      "v" 1*HEXDIG "." 1*( unreserved | sub-delims | ":" )
     *   </pre></blockquote>
     *   </li>
     *
     * </ul>
     *
     * Further information on the exact form of the raw host component of an
     * IRI <i>u</i> can be obtained by calling {@link #getHostType(String)
     * IRI.getHostType(u.getRawHostString())}.
     *
     * @return  The raw host component of this IRI,
     *          or {@code null} if the host component is undefined
     */
    public String getRawHostString() {
        return host;
    }

    /**
     * Returns the decoded <i>host</i> component of this IRI.
     *
     * <p> The string returned by this method is equal to that returned by the
     * {@link #getRawHostString() getRawHostString} method except that all
     * sequences of percent-encoded octets are <a href="#decode">decoded</a>
     * as specified <a href="#decodemethods">earlier</a>
     * in this document.</p>
     *
     * Further information on the exact form of the decoded host component of
     * an IRI <i>u</i> can be obtained by calling {@link #getHostType(String)
     * IRI.getHostType(u.getHostString())}. Note that the raw host component
     * and decoded host component might parse as different types if the raw
     * host component contains superfluous percent-encoding of US-ASCCI
     * characters.
     *
     * @apiNote
     *
     * Because RFC 3986 allows the presence of percent-encoded
     * characters in the raw host component, the string returned by this method
     * can contain any characters, including characters that are not legal
     * in a URI, or in a DNS name. Applications are thus encouraged to
     * further validate the content of the host string before using it.
     * See also the {@link #getHost() IRI.getHost} method.
     *
     * @return  The decoded host component of this IRI,
     *          or {@code null} if the host component is undefined
     */
    public String getHostString() {
        String decoded = decodedHost;
        if ((decoded == null) && (host != null)) {
            decoded = decodedHost = decode(host, DecodeInfo.HOST);
        }
        return decoded;
    }

    /**
     * Returns the decoded host component of this IRI, if it
     * can be parsed as a syntactically valid IPv4 literal address,
     * IPv6 literal address, or a reg-name conforming to the DNS syntax,
     * {@code null} otherwise.
     *
     * @apiNote
     *
     * <p> The string returned by this method is either equal to
     * that returned by the {@link #getHostString() getHostString}
     * method, if {@code IRI.getHostType(getHostString()).isInternetName()}
     * yields true, or {@code null}. </p>
     *
     * <p> This method is provided to avoid misinterpretation of the
     * host component when using the base {@linkplain ResourceIdentifier}
     * abstraction. Because RFC 3986 allows for the presence of
     * percent-encoded triplets in the host component, using the
     * {@linkplain #getHostString() decoded host} string directly without
     * further validation could be dangerous, as a <i>reg-name</i>,
     * once decoded, could contain just any character.
     * On the other hand the string returned by the {@code getHost}
     * method, if not {@code null}, is guaranteed to be a syntactically
     * valid IPv4 literal, bracket-enclosed IPv6 literal, or to be
     * a name conforming to the DNS syntax.
     * APIs that need to deal with internationalized host
     * names are encouraged to make use of the {@link IDN} class
     * to encode the host component prior to creating an IRI, or to
     * make use of the {@link #getHostString()}, {@link #getRawHostString()}
     * and {@link #getHostType(String)} methods in order to figure out
     * whether a host component needs to be {@linkplain IDN IDN} encoded
     * before being resolved into an internet address. </p>
     *
     * @return  The decoded host component of this URI,
     *          or {@code null} if the decoded host component does not
     *          parse as an {@linkplain HostType#isInternetName()
     *          internet name}.
     */
    public String getHost() {
        String decoded = getHostString();
        return getHostType(decoded).isInternetName()
                ? decoded : null;
    }

    /**
     * Returns the port number of this URI.
     *
     * <p> The port component of a URI, if defined, is a non-negative
     * integer. </p>
     *
     * @return  The port component of this URI,
     *          or {@code -1} if the port is undefined
     */
    @Override
    public int getPort() {
        return port;
    }

    /**
     * Returns the raw path component of this URI.
     *
     * <p> The path component of a URI, if defined, only contains the slash
     * character ({@code '/'}), the commercial-at character ({@code '@'}),
     * the colon character ({@code ':'}) and characters in the
     * <i>unreserved</i>, <i>sub-delims</i>, <i>percent-encoded</i>,
     * and <i>other</i> categories. </p>
     *
     * @return  The path component of this URI,
     *          or {@code null} if the path is undefined
     */
    @Override
    public String getRawPath() {
        return path;
    }

    /**
     * Returns the decoded path component of this URI.
     *
     * <p> The string returned by this method is equal to that returned by the
     * {@link #getRawPath() getRawPath} method except that all sequences of
     * percent-encoded octets are <a href="#decode">decoded</a> as specified
     * <a href="#decodemethods">earlier</a> in this document.  </p>
     *
     * @return  The decoded path component of this URI,
     *          or {@code null} if the path is undefined
     */
    @Override
    public String getPath() {
        String decoded = decodedPath;
        if (decoded == null) {
            decodedPath = decoded = decode(path, DecodeInfo.PATH);
        }
        return decoded;
    }

    /**
     * Returns the raw query component of this URI.
     *
     * <p> The query component of a URI, if defined, only contains legal URI
     * characters. </p>
     *
     * @return  The raw query component of this URI,
     *          or {@code null} if the query is undefined
     */
    @Override
    public String getRawQuery() {
        return query;
    }

    /**
     * Returns the decoded query component of this URI.
     *
     * <p> The string returned by this method is equal to that returned by the
     * {@link #getRawQuery() getRawQuery} method except that all sequences of
     * percent-encoded octets are <a href="#decode">decoded</a>
     * as specified <a href="#decodemethods">earlier</a>
     * in this document.  </p>
     *
     * @return  The decoded query component of this URI,
     *          or {@code null} if the query is undefined
     */
    @Override
    public String getQuery() {
        String decoded = decodedQuery;
        if ((decoded == null) && (query != null)) {
            decodedQuery = decoded = decode(query, DecodeInfo.QUERY);
        }
        return decoded;
    }

    /**
     * Returns the raw fragment component of this URI.
     *
     * <p> The fragment component of a URI, if defined, only contains legal URI
     * characters. </p>
     *
     * @return  The raw fragment component of this URI,
     *          or {@code null} if the fragment is undefined
     */
    @Override
    public String getRawFragment() {
        return fragment;
    }

    /**
     * Returns the decoded fragment component of this URI.
     *
     * <p> The string returned by this method is equal to that returned by the
     * {@link #getRawFragment() getRawFragment} method except that all
     * sequences of percent-encoded octets are <a href="#decode">decoded</a>
     * as specified <a href="#decodemethods">earlier</a>
     * in this document.
     * </p>
     *
     * @return  The decoded fragment component of this URI,
     *          or {@code null} if the fragment is undefined
     */
    @Override
    public String getFragment() {
        String decoded = decodedFragment;
        if ((decoded == null) && (fragment != null)) {
            decodedFragment = decoded = decode(fragment, DecodeInfo.FRAG);
        }
        return decoded;
    }


    // -- Equality, comparison, hash code, toString, and serialization --

    /**
     * Tests this URI for equality with another object.
     *
     * <p> If the given object is not a URI then this method immediately
     * returns {@code false}.
     *
     * <p> For two URIs to be considered equal requires that either both are
     * opaque or both are hierarchical.  Their schemes must either both be
     * undefined or else be equal without regard to case. Their fragments
     * must either both be undefined or else be equal.
     *
     * <p> For two opaque URIs to be considered equal, their scheme-specific
     * parts must be equal.
     *
     * <p> For two hierarchical URIs to be considered equal, their paths must
     * be equal and their queries must either both be undefined or else be
     * equal.  Their authorities must either both be undefined, or both
     * their hosts must be equal without regard to case, their port numbers
     * must be equal, and their user-information components must be equal.
     *
     * <p> When testing the user-information, path, query, fragment, authority,
     * or scheme-specific parts of two URIs for equality, the decoded forms rather
     * than the raw forms of these components are compared.
     *
     * <p> This method satisfies the general contract of the {@link
     * Object#equals(Object) Object.equals} method. </p>
     *
     * @param   obj  The object to which this object is to be compared
     *
     * @return  {@code true} if, and only if, the given object is a URI that
     *          is identical to this URI
     */
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (!(obj instanceof IRI))
            return false;

        IRI that = (IRI)obj;
        this.ensureComponentDecoded();
        that.ensureComponentDecoded();

        if (this.isOpaque() != that.isOpaque()) return false;

        if (!equalIgnoringCase(this.scheme, that.scheme)) return false;
        if (!equal(this.decodedFragment, that.decodedFragment)) return false;

        // Hierarchical or Opaque
        if (!equal(this.decodedPath, that.decodedPath)) return false;
        if (!equal(this.decodedQuery, that.decodedQuery)) return false;

        // Opaque will stop there as both authorities should be null
        if (this.authority == that.authority) return true;

        // Hierarchical
        if (this.host != null) {
            // Server-based
            if (!equal(this.decodedUserInfo, that.decodedUserInfo)) return false;
            if (!equalIgnoringCase(this.decodedHost, that.decodedHost)) return false;
            if (this.port != that.port) return false;
            assert IRI.getHostType(decodedHost) == IRI.getHostType(that.decodedHost);
        } else {
            assert this.userInfo == null;
            assert this.port == -1;
            assert this.authority == null;
            assert that.authority != null;
            // this.authority is null but not that.
            return false;
        }

        return true;
    }

    private void ensureComponentDecoded() {
        if ((decodedUserInfo == null) && (userInfo != null)) {
            decodedUserInfo = decode(userInfo, DecodeInfo.USER);
        }
        if ((decodedAuthority == null) && (authority != null)) {
            decodedAuthority = decode(authority, DecodeInfo.AUTH);
        }
        if ((decodedHost == null) && (host != null)) {
            decodedHost = decode(host, DecodeInfo.HOST);
        }
        if (decodedPath == null) {
            decodedPath = decode(path, DecodeInfo.PATH);
        }
        if ((decodedQuery == null) && (query != null)) {
            decodedQuery = decode(query, DecodeInfo.QUERY);
        }
        if ((decodedFragment == null) && (fragment != null)) {
            decodedFragment = decode(fragment, DecodeInfo.FRAG);
        }
    }

    /**
     * Returns a hash-code value for this URI.  The hash code is based upon all
     * of the URI's components, and satisfies the general contract of the
     * {@link Object#hashCode() Object.hashCode} method.
     *
     * @return  A hash-code value for this URI
     */
    public int hashCode() {
        int h = hash;
        if (h == 0) {
            ensureComponentDecoded();
            h = hashIgnoringCase(0, scheme);
            h = hash(h, decodedFragment);
            h = hash(h, decodedPath);
            h = hash(h, decodedQuery);
            if (host != null) {
                h = hash(h, decodedUserInfo);
                h = hashIgnoringCase(h, decodedHost);
                h += 1949 * port;
            } else {
                h = hash(h, decodedAuthority);
            }

            if (h != 0) {
                hash = h;
            } else {
                // don't allow 0 to avoid hashing again
                hash = 0x0DEC0DED;
            }
        }
        return h;
    }

    /**
     * Compares this URI to another object, which must be a URI.
     *
     * <p> When comparing corresponding components of two URIs, if one
     * component is undefined but the other is defined then the first is
     * considered to be less than the second.  Unless otherwise noted, string
     * components are ordered according to their natural, case-sensitive
     * ordering as defined by the {@link String#compareTo(Object)
     * String.compareTo} method.  String components that are subject to
     * encoding are compared by comparing their raw forms rather than their
     * encoded forms.
     *
     * <p> The ordering of URIs is defined as follows: </p>
     *
     * <ul>
     *
     *   <li><p> Two URIs with different schemes are ordered according the
     *   ordering of their schemes, without regard to case. </p></li>
     *
     *   <li><p> A hierarchical URI is considered to be less than an opaque URI
     *   with an identical scheme. </p></li>
     *
     *   <li><p> Two opaque URIs with identical schemes are ordered according
     *   to the ordering of their paths; if their paths are identical then they
     *   are ordered according to the ordering of their queries; if the queries
     *   are identical then they are ordered according to the order of their
     *   fragments. </p></li>
     *
     *   <li><p> Two hierarchical URIs with identical schemes are ordered
     *   according to the ordering of their authority components: </p>
     *
     *   <ul>
     *
     *     <li><p> If both authority components are defined then the URIs
     *     are ordered according to their user-information components; if these
     *     components are identical then the URIs are ordered according to the
     *     ordering of their hosts, without regard to case; if the hosts are
     *     identical then the URIs are ordered according to the ordering of
     *     their ports. </p></li>
     *
     *     <li><p> If only one authority component is defined,
     *     the URI with an undefined ({@code null}) authority compares
     *     less than the other.</p></li>
     *
     *   </ul></li>
     *
     *   <li><p> Finally, two hierarchical URIs with identical schemes and
     *   authority components are ordered according to the ordering of their
     *   paths; if their paths are identical then they are ordered according to
     *   the ordering of their queries; if the queries are identical then they
     *   are ordered according to the order of their fragments. </p></li>
     *
     * </ul>
     *
     * <p> This method satisfies the general contract of the {@link
     * Comparable#compareTo(Object) Comparable.compareTo}
     * method. </p>
     *
     * @param   that
     *          The object to which this URI is to be compared
     *
     * @return  A negative integer, zero, or a positive integer as this URI is
     *          less than, equal to, or greater than the given URI
     *
     * @throws  ClassCastException
     *          If the given object is not a URI
     */
    public int compareTo(IRI that) {
        int c;
        if ((c = compareIgnoringCase(this.scheme, that.scheme)) != 0)
            return c;

        this.ensureComponentDecoded();
        that.ensureComponentDecoded();

        if (this.isOpaque()) {
            if (that.isOpaque()) {
                // Both opaque
                if ((c = compare(this.decodedPath, that.decodedPath)) != 0)
                    return c;
                if ((c = compare(this.decodedQuery, that.decodedQuery)) != 0)
                    return c;
                return compare(this.decodedFragment, that.decodedFragment);
            }
            return +1;                  // Opaque > hierarchical
        } else if (that.isOpaque()) {
            return -1;                  // Hierarchical < opaque
        }

        // Hierarchical
        if ((this.host != null) && (that.host != null)) {
            // Both server-based
            if ((c = compare(this.decodedUserInfo, that.decodedUserInfo)) != 0)
                return c;
            if ((c = compareIgnoringCase(this.decodedHost, that.decodedHost)) != 0)
                return c;
            if ((c = this.port - that.port) != 0)
                return c;
            assert IRI.getHostType(decodedHost).compareTo(IRI.getHostType(decodedHost)) == 0;
        } else {
            assert this.authority == null || that.authority == null;
            // At least one authority component must be null. Find out which
            if ((c = compare(this.decodedAuthority, that.decodedAuthority)) != 0) return c;
        }

        if ((c = compare(this.decodedPath, that.decodedPath)) != 0) return c;
        if ((c = compare(this.decodedQuery, that.decodedQuery)) != 0) return c;
        return compare(this.decodedFragment, that.decodedFragment);
    }

    /**
     * Returns the content of this URI as a string in its original Unicode form.
     *
     * <p> If this URI was created by invoking one of the factory methods in this
     * class then a string equivalent to the original input string, or to the
     * string computed from the originally-given components, as appropriate, is
     * returned.  Otherwise this URI was created by normalization, resolution,
     * or relativization, and so a string is constructed from this URI's
     * components according to the rules specified in <a
     * href="http://www.ietf.org/rfc/rfc2396.txt">RFC&nbsp;3986</a>,
     * section&nbsp;5.3. </p>
     *
     * @return  The string form of this URI, in its original Unicode form.
     */
    public String toString() {
        String s = string;
        if (s == null) {
            s = defineString();
        }
        return s;
    }

    private static StringBuilder buildString(StringBuilder sb,
                                             String scheme,
                                             String userInfo, String host, int port,
                                             String path,
                                             String query, String fragment) {
        if (scheme != null) {
            sb.append(scheme);
            sb.append(':');
        }
        if (host != null) {
            sb.append("//");
            if (userInfo != null) {
                sb.append(userInfo);
                sb.append('@');
            }
            boolean needBrackets = ((host.indexOf(':') >= 0)
                    && !host.startsWith("[")
                    && !host.endsWith("]"));
            if (needBrackets) sb.append('[');
            sb.append(host);
            if (needBrackets) sb.append(']');
            if (port != -1) {
                sb.append(':');
                sb.append(port);
            }
        }
        if (path != null)
            sb.append(path);
        if (query != null) {
            sb.append('?');
            sb.append(query);
        }
        if (fragment != null) {
            sb.append('#');
            sb.append(fragment);
        }
        return sb;
    }

    // used by resolve()
    private static StringBuilder buildString(StringBuilder sb,
                                             String scheme,
                                             String authority,
                                             String path,
                                             String query, String fragment) {
        if (scheme != null) {
            sb.append(scheme);
            sb.append(':');
        }
        if (authority != null) {
            sb.append("//");
            sb.append(authority);
        }
        if (path != null)
            sb.append(path);
        if (query != null) {
            sb.append('?');
            sb.append(query);
        }
        if (fragment != null) {
            sb.append('#');
            sb.append(fragment);
        }
        return sb;
    }

    private String defineString() {
        String s = string;
        if (s != null) {
            return s;
        }

        assert host != null || authority == null;
        string = s = buildString(new StringBuilder(),
                scheme,
                userInfo, host, port,
                path, query, fragment).toString();
        return s;
    }

    /**
     * Returns a representation of this IRI that only contains US-ASCII
     * characters.  Conceptually mapping an IRI representation to its URI
     * representation.
     *
     * <p> If this IRI does not contain any characters in the <i>other</i>
     * category then an invocation of this method will return the same value as
     * an invocation of the {@link #toString() toString} method.  Otherwise
     * this method works as if by invoking that method and then <a
     * href="#encode">encoding</a> the result.  </p>
     *
     * @return  The string form of this URI, encoded as needed
     *          so that it only contains US-ASCII characters.
     */
    @Override
    public String toASCIIString() {
        return encode(toString());
    }


    /**
     * Returns a decoded representation of this IRI that may contain unicode
     * characters.  Conceptually mapping a URI representation to its IRI
     * representation.
     *
     * <p> If this URI does not contain any percent-encoded octets for all
     * components, then an invocation of this method will return the same
     * value as an invocation of the {@link #toString() toString} method.
     * Otherwise percent-encoded <i>other</i> characters are converted to
     * Unicode. Any percent-encoded octets which do not represent valid
     * UTF-8 characters or which represent <i>reserved</i> characters or
     * which are not allowed in IRIs, e.g. bidirectional formatting characters
     * (LRM, RLM, LRE, RLE, LRO, RLO, and PDF), are not converted.
     *
     * @return  The content of this URI as an IRI string,
     *          i.e. may contain non-US-ASCII characters.
     *
     */
    public String toIRIString() {
        defineIRIString();
        return iriString;
    }

    /**
     * Returns a decoded representation of this IRI that may contain
     * unicode characters, as well as printable non-US-ASCII
     * characters which would have been accepted by a
     * {@linkplain #parseLenient(String) lenient parser}.
     *
     * <p> This method acts as {@link #toString()}, but additionally
     * {@linkplain #unquoteLenient(String) leniently decodes printable
     * characters in US-ASCII that are not allowed in URIs}, but which are
     * sometime used in queries (such as the pipe ({@code '|'}) character).
     *
     * @apiNote Note the resulting string may no longer be a
     * valid IRI, as it may contain characters that are not valid in URIs,
     * and could lead to dangerous misinterpretation if used in a wider
     * context (such as an XML document) without precaution.
     *
     * @implSpec
     * <p>This method is provided as a convenience method and is
     * equivalent to calling {@code IRI.unquoteLenient(iri.toString())}.
     * In addition for any string <i>str</i> for which
     * {@code IRI.parseLenient(}<i>str</i>{@code)} doesn't fail, and then
     * <pre>IRI.parseLenient(str).toLenientString().equals(IRI.unquoteLenient(str))</pre>
     *
     * @return Returns a decoded representation of this IRI that
     * may contain unicode characters, as well as printable non-US-ASCII
     * character which are normally not allowed in an IRI or URI.
     *
     * @see #unquoteLenient(String)
     * @see #parseLenient(String)
     * @see #quoteLenient(String)
     */
    public String toLenientString() {
        defineString();
        // further leniently decode printable US-ASCII characters
        // which are normally not used in URI.
        return decode(string, DecodeInfo.LENIENT);
    }

    // public static helpers

    /**
     * Leniently pre-quote illegal printable US-ASCII characters.
     *
     * @apiNote
     * RFC 3987 specifies that systems accepting IRIs may
     * also deal with the printable characters in US-ASCII that are not
     * allowed in URIs, namely {@code '<'}, {@code '>'}, {@code '"'},
     * space, {@code '{'}, {@code '}'}, {@code '|'}, {@code '\'},
     * {@code '^'}, and {@code '`'}. This method will leniently pre-quote
     * these characters in the given string {@code s} so that the resulting
     * string can be passed to {@link IRI#parseIRI(String)} or
     * {@link #of(String)} without triggering an exception when
     * they are encountered.
     *
     * <p> This method is provided as convenience for those APIs
     * that produce strings in which these characters appear in raw
     * unquoted form in IRI strings or IRI components.
     * Note that blindly applying  this method to any string
     * whithout prior validation is not encouraged as the presence of
     * these character in an IRI string could potentially be due to
     * malicious input.
     *
     * @param s The string in which illegal printable US-ASCII characters
     *          should be leniently pre-quoted. May be {@code null}, in which
     *          case {@code null} is returned.
     *
     * @return A string in which illegal printable US-ASCII characters
     *         have been leniently pre-quoted.
     *
     * @see #unquoteLenient(String)
     * @see #parseLenient(String)
     * @see #toLenientString()
     */
    public static String quoteLenient(String s) {
        return quote(s, L_LENIENT, H_LENIENT, NonASCII.NOQUOTES);
    }

    /**
     * Leniently unquote illegal printable US-ASCII characters.
     *
     * @apiNote
     * RFC 3987 specifies that systems accepting IRIs may
     * also deal with the printable characters in US-ASCII that are not
     * allowed in URIs, namely {@code '<'}, {@code '>'}, {@code '"'},
     * space, {@code '{'}, {@code '}'}, {@code '|'}, {@code '\'},
     * {@code '^'}, and {@code '`'}. This method will leniently unquote
     * these characters if present in quoted form in the given string
     * {@code s}, performing the opposite operation that
     * {@link #quoteLenient(String)} might have done.
     *
     * <p> Note however that when applied to the full string
     * representation of an IRI, such as returned by
     * {@link #toString()}, {@link #toASCIIString()}, or
     * {@link #toIRIString()}, the resulting string may no longer be a
     * valid IRI, as it may contain characters that are not valid in URIs,
     * and could lead to dangerous misinterpretation if used in a wider
     * context (such as an XML document) without precaution.
     *
     * @param s The string in which illegal printable US-ASCII characters
     *          should be leniently pre-quoted. May be {@code null}, in which
     *          case {@code null} is returned.
     *
     * @return A string in which illegal printable US-ASCII characters
     *         have been leniently pre-quoted.
     *
     * @see #quoteLenient(String)
     * @see #parseLenient(String)
     * @see #toLenientString()
     */
    public static String unquoteLenient(String s) {
        return decode(s, DecodeInfo.LENIENT);
    }

    /**
     * Substitutes the percent character of any percent encoded octet found in
     * {@code s} with "%25", thus re-encoding any percent encoded octet {@code %hh}
     * into {@code %25hh}.
     *
     * @apiNote
     * This method can be used on input arguments prior to calling a
     * <a href="#multiargs">multi-argument factory method</a>
     * if the behaviour that was previously implemented by
     * {@code java.net.URI} multi-argument constructors is desired.
     *
     * @implNote
     * This method behaves as {@code String.replaceAll("%(([0-9]|[a-fA-F]){2})", "%25$1")}.
     *
     * @param s an input string
     *
     * @return a string in which any percent encoded octets are quoted
     **/
    public static String quoteEncodedOctets(String s) {
        int start=0, n=0, len=-1;
        StringBuilder sb = null;
        while ((n = s.indexOf('%', n)) > -1) {
            if (len == -1) len = s.length();
            if (n < len -2) {
                char c1, c2;
                if (match(c1=s.charAt(n+1), L_HEX, H_HEX)
                    && match(c2=s.charAt(n+2), L_HEX, H_HEX)) {
                    if (sb == null) {
                        sb = new StringBuilder(len + 3);
                    }
                    sb.append(s, start, n);
                    sb.append('%');
                    sb.append('2');
                    sb.append('5');
                    sb.append(c1);
                    sb.append(c2);
                    start = n = n+3;
                } else n++;
            } else {
                break;
            }
        }
        if (sb != null && start < len) {
            sb.append(s, start, len);
        }
        return sb == null ? s : sb.toString();
    }

    /**
     * Decodes any percent encoded octet corresponding to a valid UTF-8 sequence.
     * If {@code useReplacementChar} is true, then any triplet corresponding to
     * an invalid UTF-8 octet will be replaced by the replacement char {@code U+FFFD}.
     * Otherwise, the invalid triplet is simply preserved.
     *
     * @apiNote
     * This method can be used on the result returned by the
     * <a href="#rawgetters">raw getters</a>
     * if the behaviour that was previously implemented by {@code java.net.URI}
     * getters is desired.
     *
     * @param s a string such as returned by non-raw getters.
     * @param useReplacementChar whether an invalid percent-encoded octet should be
     *                           replaced with the replacement char {@code U+FFFD}
     *
     * @return a string in which all valid percent encoded sequences have been
     *         been decoded.
     **/
    public static String unquoteEncodedOctets(String s, boolean useReplacementChar) {
        DecodeInfo info = useReplacementChar
                ? DecodeInfo.REPLACE_INVALID : DecodeInfo.ALL_VALID;
        return decode(s, info);
    }

    // -- Serialization support --

    /**
     * Saves the content of this URI to the given serial stream.
     *
     * <p> The only serializable field of a URI instance is its {@code string}
     * field.  That field is given a value, if it does not have one already,
     * and then the {@link ObjectOutputStream#defaultWriteObject()}
     * method of the given object-output stream is invoked. </p>
     *
     * @param  os  The object-output stream to which this object
     *             is to be written
     */
    private void writeObject(ObjectOutputStream os)
        throws IOException
    {
        defineString();
        os.defaultWriteObject();        // Writes the string field only
    }

    /**
     * Reconstitutes a URI from the given serial stream.
     *
     * <p> The {@link ObjectInputStream#defaultReadObject()} method is
     * invoked to read the value of the {@code string} field.  The result is
     * then parsed in the usual way.
     *
     * @param  is  The object-input stream from which this object
     *             is being read
     */
    private void readObject(ObjectInputStream is)
        throws ClassNotFoundException, IOException
    {
        is.defaultReadObject();
    }

    /**
     * Returns the {@code IRI} resulting from the parsed IRI-string.
     * @return the {@code IRI} resulting from the parsed IRI-string
     */
    private Object readResolve() throws ObjectStreamException {
        try {
            return new Parser(string).parse(true);
        } catch (URISyntaxException x) {
            InvalidObjectException y = new InvalidObjectException("Invalid URI");
            y.initCause(x);
            throw y;
        }
    }


    // -- End of public methods --


    // -- Utility methods for string-field comparison and hashing --

    // These methods return appropriate values for null string arguments,
    // thereby simplifying the equals, hashCode, and compareTo methods.

    // US-ASCII only
    private static int toLower(char c) {
        if ((c >= 'A') && (c <= 'Z'))
            return c + ('a' - 'A');
        return c;
    }

    // US-ASCII only
    private static int toUpper(char c) {
        if ((c >= 'a') && (c <= 'z'))
            return c - ('a' - 'A');
        return c;
    }

    private static boolean equal(String s, String t) {
        if (s == t) return true;
        if ((s != null) && (t != null)) {
            if (s.length() != t.length())
                return false;
            if (s.indexOf('%') < 0)
                return s.equals(t);
            int n = s.length();
            for (int i = 0; i < n;) {
                char c = s.charAt(i);
                char d = t.charAt(i);
                if (c != '%') {
                    if (c != d)
                        return false;
                    i++;
                    continue;
                }
                if (d != '%')
                    return false;
                i++;
                if (toLower(s.charAt(i)) != toLower(t.charAt(i)))
                    return false;
                i++;
                if (toLower(s.charAt(i)) != toLower(t.charAt(i)))
                    return false;
                i++;
            }
            return true;
        }
        return false;
    }

    private static boolean equalIgnoringCase(String s, String t) {
        if (s == t) return true;
        if ((s != null) && (t != null)) {
            return s.equalsIgnoreCase(t);
        }
        return false;
    }

    private static int hash(int hash, String s) {
        if (s == null) return hash;
        return s.indexOf('%') < 0 ? hash * 127 + s.hashCode()
                                  : normalizedHash(hash, s);
    }


    // US-ASCII only
    private static int normalizedHash(int hash, String s) {
        int h = 0;
        for (int index = 0; index < s.length(); index++) {
            char ch = s.charAt(index);
            h = 31 * h + ch;
            if (ch == '%') {
                /*
                 * Process the next two encoded characters
                 */
                for (int i = index + 1; i < index + 3; i++)
                    h = 31 * h + toUpper(s.charAt(i));
                index += 2;
            }
        }
        return hash * 127 + h;
    }

    private static int hashIgnoringCase(int hash, String s) {
        if (s == null) return hash;
        return hash * 31 + s.toLowerCase().hashCode();
    }

    private static int compare(String s, String t) {
        if (s == t) return 0;
        if (s != null) {
            if (t != null)
                return s.compareTo(t);
            else
                return +1;
        } else {
            return -1;
        }
    }

    private static int compareIgnoringCase(String s, String t) {
        if (s == t) return 0;
        if (s != null) {
            if (t != null) {
                return s.compareToIgnoreCase(t);
            }
            return +1;
        } else {
            return -1;
        }
    }


    // -- String construction --

    // If a scheme is given then the path, if given, must be absolute
    //
//    private static void checkPath(String s, String scheme, String path)
//            throws URISyntaxException
//    {
//        if (scheme != null) {
//            if ((path != null)
//                    && ((path.length() > 0) && (path.charAt(0) != '/')))
//                throw new URISyntaxException(s,
//                        "Relative path in absolute URI");
//        }
//    }

    // check consistency of hierarchical IRI parameters
    // If scheme and authority are absent, and the path contains
    // a : then it needs to be protected
    //
    private static String checkHierarchicalPath(String scheme, String authority, String userinfo,
                                                String host, int port, String path,
                                                boolean reject)
    {
        if (path == null) return path;
        if (authority != null || host != null) {
            if (!path.isEmpty() && path.charAt(0) != '/') {
                assert reject; // components should have been checked if reject=false;
                throw new IllegalArgumentException(
                        "relative path with non null authority component");
            }
            return path;
        }
        if (path.startsWith("//")) {
            assert reject; // components should have been checked if reject=false;
            throw new IllegalArgumentException(
                    "path cannot start with // when no authority is provided");
        }
        if (scheme != null) {
            if (!path.isEmpty() && path.charAt(0) != '/') {
                assert reject; // components should have been checked if reject=false;
                throw new IllegalArgumentException(
                        "hierarchical path must be absolute or empty when a scheme is provided");
            }
            return path;
        }

        // now we have a null scheme, and a null authority - so a
        // path starting with xxxx:vvvv could be parsed as a
        // scheme.
        int qm, ps=-1;
        if ((qm = path.indexOf(':')) > -1 &&
                ((ps = path.indexOf('/')) > qm || ps == -1)) {
            // if the path is of the form xxxx:vvv where none of
            // the x is a slash then we need to protect the path
            // by prepending "./" to avoid having it interpreted
            // as a scheme.
            // Alternatively - we could choose to throw IAE?
            if (reject)
                throw new IllegalArgumentException(
                        "path should start with \"./\" if its first segment has a ':'");
            path = "./" + path;
        }
        return path;
    }

    private static void appendAuthority(StringBuilder sb,
                                        String authority,
                                        String userInfo,
                                        String host,
                                        int port)
    {
        if (host != null) {
            sb.append("//");
            if (userInfo != null) {
                sb.append(quote(userInfo, L_USERINFO, H_USERINFO));
                sb.append('@');
            }
            boolean needBrackets = ((host.indexOf(':') >= 0)
                                    && !host.startsWith("[")
                                    && !host.endsWith("]")
                                    && getHostType(host, false).isLiteral());
            if (needBrackets) sb.append('[');
            sb.append(quoteHost(host));
            if (needBrackets) sb.append(']');
            if (port != -1) {
                sb.append(':');
                sb.append(port);
            }
        } else if (authority != null) {
            sb.append("//");
            if (!appendIfIPv6Literal(sb, authority,
                                     L_AUTHORITY,
                                     H_AUTHORITY))
            {
                sb.append(quote(authority,
                                L_AUTHORITY,
                                H_AUTHORITY));
            }
        }
    }

    //
    // if given authority contains IPv6 literal, append it to
    // sb and return true;
    // otherwise, do nothing but return false
    //
    private static boolean appendIfIPv6Literal(StringBuilder sb,
                                               String authority,
                                               long lowMask,
                                               long highMask)
    {
        String doquote, dontquote;
        if (!authority.isEmpty() && authority.charAt(0) == '[') {
            int end = authority.indexOf(']');
            if (end != -1 && isIPLiteralAddress(
                    dontquote = authority.substring(0, end+1))) {
                if (end == authority.length()) {
                    doquote = "";
                } else {
                    doquote = authority.substring(end + 1);
                }
                sb.append(dontquote);
                sb.append(quote(doquote,
                        lowMask,
                        highMask));
                return true;
            }
        } else {
            //
            // don't quote IPv6 address piece inside authority
            // if given authority = 'userinfo@[IPV6]:port'
            //
            int start = authority.indexOf("@[");
            int end = start == -1 ? -1 : authority.indexOf(']', start);
            if (start != -1 && end != -1
                    && isIPLiteralAddress(
                            dontquote = authority.substring(start+1, end+1))) {
                if (start > 0) {
                    sb.append(quote(authority.substring(0, start),
                            L_USERINFO, H_USERINFO));
                }
                sb.append('@');
                sb.append(dontquote);
                sb.append(quote(authority.substring(end+1), lowMask, highMask));
                return true;
            }
        }

        return false;
    }

    private static String toString(String scheme,
                                   String authority,
                                   String userInfo,
                                   String host,
                                   int port,
                                   String path,
                                   String query,
                                   String fragment)
    {
        StringBuilder sb = new StringBuilder();

        // append scheme
        if (scheme != null) {
            sb.append(scheme);
            sb.append(':');
        }

        // append authority
        appendAuthority(sb, authority, userInfo, host, port);

        // append path
        if (path != null) {
            sb.append(quote(path, L_PATH, H_PATH));
        }

        // append query
        if (query != null) {
            sb.append('?');
            sb.append(quote(query, L_QUERY, H_QUERY, NonASCII.QUERY));
        }

        // append fragment
        if (fragment != null) {
            sb.append('#');
            sb.append(quote(fragment, L_FRAGMENT, H_FRAGMENT));
        }

        // done...
        return sb.toString();
    }

    private void defineIRIString() {
        if (iriString != null) return;
        // Use decodeIRI to preserve reentrant IRI strings.
        // The main difference is that %25 will always be preserved
        // when it's followed by two hex digits.
        // So if you have something like %2541 or %25%34%31 it will
        // produce %2541 in the IRI string, not %41, ensuring that
        // parsing the IRI string and echoing it back produces the
        // same IRI string.
        iriString = toString(scheme,
                             decodeIRI(authority, DecodeInfo.AUTH),
                             decodeIRI(userInfo, DecodeInfo.USER),
                             decodeIRI(host, DecodeInfo.HOST),
                             port,
                             decodeIRI(path, DecodeInfo.PATH),
                             decodeIRI(query, DecodeInfo.QUERY),
                             decodeIRI(fragment, DecodeInfo.FRAG));
    }

    // -- Normalization, resolution, and relativization --

    // RFC3986 sec. 5.2
    private static String resolvePath(String base, String child,
                                      boolean absolute)
    {
        int i = base.lastIndexOf('/');
        int cn = child.length();
        String path = "";

        if (cn == 0) {
            // 5.2 (6a)
            if (i >= 0)
                path = base.substring(0, i + 1);
        } else {
            StringBuilder sb = new StringBuilder(base.length() + cn);
            // 5.2 (6a)
            if (i >= 0)
                sb.append(base, 0, i + 1);
            // 5.2 (6b)
            sb.append(child);
            path = sb.toString();
        }

        // 5.2 (6c-f)
        String np = normalize(path);

        // 5.2 (6g): If the result is absolute but the path begins with "../",
        // then we simply leave the path as-is

        return np;
    }

    // RFC3986 sec. 5.2
    private static IRI resolve(IRI base, IRI child) {
        // check if child if opaque first so that NPE is thrown
        // if child is null.
        if (child.isOpaque() || base.isOpaque())
            return child;

        // 5.2.2  Target URI fields
        String scheme;
        String authority;
        String userInfo ;
        String host ;
        int port;
        String path;
        String query;
        String fragment;

        if (child.scheme != null) {
            scheme = child.scheme;
            authority = child.authority;
            userInfo = child.userInfo;
            host = child.host;
            port = child.port;
            path = normalize(child.path);
            query = child.query;
        } else {
            if (child.authority != null) {
                authority = child.authority;
                userInfo = child.userInfo;
                host = child.host;
                port = child.port;
                path = normalize(child.path);
                query = child.query;
            } else {
                if (child.path.isEmpty()) {
                    path = base.path;
                    query = (child.query != null) ? child.query : base.query;
                } else {
                    String p;
                    if (child.path.charAt(0) == '/') {
                        p = child.path;
                    } else {
                        p = mergePath(base, child);
                    }
                    path = normalize(p);
                    query = child.query;
                }
                authority = base.authority;
                userInfo = base.userInfo;
                host = base.host;
                port = base.port;
            }
            scheme = base.scheme;
        }

        fragment = child.fragment;

        // don't normalize authority
        String input = (authority == null || !authority.endsWith(":"))
                ? null // no need to eagerly define the string rep.
                : buildString(new StringBuilder(),
                    scheme, authority, path, query, fragment).toString();

        return new IRI(input,
                       scheme,
                       authority,
                       userInfo,
                       host,
                       port,
                       path,
                       query,
                       fragment);
    }

    // RFC 3986 5.2.3
    private static String mergePath(IRI base, IRI child) {
        StringBuilder sb = new StringBuilder();

        if (base.authority != null && base.path.equals("")) {
            sb.append('/').append(child.path);
            return sb.toString();
        } else if (base.path.endsWith("/..")
                || base.path.equals("..")) {
            sb.append(base.path)
                    .append('/')
                    .append(child.path);
            return sb.toString();
        } else {
            int index = base.path.lastIndexOf('/');
            if (index != -1) {
                sb.append(base.path.substring(0, index))
                        .append('/')
                        .append(child.path);
                return sb.toString();
            } else {
                return child.path;
            }
        }
    }

    // If the given URI's path is normal then return the URI;
    // o.w., return a new URI containing the normalized path.
    //
    private static IRI normalize(IRI u) {
        if (u.isOpaque()) return u;

        // normalize authority by removing superfluous colon
        String na = (u.authority == null || !u.authority.endsWith(":"))
                ? u.authority
                : u.authority.substring(0, u.authority.length() -1);

        // normalize path
        String np;
        if (u.path.isEmpty()) {
            np = u.path;
        }  else {
            np = normalize(u.path);
        }

        if (u.authority != null) {
            // RFC 3986: 6.2.3.  Scheme-Based Normalization:
            // In general, a URI that uses the generic syntax for
            // authority with an empty path should be normalized
            // to a path of "/".
            if (np == null || np.isEmpty()) {
                np = "/";
            }
        }

        // if nothing changed, we're done!
        if (np == u.path && na == u.authority) {
            return u;
        }

        if (u.scheme == null) {
            np = checkHierarchicalPath(u.scheme, na, u.userInfo, u.host,
                    u.port, np, false);
        }

        return new IRI(null,
                       u.scheme,
                       na,
                       u.userInfo,
                       u.host,
                       u.port,
                       np,
                       u.query,
                       u.fragment);
    }

    // If both URIs are hierarchical, their scheme and authority components are
    // identical, and the base path is a prefix of the child's path, then
    // return a relative URI that, when resolved against the base, yields the
    // child; otherwise, return the child.
    //
    private static IRI relativize(IRI base, IRI child) {
        // check if child if opaque first so that NPE is thrown
        // if child is null.
        if (child.isOpaque() || base.isOpaque())
            return child;
        if (!equalIgnoringCase(base.scheme, child.scheme)
            || !equal(base.authority, child.authority))
            return child;

        String bp = normalize(base.path);
        String cp = normalize(child.path);

        if (!bp.equals(cp)) {
            int last = bp.lastIndexOf('/');
            // if the base path has no slash, then it must be empty,
            // or relative. A path cannot be relative if the authority
            // is not null.
            assert last != -1 || bp.isEmpty() || base.authority == null;

            // if (last == -1) we need to replace the whole bp with "".
            // if (last > -1) we replace only the last element.
            bp = bp.substring(0,last+1);
            assert last == -1 && bp.isEmpty() || bp.charAt(last) == '/';

            if (!cp.startsWith(bp))
                return child;
        }

        String path = checkHierarchicalPath(null, null, null, null, -1,
                cp.substring(bp.length()), false);
        String query = child.query;
        String fragment = child.fragment;
        return new IRI(null,
                       null,
                       null,
                       null,
                       null,
                       -1,
                       path,
                       query,
                       fragment);
    }



    // -- Path normalization --

    // The following algorithm for path normalization avoids the creation of a
    // string object for each segment, as well as the use of a string buffer to
    // compute the final result, by using a single char array and editing it in
    // place.  The array is first split into segments, replacing each slash
    // with '\0' and creating a segment-index array, each element of which is
    // the index of the first char in the corresponding segment.  We then walk
    // through both arrays, removing ".", "..", and other segments as necessary
    // by setting their entries in the index array to -1.  Finally, the two
    // arrays are used to rejoin the segments and compute the final result.
    //
    // This code is based upon src/solaris/native/java/io/canonicalize_md.c


    // Check the given path to see if it might need normalization.  A path
    // might need normalization if it contains duplicate slashes, a "."
    // segment, or a ".." segment.  Return -1 if no further normalization is
    // possible, otherwise return the number of segments found.
    //
    // This method takes a string argument rather than a char array so that
    // this test can be performed without invoking path.toCharArray().
    //
    private static int needsNormalization(String path) {
        boolean normal = true;
        int ns = 0;                     // Number of segments
        int end = path.length() - 1;    // Index of last char in path
        int p = 0;                      // Index of next char in path

        // Skip initial slashes
        while (p <= end) {
            if (path.charAt(p) != '/') break;
            p++;
        }
        if (p > 1) normal = false;

        // Scan segments
        while (p <= end) {

            // Looking at "." or ".." ?
            if ((path.charAt(p) == '.')
                && ((p == end)
                    || ((path.charAt(p + 1) == '/')
                        || ((path.charAt(p + 1) == '.')
                            && ((p + 1 == end)
                                || (path.charAt(p + 2) == '/')))))) {
                normal = false;
            }
            ns++;

            // Find beginning of next segment
            while (p <= end) {
                if (path.charAt(p++) != '/')
                    continue;

                // Skip redundant slashes
                while (p <= end) {
                    if (path.charAt(p) != '/') break;
                    normal = false;
                    p++;
                }

                break;
            }
        }

        return normal ? -1 : ns;
    }


    // Split the given path into segments, replacing slashes with nulls and
    // filling in the given segment-index array.
    //
    // Preconditions:
    //   segs.length == Number of segments in path
    //
    // Postconditions:
    //   All slashes in path replaced by '\0'
    //   segs[i] == Index of first char in segment i (0 <= i < segs.length)
    //
    private static void split(char[] path, int[] segs) {
        int end = path.length - 1;      // Index of last char in path
        int p = 0;                      // Index of next char in path
        int i = 0;                      // Index of current segment

        // Skip initial slashes
        while (p <= end) {
            if (path[p] != '/') break;
            path[p] = '\0';
            p++;
        }

        while (p <= end) {

            // Note start of segment
            segs[i++] = p++;

            // Find beginning of next segment
            while (p <= end) {
                if (path[p++] != '/')
                    continue;
                path[p - 1] = '\0';

                // Skip redundant slashes
                while (p <= end) {
                    if (path[p] != '/') break;
                    path[p++] = '\0';
                }
                break;
            }
        }

        if (i != segs.length)
            throw new InternalError();  // ASSERT
    }


    // Join the segments in the given path according to the given segment-index
    // array, ignoring those segments whose index entries have been set to -1,
    // and inserting slashes as needed.  Return the length of the resulting
    // path.
    //
    // Preconditions:
    //   segs[i] == -1 implies segment i is to be ignored
    //   path computed by split, as above, with '\0' having replaced '/'
    //
    // Postconditions:
    //   path[0] .. path[return value] == Resulting path
    //
    private static int join(char[] path, int[] segs) {
        int ns = segs.length;           // Number of segments
        int end = path.length - 1;      // Index of last char in path
        int p = 0;                      // Index of next path char to write

        if (path[p] == '\0') {
            // Restore initial slash for absolute paths
            path[p++] = '/';
        }

        for (int i = 0; i < ns; i++) {
            int q = segs[i];            // Current segment
            if (q == -1)
                // Ignore this segment
                continue;

            if (p == q) {
                // We're already at this segment, so just skip to its end
                while ((p <= end) && (path[p] != '\0'))
                    p++;
                if (p <= end) {
                    // Preserve trailing slash
                    path[p++] = '/';
                }
            } else if (p < q) {
                // Copy q down to p
                while ((q <= end) && (path[q] != '\0'))
                    path[p++] = path[q++];
                if (q <= end) {
                    // Preserve trailing slash
                    path[p++] = '/';
                }
            } else
                throw new InternalError(); // ASSERT false
        }

        return p;
    }


    // Remove "." segments from the given path, and remove segment pairs
    // consisting of a non-".." segment followed by a ".." segment.
    //
    private static void removeDots(char[] path, int[] segs) {
        int ns = segs.length;
        int end = path.length - 1;

        for (int i = 0; i < ns; i++) {
            int dots = 0;               // Number of dots found (0, 1, or 2)

            // Find next occurrence of "." or ".."
            do {
                int p = segs[i];
                if (path[p] == '.') {
                    if (p == end) {
                        dots = 1;
                        break;
                    } else if (path[p + 1] == '\0') {
                        dots = 1;
                        break;
                    } else if ((path[p + 1] == '.')
                               && ((p + 1 == end)
                                   || (path[p + 2] == '\0'))) {
                        dots = 2;
                        break;
                    }
                }
                i++;
            } while (i < ns);
            if ((i > ns) || (dots == 0))
                break;

            if (dots == 1) {
                // Remove this occurrence of "."
                segs[i] = -1;
            } else {
                // If there is a preceding non-".." segment, remove both that
                // segment and this occurrence of ".."; otherwise, leave this
                // ".." segment as-is, unless if it's the first segment, and
                // the path is absolute. In this latter case remove it.
                // See RFC 3986, Section 5.2.4.  Remove Dot Segments
                // and Section 5.4.2.  Abnormal Examples
                int j;
                for (j = i - 1; j >= 0; j--) {
                    if (segs[j] != -1) break;
                }
                if (j >= 0) {
                    int q = segs[j];
                    if (!((path[q] == '.')
                          && (path[q + 1] == '.')
                          && (path[q + 2] == '\0'))) {
                        segs[i] = -1;
                        segs[j] = -1;
                    }
                } else if (j == -1 && path[0] == '\0') {
                    segs[i] = -1;
                }
            }
        }
    }


    // DEVIATION: If the normalized path is relative, and if the first
    // segment could be parsed as a scheme name, then prepend a "." segment
    //
    private static void maybeAddLeadingDot(char[] path, int[] segs) {

        if (path[0] == '\0')
            // The path is absolute
            return;

        int ns = segs.length;
        int f = 0;                      // Index of first segment
        while (f < ns) {
            if (segs[f] >= 0)
                break;
            f++;
        }
        if ((f >= ns) || (f == 0))
            // The path is empty, or else the original first segment survived,
            // in which case we already know that no leading "." is needed
            return;

        int p = segs[f];
        while ((p < path.length) && (path[p] != ':') && (path[p] != '\0')) p++;
        if (p >= path.length || path[p] == '\0')
            // No colon in first segment, so no "." needed
            return;

        // At this point we know that the first segment is unused,
        // hence we can insert a "." segment at that position
        path[0] = '.';
        path[1] = '\0';
        segs[0] = 0;
    }


    // Normalize the given path string.  A normal path string has no empty
    // segments (i.e., occurrences of "//"), no segments equal to ".", and no
    // segments equal to ".." that are preceded by a segment not equal to "..".
    // In contrast to Unix-style pathname normalization, for URI paths we
    // always retain trailing slashes.
    //
    private static String normalize(String ps) {

        // Does this path need normalization?
        int ns = needsNormalization(ps);        // Number of segments
        if (ns < 0)
            // Nope -- just return it
            return ps;

        char[] path = ps.toCharArray();         // Path in char-array form

        // Split path into segments
        int[] segs = new int[ns];               // Segment-index array
        split(path, segs);

        // Remove dots
        removeDots(path, segs);

        // Prevent scheme-name confusion
        maybeAddLeadingDot(path, segs);

        // Join the remaining segments and return the result
        String s = new String(path, 0, join(path, segs));
        if (s.equals(ps)) {
            // string was already normalized
            return ps;
        }
        return s;
    }



    // -- Character classes for parsing --

    // RFC2396 precisely specifies which characters in the US-ASCII charset are
    // permissible in the various components of a URI reference.  We here
    // define a set of mask pairs to aid in enforcing these restrictions.  Each
    // mask pair consists of two longs, a low mask and a high mask.  Taken
    // together they represent a 128-bit mask, where bit i is set iff the
    // character with value i is permitted.
    //
    // This approach is more efficient than sequentially searching arrays of
    // permitted characters.  It could be made still more efficient by
    // precompiling the mask information so that a character's presence in a
    // given mask could be determined by a single table lookup.

    // To save startup time, we manually calculate the low-/highMask constants.
    // For reference, the following methods were used to calculate the values:

    // Compute the low-order mask for the characters in the given string
    private static long lowMask(String chars) {
        int n = chars.length();
        long m = 0;
        for (int i = 0; i < n; i++) {
            char c = chars.charAt(i);
            if (c < 64)
                m |= (1L << c);
        }
        return m;
    }

    // Compute the high-order mask for the characters in the given string
    private static long highMask(String chars) {
        int n = chars.length();
        long m = 0;
        for (int i = 0; i < n; i++) {
            char c = chars.charAt(i);
            if ((c >= 64) && (c < 128))
                m |= (1L << (c - 64));
        }
        return m;
    }

    // Compute a low-order mask for the characters
    // between first and last, inclusive
    private static long lowMask(char first, char last) {
        long m = 0;
        int f = Math.max(Math.min(first, 63), 0);
        int l = Math.max(Math.min(last, 63), 0);
        for (int i = f; i <= l; i++)
            m |= 1L << i;
        return m;
    }

    // Compute a high-order mask for the characters
    // between first and last, inclusive
    private static long highMask(char first, char last) {
        long m = 0;
        int f = Math.max(Math.min(first, 127), 64) - 64;
        int l = Math.max(Math.min(last, 127), 64) - 64;
        for (int i = f; i <= l; i++)
            m |= 1L << i;
        return m;
    }

    // Tell whether the given character is permitted by the given mask pair
    private static boolean match(char c, long lowMask, long highMask) {
        if (c == 0) // 0 doesn't have a slot in the mask. So, it never matches.
            return false;
        if (c < 64)
            return ((1L << c) & lowMask) != 0;
        if (c < 128)
            return ((1L << (c - 64)) & highMask) != 0;
        return false;
    }

    // Character-class masks, in reverse order from RFC3986 because
    // initializers for static fields cannot make forward references.

    // digit    = "0" | "1" | "2" | "3" | "4" | "5" | "6" | "7" |
    //            "8" | "9"
    private static final long L_DIGIT = 0x3FF000000000000L; // lowMask('0', '9');
    private static final long H_DIGIT = 0L;

    // upalpha  = "A" | "B" | "C" | "D" | "E" | "F" | "G" | "H" | "I" |
    //            "J" | "K" | "L" | "M" | "N" | "O" | "P" | "Q" | "R" |
    //            "S" | "T" | "U" | "V" | "W" | "X" | "Y" | "Z"
    private static final long L_UPALPHA = 0L;
    private static final long H_UPALPHA = 0x7FFFFFEL; // highMask('A', 'Z');

    // lowalpha = "a" | "b" | "c" | "d" | "e" | "f" | "g" | "h" | "i" |
    //            "j" | "k" | "l" | "m" | "n" | "o" | "p" | "q" | "r" |
    //            "s" | "t" | "u" | "v" | "w" | "x" | "y" | "z"
    private static final long L_LOWALPHA = 0L;
    private static final long H_LOWALPHA = 0x7FFFFFE00000000L; // highMask('a', 'z');

    // alpha         = lowalpha | upalpha
    private static final long L_ALPHA = L_LOWALPHA | L_UPALPHA;
    private static final long H_ALPHA = H_LOWALPHA | H_UPALPHA;

    // alphanum      = alpha | digit
    private static final long L_ALPHANUM = L_DIGIT | L_ALPHA;
    private static final long H_ALPHANUM = H_DIGIT | H_ALPHA;

    // hex           = digit | "A" | "B" | "C" | "D" | "E" | "F" |
    //                         "a" | "b" | "c" | "d" | "e" | "f"
    private static final long L_HEX = L_DIGIT;
    private static final long H_HEX = 0x7E0000007EL; // highMask('A', 'F') | highMask('a', 'f');

    // sub-delims    =  "!" / "$" / "&" / "'" / "(" / ")"
    //  / "*" / "+" / "," / ";" / "="
    // TODO: inline value
    private static final long L_SUB_DELIMS = lowMask("!$&'()*+,;=");
    private static final long H_SUB_DELIMS = highMask("!$&'()*+,;=");

    // gen-delims    =  ":" / "/" / "?" / "#" / "[" / "]" / "@"
    // TODO: inline value
    private static final long L_GEN_DELIMS = lowMask(":/?#[]@");
    private static final long H_GEN_DELIMS = highMask(":/?#[]@");

    // mark          = "-" | "_" | "." | "!" | "~" | "*" | "'" |
    //                 "(" | ")"
    // TODO: inline value
    private static final long L_MARK = lowMask("-._~");
    private static final long H_MARK = highMask("-._~");

    // unreserved    = alphanum | mark
    private static final long L_UNRESERVED = L_ALPHANUM | L_MARK;
    private static final long H_UNRESERVED = H_ALPHANUM | H_MARK;

    // reserved      =  gen-delims / sub-delims
    private static final long L_RESERVED = L_SUB_DELIMS | L_GEN_DELIMS;
    private static final long H_RESERVED = H_SUB_DELIMS | H_GEN_DELIMS;

    // The zero'th bit is used to indicate that escape pairs and non-US-ASCII
    // characters are allowed; this is handled by the scanEscape method below.
    private static final long L_ESCAPED = 1L;
    private static final long H_ESCAPED = 0L;

    // uric          = reserved | unreserved | escaped
    // private static final long L_URIC = L_RESERVED | L_UNRESERVED | L_ESCAPED;
    // private static final long H_URIC = H_RESERVED | H_UNRESERVED | H_ESCAPED;

    // pchar         =  unreserved / pct-encoded / sub-delims / ":" / "@"
    // TODO: inline value
    private static final long L_PCHAR
        = L_UNRESERVED | L_ESCAPED | L_SUB_DELIMS | lowMask(":@");
    private static final long H_PCHAR
        = H_UNRESERVED | H_ESCAPED | H_SUB_DELIMS | highMask(":@");

    // fragment      =  *( pchar / "/" / "?" )
    // TODO: inline value
    private static final long L_FRAGMENT = L_PCHAR | lowMask("/?");
    private static final long H_FRAGMENT = H_PCHAR | highMask("/?");

    // query         =  *( pchar / "/" / "?" )
    // TODO: inline value
    private static final long L_QUERY = L_PCHAR | lowMask("/?");
    private static final long H_QUERY = H_PCHAR | highMask("/?");    // All valid path characters


    // All valid path characters
    // TODO: inline value
    private static final long L_PATH = L_PCHAR | lowMask("/");
    private static final long H_PATH = H_PCHAR | highMask("/");

    // Dash, for use in domainlabel and toplabel
    // private static final long L_DASH = 0x200000000000L; // lowMask("-");
    // private static final long H_DASH = 0x0L; // highMask("-");

    // Dot, for use in hostnames
    private static final long L_DOT = 0x400000000000L; // lowMask(".");
    private static final long H_DOT = 0x0L; // highMask(".");

    // Dash, for use in domainlabel and toplabel
    private static final long L_DASH = 0x200000000000L; // lowMask("-");
    private static final long H_DASH = 0x0L; // highMask("-");

    // Colon, for use in addresses
    // TODO: inline value
    private static final long L_COLON = lowMask(":");
    private static final long H_COLON = highMask(":");

    // userinfo      =  *( unreserved / pct-encoded / sub-delims / ":" )
    private static final long L_USERINFO
            = L_UNRESERVED | L_ESCAPED | L_SUB_DELIMS | L_COLON;
    private static final long H_USERINFO
            = H_UNRESERVED | H_ESCAPED | H_SUB_DELIMS | H_COLON;

    // IPvFuture      = "v" 1*HEXDIG "." 1*( unreserved / sub-delims / ":" )
    // This mask is only intended to cover the second part of the rule:
    //   1*( unreserved / sub-delims / ":" )
    // though it also covers the first part as "v" 1*HEXDIG "." are all
    // unreserved.
    private static final long L_IPVFUTURE
            = L_UNRESERVED | L_SUB_DELIMS | L_COLON;
    private static final long H_IPVFUTURE
            = H_UNRESERVED | H_SUB_DELIMS | H_COLON;

    // reg_name      =  *( unreserved / pct-encoded / sub-delims )
    private static final long L_REG_NAME
        = L_UNRESERVED | L_ESCAPED | L_SUB_DELIMS;
    private static final long H_REG_NAME
        = H_UNRESERVED | H_ESCAPED | H_SUB_DELIMS;

    // authority     = [ userinfo "@" ] host [ ":" port ]
    // TODO: inline value
    private static final long L_AUTHORITY
            = L_USERINFO | L_REG_NAME | L_DIGIT | lowMask("@:");
    private static final long H_AUTHORITY
            = H_USERINFO | H_REG_NAME | H_DIGIT | highMask("@:");

    // scheme        = alpha *( alpha | digit | "+" | "-" | "." )
    private static final long L_SCHEME = L_ALPHA | L_DIGIT | 0x680000000000L; // lowMask("+-.");
    private static final long H_SCHEME = H_ALPHA | H_DIGIT; // | highMask("+-.") == 0L

    // scope_id = alpha | digit | "_" | "."
    private static final long L_SCOPE_ID
        = L_ALPHANUM | 0x400000000000L; // lowMask("_.");
    private static final long H_SCOPE_ID
        = H_ALPHANUM | 0x80000000L; // highMask("_.");

    // Masks used for lenient parsing
    // TODO: inline value
    private static final long L_NOTALLOWED = lowMask("<>\" {}|\\^`");
    private static final long H_NOTALLOWED = highMask("<>\" {}|\\^`");
    private static final long L_LENIENT = ~L_NOTALLOWED;
    private static final long H_LENIENT = ~H_NOTALLOWED;


    // -- Escaping and encoding --

    private static final char[] hexDigits = {
        '0', '1', '2', '3', '4', '5', '6', '7',
        '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    };

    private static void appendEscape(StringBuilder sb, byte b) {
        sb.append('%');
        sb.append(hexDigits[(b >> 4) & 0x0f]);
        sb.append(hexDigits[(b >> 0) & 0x0f]);
    }

    private static void appendEscape(CharBuffer cb, byte b) {
        cb.append('%');
        cb.append(hexDigits[(b >> 4) & 0x0f]);
        cb.append(hexDigits[(b >> 0) & 0x0f]);
    }

    // deals with surrogate pair, returns the index of the last escaped
    // char (either pos or pos+1)
    private static int appendEncoded(StringBuilder sb, CharSequence s, int pos, char c) {
        ByteBuffer bb = null;
        try {
            if (Character.isHighSurrogate(c) && pos < s.length() - 1) {
                assert s.charAt(pos) == c;
                char ca[] = {c, s.charAt(++pos)};
                assert Character.isLowSurrogate(ca[1]);
                bb = ThreadLocalCoders.encoderFor(StandardCharsets.UTF_8)
                        .encode(CharBuffer.wrap(ca));
            } else {
                bb = ThreadLocalCoders.encoderFor(StandardCharsets.UTF_8)
                        .encode(CharBuffer.wrap("" + c));
            }
        } catch (CharacterCodingException x) {
            assert false;
        }
        while (bb.hasRemaining()) {
            int b = bb.get() & 0xff;
            appendEscape(sb, (byte)b);
        }
        return pos;
    }

    // RFC 3987 defines these two categories - which in this document are
    // collapsed into the single category 'other'.
    //
    //    ucschar        = %xA0-D7FF / %xF900-FDCF / %xFDF0-FFEF
    //                  / %x10000-1FFFD / %x20000-2FFFD / %x30000-3FFFD
    //                  / %x40000-4FFFD / %x50000-5FFFD / %x60000-6FFFD
    //                  / %x70000-7FFFD / %x80000-8FFFD / %x90000-9FFFD
    //                  / %xA0000-AFFFD / %xB0000-BFFFD / %xC0000-CFFFD
    //                  / %xD0000-DFFFD / %xE1000-EFFFD
    //
    //    iprivate       = %xE000-F8FF / %xF0000-FFFFD / %x100000-10FFFD
    //
    // These definitions leave several 'holes':
    //
    //  1. high-surrogate/low-surrogate used in UTF-16 encoding
    //     U+D800-U+DBFF and  U+DC00-U+DFFF
    //  2. non characters: U+FDD0-U+FDEF and chars ending with FFFE-FFFF
    //  3. special chars U+FFF0-U+FFFD - which include the replacement char
    //     U+FFFD.
    //
    //  1. should not occur - since Java default encoding is UTF-16, then
    //     surrogate pairs should produce a code point and surrogate chars
    //     that don't produce a code point should form an illegal sequence.
    //  2. These characters are not characters but they will not be
    //     considered as illegal sequences by the decoders.
    //     This class will need to handle them as bidi chars [TODO].
    //  3. Special character should be quoted - just as controls and spaces
    //
    // Additional restrictions:
    //
    //  4. In addition - RFC 3987 specifies that bidi-chars should not be rendered
    //     but should be quoted when mapping a URI to/from an IRI.
    //     We also don't decode bidi chars in decoded strings, but treat
    //     them as invalid sequences.
    //

    // Special characters in the range U+FFF0-U+FFFD
    private static boolean isSpecial(char c) {
        return c >=  0xFFF0 && c <= 0xFFFD;
    }

    // Non char in the Basic Multilingual Plane:
    // U+FDD0-U+FDEF and U+FFFE-U+FFFF
    private static boolean isNonCharBMP(char c) {
        return c >= 0xFDD0 && (c <= 0xFDEF || c == 0xFFFE || c == 0xFFFF);
    }

    private static boolean isNonChar(CharSequence s, int pos, char c) {
        assert s.charAt(pos) == c;
        if (Character.isHighSurrogate(c)) {
            int cp = Character.codePointAt(s, pos);
            int masked = cp & 0xFFFF;
            // any codepoint ending with FFFF or FFFE is a
            // non character
            return masked == 0xFFFF || masked == 0xFFFE;
        } else {
            return isNonCharBMP(c);
        }
    }

    private static boolean isPrivate(CharSequence s, int pos, char c) {
        if (c >= 0xE000 && c <= 0xF8FF) return true; // BMP
        if (Character.isHighSurrogate(c)) { // supplementary planes
            int cp = Character.codePointAt(s, pos);
            if (cp >= 0xF0000) {
                if (cp <= 0xFFFFD) return true;
                return cp >= 0x100000 && cp <= 0x10FFFD;
            }
        }
        return false;
    }

    //
    // test if the string contains bidi formatting character sequence
    // at the given position:
    //      bidi chars      value       represent in UTF-8 character set
    //      LRM             \u200E      %E2%80%8E
    //      RLM             \u200F      %E2%80%8F
    //      LRE             \u202A      %E2%80%AA
    //      RLE             \u202B      %E2%80%AB
    //      PDF             \u202C      %E2%80%AC
    //      LRO             \u202D      %E2%80%AD
    //      RLO             \u202E      %E2%80%AE
    //
    // IRIs MUST NOT contain bidirectional formatting characters
    // (LRM, RLM, LRE, RLE, LRO, RLO, and PDF).
    private static boolean isBidi(char c) {
        return c >=  0x200E && c <= 0x202E && (c <= 0x200F || c >= 0x202A);
    }

    private static boolean mustStayEncoded(CharSequence s, int pos, char c) {
        return isBidi(c) || isNonChar(s, pos, c);
    }

    /**
     * This enumeration is used to provide further indication on
     * how a given host component was parsed by an {@link IRI}
     * instance.
     *
     * @since TBD
     *
     * @see IRI#getHostType(String)
     */
    public static enum HostType {
        /**
         * Represents a host component that parses as an IPv4 literal
         * address (RFC 3986 Appendix A: IPv4address).
         */
        IPv4,

        /**
         * Represents a host component that parses as an IPv6 literal
         * address (RFC 3986 Appendix A: IP-literal, IPv6address).
         */
        IPv6,

        /**
         * Represents a host component that parses as an IPvFuture literal
         * address (RFC 3986 Appendix A: IP-literal, IPvFuture).
         */
        IPvFuture,

        /**
         * Represents a host component that parses as a <i>reg-name</i>
         * and additionally conforms to the DNS syntax.
         * (RFC 3986 Appendix A: reg-name, further conforming to
         *  RFC 2396 Appendix A: hostname)
         */
        DNSRegName,

        /**
         * Represents a host component that parses as a <i>reg-name</i>
         * but does not necessarily conform to the DNS syntax.
         * (Any name allowed by RFC 3987 Section 2.2: ireg-name
         *  which doesn't necessarily conform to RFC 2396 Appendix A: hostname)
         */
        RegName,

        /**
         * Represent a host component which is absent.
         */
        None;

        /**
         * Tells whether a host component was parsed as a
         * literal address type.
         *
         * @apiNote
         *
         * This method returns true for {@link #IPv4}, {@link #IPv6}
         * and {@link #IPvFuture}.
         *
         * @return True if this value represents a host
         *         component that was parsed as a literal
         *         address type, false otherwise.
         */
        public boolean isLiteral() {
            switch (this) {
                case IPv4: return true;
                case IPv6: return true;
                case IPvFuture: return true;
                default: return false;
            }
        }

        /**
         * Tells whether a host component corresponds to an Internet
         * literal address or host name.
         *
         * This method yields true for those syntax types which are
         * known to be usable to connect to a host on the Internet
         * without further syntax processing, such as IPv4 and IPv6
         * literals, as well as for host names that conform to the
         * DNS syntax.
         *
         * @apiNote
         *
         * This method returns true for {@link #IPv4}, {@link #IPv6}
         * and {@link #DNSRegName}, false otherwise.
         *
         * @return True if this value represents an address
         *         form that can be used to connect to
         *         a host on the Internet without further
         *         syntax processing.
         */
        public boolean isInternetName() {
            switch (this) {
                case IPv4: return true;
                case IPv6: return true;
                case DNSRegName: return true;
                default: return false;
            }
        }
    }


    /**
     * Indicates how a given {@code hostString} component parses according to
     * RFC 3987 grammar.
     * This method can be called to provide further information about the
     * syntactic form of the {@linkplain #getRawHostString() raw host string}
     * or {@linkplain #getHostString() decoded host string} of an IRI.
     *
     * <ol>
     *   <li>{@link HostType#None None}: if the given {@code hostString} is null.</li>
     *   <li>{@link HostType#IPv4 IPv4}: the given {@code hostString} can be parsed
     *       as an IPv4 literal address.</li>
     *   <li>{@link HostType#IPv6 IPv6}: the given {@code hostString} can be parsed
     *       as an IPv6 literal address.</li>
     *   <li>{@link HostType#IPvFuture IPvFuture}: the given {@code hostString} can
     *       be parsed as an IPvFuture literal address.</li>
     *   <li>{@link HostType#DNSRegName DNSRegName}: the given {@code hostString}
     *       can be parsed as a <i>reg-name</i>, and additionally conforms to the DNS
     *       syntax as specified by RFC 2386 <i>hostname</i>.</li>
     *   <li>{@link HostType#RegName RegName}: the given {@code hostString}
     *       is parsed as an <i>ireg-name</i> (but may be empty)</li>
     * </ol>
     *
     * @apiNote
     *
     *  Usually, for any IRI <i>u</i>, calling {@code IRI.getHostType(u.getHostString())}
     *  or {@code IRI.getHostType(u.getRawHostString())} should yield the same result,
     *  except when the {@linkplain #getRawHostString() raw host string} contains
     *  unreserved US-ASCII characters in percent-encoded form. For example,
     *  the raw host string {@code "%41%42%43.example.com"} will parse as
     *  a {@linkplain HostType#RegName reg-name}, whereas its decoded form
     *  {@code "ABC.example.com"} will parse as a {@linkplain HostType#DNSRegName
     *  DNS reg-name}.
     *  When providing an IRI, whether to use the raw form or the decoded form of
     *  the host component is usually left up to the code that accepts the IRI.
     *  This method can help the caller decide whether the IRI it hands off, or that
     *  it accepts, is appropriate to use by the underlying APIs that it wants to
     *  call.
     *
     * @param hostString The host component string, as returned by one of
     *                   {@link #getHost()}, {@link #getHostString()} or
     *                   {@link #getRawHostString()}.
     *
     * @return A {@link HostType} value that indicates how the
     *
     */
    public static HostType getHostType(String hostString) {
        // TODO: cache value during parsing?
        return hostString == null ? HostType.None : getHostType(hostString, true);
    }

    //
    // quote the given host string if it is a reg-name,
    // i.e. neither IPv4Address nor IPv6Address nor IPvFuture
    //
    private static String quoteHost(String host) {
        HostType addressType = getHostType(host, false);
        if (addressType.isLiteral()) {
            // IPv4 doesn't need to be quoted
            // % in IPv6 should not be quoted (it's the scope delimiter)
            // IPvFuture doesn't allow percent encoded chars
            return host;
        }
        // otherwise it's a reg-name - and anything within
        // must be quoted.
        return quote(host, L_REG_NAME, H_REG_NAME);
    }

    // Returns true if the host is a literal IP address.
    // This can be an IPv4 literal address, an IPv6 literal
    // address, enclosed or not in square brackets, or
    // an IPvFuture literal address (in which case square
    // brackets around it are required)
    private static boolean isIPLiteralAddress(String host) {
        return getHostType(host, false).isLiteral();
    }

    // Figure out how a host component was parsed.
    // If parseDNS is true, further extends the analysis to figure
    // out if a RegName is a DNSRegName.
    private static HostType getHostType(String host, boolean parseDNS) {
        if (IPAddressUtil.isIPv4LiteralAddress(host)
                && isStrictIPv4Address(host)) {
            return HostType.IPv4;
        } else {
            boolean betweenBrackets = (host.startsWith("[") && host.endsWith("]"));
            String literalIPv6 = null;

            // a literal IPv6 address may or may not be surrounded by brackets
            // normalize it before being feeded to isIPv6LiteralAddress()
            if (betweenBrackets)
                literalIPv6 = host.substring(1, host.length() - 1);
            else
                literalIPv6 = host;

            if (IPAddressUtil.isIPv6LiteralAddress(literalIPv6))
                return HostType.IPv6;
            // require brackets for IPvFuture
            if (betweenBrackets && isIPvFuture(literalIPv6))
                return HostType.IPvFuture;
        }
        return (parseDNS && isDNSName(host)) ? HostType.DNSRegName : HostType.RegName;
    }

    // Returns true if the literalIP string is an IPvFuture
    // literal address (square brackets must have been
    // removed before calling this method).
    private static boolean isIPvFuture(String literalIP)
    {
        int p = 0;
        int q;
        int n = literalIP.length();

        if (p >= n || literalIP.charAt(p++) != 'v') {     // check and skip 'v'
            return false;
        }

        for (q = p; q < n && match(literalIP.charAt(q), L_HEX, H_HEX); q++);
        if (q <= p || q >= n) return false;
        p = q;
        if (p >= n || literalIP.charAt(p++) != '.') {     // check and skip '.'
            return false;
        }

        for (q = p; q < n && match(literalIP.charAt(q),
                L_IPVFUTURE, H_IPVFUTURE); q++);
        if (q <= p || q != n) {
            return false;
        }

        return true;
    }

    // scans the next char in the char sequence.
    private static int scanAscii(CharSequence input, int start, int end, char c) {
        if ((start < end) && (input.charAt(start) == c))
            return start + 1;
        return start;
    }

    // scans a char sequence until a character that doesn't match the provided
    // mask is found. Doesn't handle escape sequences.
    private static int scanAscii(CharSequence input, int start, int n, long lowMask, long highMask)
    {
        int p = start;
        while (p < n) {
            char c = input.charAt(p);
            if (match(c, lowMask, highMask)) {
                p++;
                continue;
            }
            break;
        }
        return p;
    }

    // Scan a string of decimal digits whose value fits in a byte
    //
    private static int scanByte(CharSequence input, int start, int n) {
        int p = start;
        int q = scanAscii(input,p, n, L_DIGIT, H_DIGIT);
        if (q <= p) return q;
        if (Integer.parseInt(input, p, q, 10) > 255) return p;
        return q;
    }

    // Scan an IPv4 address.
    //
    // If the strict argument is true then we require that the given
    // interval contain nothing besides an IPv4 address; if it is false
    // then we only require that it start with an IPv4 address.
    //
    // If the interval does not contain or start with (depending upon the
    // strict argument) a legal IPv4 address characters then we return -1
    // immediately; otherwise we insist that these characters parse as a
    // legal IPv4 address and throw an exception on failure.
    //
    // We assume that any string of decimal digits and dots must be an IPv4
    // address.  It won't parse as a hostname anyway, so making that
    // assumption here allows more meaningful exceptions to be thrown.
    //
    private static int scanIPv4Address(CharSequence input, int start, int n, boolean strict) {
        int p = start;
        int q;
        int m = scanAscii(input, p, n, L_DIGIT | L_DOT, H_DIGIT | H_DOT);
        if ((m <= p) || (strict && (m != n)))
            return -1;
        for (;;) {
            // Per RFC2732: At most three digits per byte
            // Further constraint: Each element fits in a byte
            if ((q = scanByte(input, p, m)) <= p) break;   p = q;
            if ((q = scanAscii(input, p, m, '.')) <= p) break;  p = q;
            if ((q = scanByte(input, p, m)) <= p) break;   p = q;
            if ((q = scanAscii(input, p, m, '.')) <= p) break;  p = q;
            if ((q = scanByte(input, p, m)) <= p) break;   p = q;
            if ((q = scanAscii(input, p, m, '.')) <= p) break;  p = q;
            if ((q = scanByte(input, p, m)) <= p) break;   p = q;
            if (q < m) break;
            return q;
        }
        return -q -2;
    }

    private static boolean isStrictIPv4Address(String host) {
        int len = host.length();
        int res = scanIPv4Address(host, 0, len, true);
        return res == len;
    }

    // Returns true if the host name conforms to the DNS syntax
    private static boolean isDNSName(String host) {
        int p = 0, n = host.length();
        int q;
        int l = -1;                 // Start of last parsed label

        do {
            // domainlabel = alphanum [ *( alphanum | "-" ) alphanum ]
            q = scanAscii(host, p, n, L_ALPHANUM, H_ALPHANUM);
            if (q <= p)
                break;
            l = p;
            if (q > p) {
                p = q;
                q = scanAscii(host, p, n, L_ALPHANUM | L_DASH, H_ALPHANUM | H_DASH);
                if (q > p) {
                    if (host.charAt(q - 1) == '-')
                        return false;
                    p = q;
                }
            }
            q = scanAscii(host, p, n, '.');
            if (q <= p)
                break;
            p = q;
        } while (p < n);

        if (p < n) return false;

        if (l < 0) return false;

        // for a fully qualified hostname check that the rightmost
        // label starts with an alpha character.
        if (l > 0 && !match(host.charAt(l), L_ALPHA, H_ALPHA)) {
            return false;
        }

        return true;
    }

    // This interface is used to figure out which non-ASCII chars
    // (greater than 0x80 (128)) are illegal and must be quoted.
    // In query, iprivate chars are legal, but not elsewhere.
    //
    private interface NonASCII {

        // these chars are illegal in query and must be quoted
        private static boolean basicQuoting(CharSequence s, int pos, char c) {
            return Character.isSpaceChar(c)
                    || Character.isISOControl(c)
                    || isSpecial(c)
                    || mustStayEncoded(s, pos, c);
        }

        // these chars are illegal everywhere else (except query)
        // and must be quoted.
        private static boolean defaultQuoting(CharSequence s, int pos, char c) {
            return basicQuoting(s, pos, c) || isPrivate(s, pos, c);
        }

        default boolean needQuoting(CharSequence s, int pos, char c) {
            return defaultQuoting(s, pos, c);
        }

        default int escapeLength(CharSequence s, int pos, char c) {
            return Character.isHighSurrogate(c)
                    && ++pos < s.length()
                    && Character.isLowSurrogate(s.charAt(pos)) ? 2 : 1;
        }

        // default quoting: control, space, private chars, etc...
        static final NonASCII DEFAULT = new NonASCII() {};

        // query quoting: same as default but allows private chars
        static final NonASCII QUERY = new NonASCII() {
            @Override
            public boolean needQuoting(CharSequence s, int pos, char c) {
                return NonASCII.basicQuoting(s, pos, c);
            }
        };

        // no quoting: all non-ascii chars are allowed in unquoted form.
        // this is used in case where the string is supposed to be already
        // quoted: unquoted char may later cause an exception to be thrown.
        static final NonASCII NOQUOTES = new NonASCII() {
            @Override
            public boolean needQuoting(CharSequence s, int pos, char c) {
                return false;
            }
        };

        static NonASCII quotingFor(String what) {
            return "query".equals(what) ? QUERY : DEFAULT;
        }
    }

    // Quote any characters in s that are not permitted
    // by the given mask pair
    //
    private static String quote(String s, long lowMask, long highMask) {
        return quote(s, lowMask, highMask, NonASCII.DEFAULT);
    }

    private static String quote(String s, long lowMask, long highMask, NonASCII quoting) {
        if (s == null)
            return null;

        StringBuilder sb = null;
        boolean allowNonASCII = ((lowMask & L_ESCAPED) != 0);
        int len = s.length();
        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);
            if (c < '\u0080') {
                if (!match(c, lowMask, highMask) && !isEscaped(s, i)) {
                    if (sb == null) {
                        sb = new StringBuilder();
                        sb.append(s, 0, i);
                    }
                    appendEscape(sb, (byte)c);
                } else {
                    if (sb != null)
                        sb.append(c);
                }
            } else if (allowNonASCII
                       && quoting.needQuoting(s, i,c)) {
                if (sb == null) {
                    sb = new StringBuilder();
                    sb.append(s, 0, i);
                }
                i = appendEncoded(sb, s, i, c);
            } else {
                if (sb != null)
                    sb.append(c);
            }
        }
        return (sb == null) ? s : sb.toString();
    }

    //
    // To check if the given string has an escaped triplet
    // at the given position
    //
    private static boolean isEscaped(CharSequence s, int pos) {
        if (s == null || ((s.length() -2) <= pos))
            return false;

        return s.charAt(pos) == '%'
                && match(s.charAt(pos + 1), L_HEX, H_HEX)
                && match(s.charAt(pos + 2), L_HEX, H_HEX);
    }

    // Encodes all characters >= \u0080 into escaped, normalized UTF-8 octets,
    // assuming that s is otherwise legal
    //
    private static String encode(String s) {
        int n = s.length();
        if (n == 0)
            return s;

        // First check whether we actually need to encode
        for (int i = 0;;) {
            if (s.charAt(i) >= '\u0080')
                break;
            if (++i >= n)
                return s;
        }

        String ns = Normalizer.normalize(s, Normalizer.Form.NFC);
        ByteBuffer bb = null;
        try {
            bb = ThreadLocalCoders.encoderFor(StandardCharsets.UTF_8)
                .encode(CharBuffer.wrap(ns));
        } catch (CharacterCodingException x) {
            assert false;
        }

        StringBuilder sb = new StringBuilder();
        while (bb.hasRemaining()) {
            int b = bb.get() & 0xff;
            if (b >= 0x80)
                appendEscape(sb, (byte)b);
            else
                sb.append((char)b);
        }
        return sb.toString();
    }

    private static int decode(char c) {
        if ((c >= '0') && (c <= '9'))
            return c - '0';
        if ((c >= 'a') && (c <= 'f'))
            return c - 'a' + 10;
        if ((c >= 'A') && (c <= 'F'))
            return c - 'A' + 10;
        assert false;
        return -1;
    }

    private static byte decode(char c1, char c2) {
        return (byte)(  ((decode(c1) & 0xf) << 4)
                      | ((decode(c2) & 0xf) << 0));
    }

    // Evaluates all escapes in s, applying UTF-8 decoding if needed.  Assumes
    // that escapes are well-formed syntactically, i.e., of the form %XX.  If a
    // sequence of escaped octets is not valid UTF-8 then the erroneous octets
    // are copied to the result string.
    // Exception: any "%" found between "[]" is left alone. It is an IPv6 literal
    //            with a scope_id
    //
    // private static String decode(String s) {
    //    return decode(s, DecodeInfo.DEFAULT);
    // }

    /**
     * The DecodeInfo interface is used to tweak the decoding algorithm
     * according to different component needs. For instance, %2F should
     * be preserved when decoding path, but not when decoding fragments.
     * Similarly %40 and %2A should be preserved when decoding authority,
     * but not when decoding host, etc...
     */
    private interface DecodeInfo {
        default boolean ignorePercentInBrackets() { return false; }
        default boolean useReplacementChar() { return false; }
        default boolean canContainPercent() { return false; }
        default boolean isComposed() { return false;}
        default boolean preservePercentEncoding(CharSequence s, int pos, char c) {
            return mustStayEncoded(s, pos, c);
        }
        static final DecodeInfo DEFAULT = new DecodeInfo() {};

        static final DecodeInfo USER = DEFAULT;

        // We should preserve % in brackets when decoding
        // host. The only place where brackets are found
        // are for IPv6 literal and IPvFuture.
        // IPvFuture can't contain %encoded octets,
        // and encoded reg-name can't contain decoded brackets
        // so return true will work here.
        static final DecodeInfo HOST = new DecodeInfo() {
            @Override
            public boolean ignorePercentInBrackets() { return true; }
        };

        // it is not safe to decode %2F in path
        static final DecodeInfo PATH = new DecodeInfo() {
            @Override
            public boolean preservePercentEncoding(CharSequence s, int pos, char c) {
                return c == '/' || mustStayEncoded(s, pos, c);
            }
        };

        // no special handling for %2F in query.
        // A URI string should be quoteEncodedOctets() before being
        // embedded in a query string.
        static final DecodeInfo QUERY = DEFAULT;

        // it is safe to decode %2F in fragment,
        static final DecodeInfo FRAG = DEFAULT;

        // Decodes a scheme specific part string.
        // We must not decode /@[]:? if found encoded in
        // encoded SSP, as the result would no longer
        // be parsable.
        // The only place where we can find non encoded
        // brackets is in IP literal - where % is a
        // scope - so don't try to decode %encoded octets
        // when found in brackets.
        static final DecodeInfo SSP = new DecodeInfo() {
            @Override
            public boolean ignorePercentInBrackets() { return true; }
            @Override
            public boolean isComposed() { return true; }
            @Override
            public boolean preservePercentEncoding(CharSequence s, int pos, char c) {
                return c == '/' || c == '@' || c == ':'
                        || c == '[' || c == ']' || c == '?'
                        || mustStayEncoded(s, pos, c);
            }
        };

        // Decodes an authority string.
        // We must not decode /@[]: if found encoded in
        // encoded authority, as the result would no longer
        // be parsable.
        // The only place where we can find non encoded
        // brackets is in IP literal - where % is a
        // scope - so don't try to decode %encoded octets
        // when found in brackets.
        static final DecodeInfo AUTH = new DecodeInfo() {
            @Override
            public boolean isComposed() { return true; }
            @Override
            public boolean ignorePercentInBrackets() { return true; }
            @Override
            public boolean preservePercentEncoding(CharSequence s, int pos, char c) {
                return c == '@' || c == ':' || c == '['
                        || c == ']' || mustStayEncoded(s, pos, c);
            }
        };

        // Leniently decodes US-ASCII printable chars
        static final DecodeInfo LENIENT = new DecodeInfo() {
            @Override
            public boolean canContainPercent() { return true; }
            @Override
            public boolean preservePercentEncoding(CharSequence s, int pos, char c) {
                return c >= 0x80 || match(c, L_LENIENT, H_LENIENT);
            }
        };

        // Decodes all percent encoded sequences, except invalid
        // sequences.
        static final DecodeInfo ALL_VALID = new DecodeInfo() {
            @Override
            public boolean canContainPercent() { return true; }
            @Override
            public boolean preservePercentEncoding(CharSequence s, int pos, char c) {
                return isNonChar(s, pos, c);
            }
        };

        // Decodes all percent encoded sequences, replace invalid
        // sequences with U+FFFD.
        static final DecodeInfo REPLACE_INVALID = new DecodeInfo() {
            @Override
            public boolean useReplacementChar() { return true; }
            @Override
            public boolean canContainPercent() { return true; }
            @Override
            public boolean preservePercentEncoding(CharSequence s, int pos, char c) {
                return isNonChar(s, pos, c);
            }
        };

    }

    // This method was introduced as a generalization of URI.decode method
    // to provide a fix for JDK-8037396
    private static String decode(String s, DecodeInfo info) {
        return decode(s, info, false);
    }

    private static String decodeIRI(String s, DecodeInfo info) {
        return decode(s, info, true);
    }

    private static String decode(String s, DecodeInfo info, boolean toIRIString) {
        if (s == null)
            return s;
        int n = s.length();
        if (n == 0)
            return s;
        if (s.indexOf('%') < 0)
            return s;

        StringBuilder sb = new StringBuilder(n);
        ByteBuffer bb = ByteBuffer.allocate(n);
        CharBuffer cb = CharBuffer.allocate(n);
        CharsetDecoder dec = ThreadLocalCoders.decoderFor(StandardCharsets.UTF_8)
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);

        // This is not horribly efficient, but it will do for now
        char c = s.charAt(0);
        boolean betweenBrackets = false;
        boolean ignorePercentInBracket = info.ignorePercentInBrackets();
        boolean canContainPercent = info.canContainPercent();

        for (int i = 0; i < n;) {
            assert c == s.charAt(i);    // Loop invariant
            if (c == '[') {
                betweenBrackets = true;
            } else if (betweenBrackets && c == ']') {
                betweenBrackets = false;
            }

            if (c != '%' || (betweenBrackets && ignorePercentInBracket)
                || (canContainPercent && !isEscaped(s, i))) {
                sb.append(c);
                ++i;
                if (i >= n) continue;
                c = s.charAt(i);
                continue;
            }
            bb.clear();
            int ui = i;
            for (;;) {
                assert (n - i >= 2);
                bb.put(decode(s.charAt(i+1), s.charAt(i+2)));
                if ((i += 3) >= n)
                    break;
                c = s.charAt(i);
                if (c != '%' || canContainPercent && !isEscaped(s, i))
                    break;
            }
            bb.flip();
            cb.clear();
            dec.reset();
            CoderResult cr;
            do {
                cr = dec.decode(bb, cb, true);
                if (cr.isMalformed() || cr.isUnmappable()) {
                    // eat one byte at the current position
                    // and try to decode again
                    if (cb.position() > 0) {
                        // pass n to prevent the method from looking up
                        // the next character in the string.
                        appendDecoded(sb, s, n, cb, info, toIRIString);
                        assert !cb.hasRemaining();
                        cb.clear();
                    }
                    if (info.useReplacementChar()) {
                        for (int j = cr.length() ; j>0 ; j--) bb.get();
                        sb.append("\ufffd");
                    } else {
                        for (int j = cr.length() ; j>0 ; j--) {
                            appendEscape(sb, bb.get());
                        }
                    }
                }
            } while (!cr.isUnderflow());
            cr = dec.flush(cb);
            assert cr.isUnderflow();
            appendDecoded(sb, s, i, cb, info, toIRIString);
        }

        return sb.toString();
    }

    // Append the characters decoded in 'cb' to the string builder 'sb',
    // re-encoding them if needed with respect to DecodeInfo.
    private static void appendDecoded(StringBuilder sb, String s, int i,
                                      CharBuffer cb, DecodeInfo info, boolean toIRIString) {
        cb.flip();
        boolean composed = info.isComposed();
        while(cb.hasRemaining()) {
            // check whether character that were percent
            // encoded needs to stay percent encoded.
            // We must be careful here because our CharSequence
            // is a CharBuffer. Therefore charAt() is relative to
            // the current position in the buffer.
            char ch = cb.charAt(0);
            int r;
            char c1, c2, c3;
            if (info.preservePercentEncoding(cb, 0, ch)
                    || (toIRIString || composed) && ch == '%' && (r=cb.remaining()) >= 1
                    && s.length() > (r > 2 ? i-1 : r > 1 ? i : (i+1))
                    && match(c1 = r > 1 ? cb.charAt(1) : s.charAt(i), L_HEX, H_HEX)
                    && match(c2 = r > 2 ? cb.charAt(2) : s.charAt(r > 1 ? i : (i+1)), L_HEX, H_HEX)
                //&& ((c3 = (char)(decode(c1,c2) & 0xFF)) < 0x80)
                //&& !info.preservePercentEncoding(String.valueOf(c3), 0, c3)
            ) {
                if (ch < 0x80) {
                    appendEscape(sb, (byte)cb.get()); // advance
                } else {
                    int consumed = appendEncoded(sb, cb, 0, ch) + 1;
                    cb.position(cb.position() + consumed); // advance 1 or 2
                }
            } else {
                sb.append(cb.get());
            }
        }

    }


    // -- Parsing --

    // For convenience we wrap the input URI string in a new instance of the
    // following internal class.  This saves always having to pass the input
    // string as an argument to each internal scan/parse method.

    private static class Parser {

        private String input;           // URI input string
        private String scheme;
        private String authority;
        private String userInfo;
        private String host;
        private int port = -1;
        private String path;
        private String query;
        private String fragment;

        private Parser(String s) {
            input = s;
        }

        // -- Methods for throwing URISyntaxException in various ways --

        private void fail(String reason) throws URISyntaxException {
            throw new URISyntaxException(input, reason);
        }

        private void fail(String reason, int p) throws URISyntaxException {
            throw new URISyntaxException(input, reason, p);
        }

        private void failExpecting(String expected, int p)
            throws URISyntaxException
        {
            fail("Expected " + expected, p);
        }


        // -- Simple access to the input string --

        // Tells whether start < end and, if so, whether charAt(start) == c
        //
        private boolean at(int start, int end, char c) {
            return (start < end) && (input.charAt(start) == c);
        }

        // Tells whether start + s.length() < end and, if so,
        // whether the chars at the start position match s exactly
        //
        private boolean at(int start, int end, String s) {
            int p = start;
            int sn = s.length();
            if (sn > end - p)
                return false;
            int i = 0;
            while (i < sn) {
                if (input.charAt(p++) != s.charAt(i)) {
                    break;
                }
                i++;
            }
            return (i == sn);
        }


        // -- Scanning --

        // The various scan and parse methods that follow use a uniform
        // convention of taking the current start position and end index as
        // their first two arguments.  The start is inclusive while the end is
        // exclusive, just as in the String class, i.e., a start/end pair
        // denotes the left-open interval [start, end) of the input string.
        //
        // These methods never proceed past the end position.  They may return
        // -1 to indicate outright failure, but more often they simply return
        // the position of the first char after the last char scanned.  Thus
        // a typical idiom is
        //
        //     int p = start;
        //     int q = scan(p, end, ...);
        //     if (q > p)
        //         // We scanned something
        //         ...;
        //     else if (q == p)
        //         // We scanned nothing
        //         ...;
        //     else if (q == -1)
        //         // Something went wrong
        //         ...;


        // Scan a specific char: If the char at the given start position is
        // equal to c, return the index of the next char; otherwise, return the
        // start position.
        //
        private int scan(int start, int end, char c) {
            if ((start < end) && (input.charAt(start) == c))
                return start + 1;
            return start;
        }

        // Scan forward from the given start position.  Stop at the first char
        // in the err string (in which case -1 is returned), or the first char
        // in the stop string (in which case the index of the preceding char is
        // returned), or the end of the input string (in which case the length
        // of the input string is returned).  May return the start position if
        // nothing matches.
        //
        private int scan(int start, int end, String err, String stop) {
            int p = start;
            while (p < end) {
                char c = input.charAt(p);
                if (err.indexOf(c) >= 0)
                    return -1;
                if (stop.indexOf(c) >= 0)
                    break;
                p++;
            }
            return p;
        }

        // Scan forward from the given start position.  Stop at the first char
        // in the stop string (in which case the index of the preceding char is
        // returned), or the end of the input string (in which case the length
        // of the input string is returned).  May return the start position if
        // nothing matches.
        //
        private int scan(int start, int end, String stop) {
            int p = start;
            while (p < end) {
                char c = input.charAt(p);
                if (stop.indexOf(c) >= 0)
                    break;
                p++;
            }
            return p;
        }

        // Scan a potential escape sequence, starting at the given position,
        // with the given first char (i.e., charAt(start) == c).
        //
        // This method assumes that if escapes are allowed then visible
        // non-US-ASCII chars are also allowed.
        //
        private int scanEscape(int start, int n, char first, NonASCII quoting)
                throws URISyntaxException
        {
            int p = start;
            char c = first;
            if (c == '%') {
                // Process escape pair
                if ((p <= n - 3)
                    && match(input.charAt(p + 1), L_HEX, H_HEX)
                    && match(input.charAt(p + 2), L_HEX, H_HEX)) {
                    return p + 3;
                }
                fail("Malformed escape pair", p);
            } else if ((c > 128)
                       && !quoting.needQuoting(input, p, c)) {
                // Take into account surrogate pairs
                return p + quoting.escapeLength(input, p, c);
            }
            return p;
        }

        // Scan chars that match the given mask pair
        //
        private int scan(int start, int n, long lowMask, long highMask)
                throws URISyntaxException
        {
            return scan(start, n, lowMask, highMask, NonASCII.DEFAULT);
        }

        private int scan(int start, int n, long lowMask, long highMask, NonASCII quoting)
            throws URISyntaxException
        {
            int p = start;
            while (p < n) {
                char c = input.charAt(p);
                if (match(c, lowMask, highMask)) {
                    p++;
                    continue;
                }
                if ((lowMask & L_ESCAPED) != 0) {
                    int q = scanEscape(p, n, c, quoting);
                    if (q > p) {
                        p = q;
                        continue;
                    }
                }
                break;
            }
            return p;
        }

        // Check that each of the chars in [start, end) matches the given mask
        //
        private void checkChars(int start, int end,
                                long lowMask, long highMask,
                                String what)
            throws URISyntaxException
        {
            // NonASCII.quotingFor(what) relies on the fact that what = "query"
            // when we parse query strings...
            int p = scan(start, end, lowMask, highMask, NonASCII.quotingFor(what));
            if (p < end)
                fail("Illegal character in " + what, p);
        }

        // Check that the char at position p matches the given mask
        //
        private void checkChar(int p,
                               long lowMask, long highMask,
                               String what)
            throws URISyntaxException
        {
            checkChars(p, p + 1, lowMask, highMask, what);
        }


        // -- Parsing --

        // URI-reference = URI / relative-ref
        // URI           = scheme ":" hier-part [ "?" query ] [ "#" fragment ]
        // relative-ref  = relative-part [ "?" query ] [ "#" fragment ]
        //
        private IRI parse(boolean rsa) throws URISyntaxException {
            int n = input.length();
            int p = scan(0, n, "/?#", ":");
            int ssp;
            if ((p >= 0) && at(p, n, ':')) {
                if (p == 0)
                    failExpecting("scheme name", 0);
                checkChar(0, L_ALPHA, H_ALPHA, "scheme name");
                checkChars(1, p, L_SCHEME, H_SCHEME, "scheme name");
                scheme = input.substring(0, p);
                p++;                    // Skip ':'
                ssp = p;
                p = parseHierpart(p, n);
            } else {
                ssp = 0;
                p = parseRelativepart(0, n);
            }
            if (at(p, n, '?')) {
                int q = scan(p, n, "", "#");
                checkChars(p + 1, q, L_QUERY, H_QUERY, "query");
                query = input.substring(p + 1, q);
                p = q;
            }
            if (at(p, n, '#')) {
                checkChars(p + 1, n,L_FRAGMENT, H_FRAGMENT, "fragment");
                fragment = input.substring(p + 1, n);
                p = n;
            }
            if (p < n)
                fail("end of URI", p);

            assert host != null || authority == null;
            return new IRI(input, scheme, authority,
                           userInfo, host, port, path, query, fragment);
        }

        // hier-part     = "//" authority path-abempty
        //               / path-absolute
        //               / path-rootless, i.e. opaque
        //               / path-empty
        private int parseHierpart(int start, int n)
                throws URISyntaxException
        {
            int p = start;
            if (at(p, n, '/') && at(p + 1, n, '/')) {
                p += 2;
                int q = scan(p, n, "/?#");
                if (q >= p) {
                    p = parseAuthority(p, q);
                } else if (q <= n) {
                    // empty authority
                } else
                    failExpecting("authority", p);
            }

            return parsePath(p, n);
        }

        // relative-part = "//" authority path-abempty
        //               / path-absolute
        //               / path-noscheme
        //               / path-empty
        private int parseRelativepart(int start, int n)
            throws URISyntaxException {
            int p = start;
            if (at(p, n, '/') && at(p + 1, n, '/')) {
                p += 2;
                int q = scan(p, n, "", "/?#");
                if (q >= p) {
                    p = parseAuthority(p, q);
                } else if (q < n) {
                    // empty authority
                } else
                    failExpecting("authority", p);
            }
            return parsePath(p, n);
        }


        private int parsePath(int start, int n)
            throws URISyntaxException
        {
            int p = start;
            int q = scan(p, n, "", "?#");
            checkChars(p, q, L_PATH, H_PATH, "path");
            path = input.substring(p, q);        // May be ""
            return q;
        }

        // [<userinfo>@]<host>[:<port>]
        //
        private int parseAuthority(int start, int n)
            throws URISyntaxException
        {
            int p = start;
            int q;
            URISyntaxException ex = null;

            // userinfo
            q = scan(p, n, "/?#", "@");
            if ((q >= p) && at(q, n, '@')) {
                checkChars(p, q, L_USERINFO, H_USERINFO, "user info");
                userInfo = input.substring(p, q);
                p = q + 1;              // Skip '@'
            }

            // host        = IP-literal / IPv4address / reg-name
            if (at(p, n, '[')) {
                // IPv6address
                // DEVIATION from RFC3986: Support scope id
                p++;
                q = scan(p, n, "/?#", "]");
                if ((q > p) && at(q, n, ']')) {
                    // look for a "%" scope id
                    int r = scan(p, q, "%");
                    if (r != q) {
                        if (r+1 == q) {
                            fail ("scope id expected");
                        }
                        parseIPv6Reference(p, r);
                        checkChars(r+1, q, L_SCOPE_ID, H_SCOPE_ID,
                                "scope id");
                    } else {
                        parseIPv6ReferenceOrIPvFuture(p, q);
                    }
                    host = input.substring(p - 1, q + 1);
                    p = q + 1;
                } else {
                    failExpecting("closing bracket for IPv6 or IPvFuture address", q);
                }
            } else {
                q = parseIPv4Address(p, n);
                if (q <= p)
                    q = parseRegname(p, n);
                p = q;
            }

            // port
            if (at(p, n, ':')) {
                p++;
                q = scan(p, n, "/");
                if (q > p) {
                    checkChars(p, q, L_DIGIT, H_DIGIT, "port number");
                    try {
                        port = Integer.parseInt(input, p, q, 10);
                    } catch (NumberFormatException x) {
                        fail("Malformed port number", p);
                    }
                    p = q;
                }
            }
            if (p < n)
                failExpecting("port number", p);

            authority = input.substring(start, n);

            return n;
        }

        // Scan an IPv4 address.
        //
        // If the strict argument is true then we require that the given
        // interval contain nothing besides an IPv4 address; if it is false
        // then we only require that it start with an IPv4 address.
        //
        // If the interval does not contain or start with (depending upon the
        // strict argument) a legal IPv4 address characters then we return -1
        // immediately; otherwise we insist that these characters parse as a
        // legal IPv4 address and throw an exception on failure.
        //
        // We assume that any string of decimal digits and dots must be an IPv4
        // address.  It won't parse as a hostname anyway, so making that
        // assumption here allows more meaningful exceptions to be thrown.
        //
        private int scanIPv4Address(int start, int n, boolean strict)
            throws URISyntaxException
        {
            int p = start;
            int q = IRI.scanIPv4Address(input, start, n, strict);
            if (q == -1) return -1;
            if (q < -1) {
                fail("Malformed IPv4 address", -q - 2);
            }
            return q;
        }

        // Take an IPv4 address: Throw an exception if the given interval
        // contains anything except an IPv4 address
        //
        private int takeIPv4Address(int start, int n, String expected)
            throws URISyntaxException
        {
            int p = scanIPv4Address(start, n, true);
            if (p <= start)
                failExpecting(expected, start);
            return p;
        }

        // Attempt to parse an IPv4 address, returning -1 on failure but
        // allowing the given interval to contain [:<characters>] after
        // the IPv4 address.
        //
        private int parseIPv4Address(int start, int n) {
            int p;

            try {
                p = scanIPv4Address(start, n, false);
            } catch (URISyntaxException x) {
                return -1;
            } catch (NumberFormatException nfe) {
                return -1;
            }

            if (p > start && p < n) {
                // IPv4 address is followed by something - check that
                // it's a ":" as this is the only valid character to
                // follow an address.
                if (input.charAt(p) != ':') {
                    p = -1;
                }
            }

            if (p > start)
                host = input.substring(start, p);

            return p;
        }

        //
        // reg-name      = *( unreserved / pct-encoded / sub-delims )
        //
        // One corner case is that RFC3986's rule authority has replaced
        // RFC2396's rule server. But hostname as in 2396 can't be empty,
        // while host as in 3986 can. The context is:
        //      hier-part       = "//" authority path-abempty
        //      authority       = [ userinfo "@" ] host [ ":" port ]
        //      host            = IP-literal / IPv4address / reg-name
        //      path-abempty    = *( "/" segment )
        // So if reg-name is empty, the authority component must be
        // something like [ userinfo "@" ][ ":" port ], i.e. a :-sign
        // or a /-sign is anticipated if empty reg-name.
        //
        private int parseRegname(int start, int n)
                throws URISyntaxException
        {
            int p = start;
            int q = scan(p, n, L_REG_NAME, H_REG_NAME);
            if (q < n && input.charAt(q) != ':' && input.charAt(q) != '/')
                fail("Illegal character in hostname", q);
            host = input.substring(start, q);
            return q;
        }


        // IPv6 address parsing, from RFC2373: IPv6 Addressing Architecture
        //
        // Bug: The grammar in RFC2373 Appendix B does not allow addresses of
        // the form ::12.34.56.78, which are clearly shown in the examples
        // earlier in the document.  Here is the original grammar:
        //
        //   IPv6address = hexpart [ ":" IPv4address ]
        //   hexpart     = hexseq | hexseq "::" [ hexseq ] | "::" [ hexseq ]
        //   hexseq      = hex4 *( ":" hex4)
        //   hex4        = 1*4HEXDIG
        //
        // We therefore use the following revised grammar:
        //
        //   IPv6address = hexseq [ ":" IPv4address ]
        //                 | hexseq [ "::" [ hexpost ] ]
        //                 | "::" [ hexpost ]
        //   hexpost     = hexseq | hexseq ":" IPv4address | IPv4address
        //   hexseq      = hex4 *( ":" hex4)
        //   hex4        = 1*4HEXDIG
        //
        // This covers all and only the following cases:
        //
        //   hexseq
        //   hexseq : IPv4address
        //   hexseq ::
        //   hexseq :: hexseq
        //   hexseq :: hexseq : IPv4address
        //   hexseq :: IPv4address
        //   :: hexseq
        //   :: hexseq : IPv4address
        //   :: IPv4address
        //   ::
        //
        // Additionally we constrain the IPv6 address as follows :-
        //
        //  i.  IPv6 addresses without compressed zeros should contain
        //      exactly 16 bytes.
        //
        //  ii. IPv6 addresses with compressed zeros should contain
        //      less than 16 bytes.

        private int ipv6byteCount = 0;

        private int parseIPv6Reference(int start, int n)
            throws URISyntaxException
        {
            int p = start;
            int q;
            boolean compressedZeros = false;

            q = scanHexSeq(p, n);

            if (q > p) {
                p = q;
                if (at(p, n, "::")) {
                    compressedZeros = true;
                    p = scanHexPost(p + 2, n);
                } else if (at(p, n, ':')) {
                    p = takeIPv4Address(p + 1,  n, "IPv4 address");
                    ipv6byteCount += 4;
                }
            } else if (at(p, n, "::")) {
                compressedZeros = true;
                p = scanHexPost(p + 2, n);
            }
            if (p < n)
                fail("Malformed IPv6 address", start);
            if (ipv6byteCount > 16)
                fail("IPv6 address too long", start);
            if (!compressedZeros && ipv6byteCount < 16)
                fail("IPv6 address too short", start);
            if (compressedZeros && ipv6byteCount == 16)
                fail("Malformed IPv6 address", start);

            return p;
        }


        //
        // IPvFuture parsing :-
        //  IPvFuture     = "v" 1*HEXDIG "." 1*( unreserved / sub-delims / ":" )
        //
        private int parseIPvFuture(int start, int n)
                throws URISyntaxException
        {
            int p = start;
            int q;

            if (!at(p++, n, 'v')) {     // check and skip 'v'
                fail("Malformed IPvFuture address", p);
            }
            q = scan(p, n, L_HEX, H_HEX);
            if (q <= p) {
                fail("Malformed IPvFuture address", q);
            }

            p = q;
            if (!at(p++, n, '.')) {     // check and skip '.'
                fail("Malformed IPvFuture address", p);
            }

            q = scan(p, n, L_UNRESERVED | L_SUB_DELIMS | L_COLON,
                    H_UNRESERVED | H_SUB_DELIMS | H_COLON);

            if (q <= p || q != n) {
                fail("Malformed IPvFuture address", q);
            }

            return q;
        }

        private int parseIPv6ReferenceOrIPvFuture(int start, int n)
                throws URISyntaxException
        {
            if (input.charAt(start) == 'v') {
                return parseIPvFuture(start, n);
            } else {
                return parseIPv6Reference(start, n);
            }
        }

        private int scanHexPost(int start, int n)
            throws URISyntaxException
        {
            int p = start;
            int q;

            if (p == n)
                return p;

            q = scanHexSeq(p, n);
            if (q > p) {
                p = q;
                if (at(p, n, ':')) {
                    p++;
                    p = takeIPv4Address(p, n, "hex digits or IPv4 address");
                    ipv6byteCount += 4;
                }
            } else {
                p = takeIPv4Address(p, n, "hex digits or IPv4 address");
                ipv6byteCount += 4;
            }
            return p;
        }

        // Scan a hex sequence; return -1 if one could not be scanned
        //
        private int scanHexSeq(int start, int n)
            throws URISyntaxException
        {
            int p = start;
            int q;

            q = scan(p, n, L_HEX, H_HEX);
            if (q <= p)
                return -1;
            if (at(q, n, '.'))          // Beginning of IPv4 address
                return -1;
            if (q > p + 4)
                fail("IPv6 hexadecimal digit sequence too long", p);
            ipv6byteCount += 2;
            p = q;
            while (p < n) {
                if (!at(p, n, ':'))
                    break;
                if (at(p + 1, n, ':'))
                    break;              // "::"
                p++;
                q = scan(p, n, L_HEX, H_HEX);
                if (q <= p)
                    failExpecting("digits for an IPv6 address", p);
                if (at(q, n, '.')) {    // Beginning of IPv4 address
                    p--;
                    break;
                }
                if (q > p + 4)
                    fail("IPv6 hexadecimal digit sequence too long", p);
                ipv6byteCount += 2;
                p = q;
            }

            return p;
        }

    }

    /**
     * Returns a {@link Builder} built from this IRI, with
     * the given {@code capabilities} set.
     *
     * @apiNote
     * This method can be used to easily change some components
     * of an IRI - for instance:
     * <pre>
     *    IRI iri = ...;
     *    IRI changed = iri.with(Builder.DEFAULT_CAPABILITY)
     *          .scheme("https").build();</pre>
     * will return a new IRI identical to the original IRI
     * except that the scheme will have been substituted with
     * {@code "https"}.
     *
     * @implSpec
     * The new builder is populated with the components of this
     * IRI.
     * <p>The {@link Builder#QUOTE_ENCODED_CAPABILITY} capability,
     * if set, only impacts components which are explicitly
     * changed through the {@link Builder} API.
     * In all cases, {@code iri.with(cap).build().equals(iri)},
     * where {@code cap} is either {@link Builder#DEFAULT_CAPABILITY}
     * or {@link Builder#QUOTE_ENCODED_CAPABILITY}.</p>
     *
     * @implNote
     * This implementation also ensures that
     * {@code iri.toString().equals(iri.with(cap).build().toString()},
     * where {@code cap} is either {@link Builder#DEFAULT_CAPABILITY}
     * or {@link Builder#QUOTE_ENCODED_CAPABILITY}.
     *
     * @param capabilities The builder capability set.
     *
     * @return A new builder pre-populated with the components of
     *         this IRI, and with the given {@code capabilities}.
     */
    public Builder with(int capabilities) {
        return new Builder(this, capabilities);
    }

    /**
     * Creates a new {@link Builder}.
     * @return A new IRI Builder.
     */
    public static Builder newBuilder() {
        return newBuilder(Builder.DEFAULT_CAPABILITY);
    }

    /**
     * Creates a new {@link Builder}, with the given capabilities.
     * @param capabilities The new builder capability set.
     *
     * @implNote
     * This implementation only supports the following
     * capabilities:
     * <ul>
     *     <li>{@link Builder#DEFAULT_CAPABILITY}: the default capabilities
     *     (empty set).</li>
     *     <li>{@link Builder#QUOTE_ENCODED_CAPABILITY}: the builder will
     *     automatically {@linkplain #quoteEncodedOctets(String) quote}
     *     percent-encoded octets present in input parameters - in the
     *     same manner that {@link java.net.URI} used to do.</li>
     * </ul>
     * @return A new IRI Builder.
     */
    public static Builder newBuilder(int capabilities) {
        return new Builder(capabilities);
    }

    /**
     * A component-wise builder of IRIs.
     *
     * <p> If desired, a builder can be created with a set of enhanced
     * capabilities that may impact how input parameters are handled
     * or how the IRI is eventually built.
     *
     * <p> Because full validation of the IRI can only be performed once all
     * the components are known, then full validation of the URI syntax
     * is delayed until the builder's {@link #build()} method is called.
     *
     * @apiNote
     * Typical usage example:
     * <pre>
     *     IRI iri;
     *     try {
     *         iri = IRI.newBuilder()
     *             .scheme("http")
     *             .host("www.example.com")
     *             .path("/sample")
     *             .query("version=12")
     *             .build();
     *     } catch(URISyntaxException x) {
     *         System.out.println("Failed to build IRI: " + x.getMessage());
     *     }
     * </pre>
     *
     * @implNote
     * This implementation only supports the following capabilities:
     * <ul>
     *     <li>{@link Builder#DEFAULT_CAPABILITY}: the default capabilities
     *     (empty set).</li>
     *     <li>{@link Builder#QUOTE_ENCODED_CAPABILITY}: the builder will
     *     automatically {@linkplain #quoteEncodedOctets(String) re-quote}
     *     percent-encoded octets present in input parameters - in the
     *     same manner that {@link java.net.URI} used to do.</li>
     * </ul>
     *
     * @since TBD
     *
     */
    public static final class Builder {
        /**
         * Represents the default capabilities (an empty set).
         */
        public static final int DEFAULT_CAPABILITY = 0;

        /**
         * Represents the capability of a builder to pre-process its input
         * parameters by calling
         * {@link #quoteEncodedOctets(String) quoteEncodedOctets}.
         * This can be useful when migrating code that used to work with
         * {@link java.net.URI java.net.URI}.
         */
        public static final int QUOTE_ENCODED_CAPABILITY = 1;

        private String scheme, host, userinfo, path, opaque, query, fragment, authority;
        private int port = -1;
        private final int capabilities;

        // Creates a builder with the given set of capabilities.
        Builder(int capabilities) {
            this.capabilities = capabilities;
        }

        // Creates a builder from the give IRI, with the given set of
        // capabilities.
        Builder(IRI iri, int capabilities) {
            assert iri != null;
            this.capabilities = capabilities;
            // encoding in multi-args factories is idempotent,
            // so we can use the raw form of the component to
            // ensure that the string form of the IRI is
            // preserved - that is, we want to have:
            // iri.with(cap).build().toString().equals(iri.toString())
            scheme = iri.scheme;
            query = iri.query;
            fragment = iri.fragment;
            if (iri.isOpaque()) {
                this.opaque = iri.path;
            } else {
                host = iri.host;
                userinfo = iri.userInfo;
                path = iri.path;
                port = iri.port;
                String auth = iri.authority;
                if (auth != null && auth.endsWith(":")) {
                    // this is a bit of a hack to preserve
                    // non-canonical authority forms
                    authority = iri.authority;
                }
            }
        }

        /**
         * Sets the IRI <i>scheme</i> component.
         *
         * <p> No validation is performed by this method, but supplying an
         * invalid value may lead to an {@code URISyntaxException} when
         * {@link #build()} is called.
         *
         * @param scheme The IRI scheme. May be {@code null}, in which case
         *               the previously defined scheme, if any, will be erased.
         * @return this builder
         */
        public Builder scheme(String scheme) {
            this.scheme = scheme;
            return this;
        }

        /**
         * Sets the IRI <i>host</i> component.
         *
         * <p> If this builder has the {@link #QUOTE_ENCODED_CAPABILITY},
         * and the provided value is not a literal address, then the
         * provided {@code host} value will be pre-processed by calling
         * {@link #quoteEncodedOctets(String) quoteEncodedOctets(host)}
         * before storing it in this builder instance.
         *
         * <p> No validation is performed by this method, but supplying an
         * invalid value may lead to an {@code URISyntaxException} when
         * {@link #build()} is called.
         *
         * @param host The IRI host. May be {@code null}, in which case
         *             the previously defined host, if any, will be erased.
         *             If non-null, then any previously supplied
         *             {@link #authority(String) authority} will be
         *             erased.
         * @return this builder
         */
        public Builder host(String host) {
            this.host = checkEncodeHost(host);
            if (host != null) {
                this.authority = null;
            }
            return this;
        }

        /**
         * Sets the IRI <i>userinfo</i> component.
         *
         * <p> If this builder has the {@link #QUOTE_ENCODED_CAPABILITY}, the
         * provided {@code userinfo} value will be pre-processed by calling
         * {@link #quoteEncodedOctets(String) quoteEncodedOctets(userinfo)}
         * before storing it in this builder instance.
         *
         * <p> No validation is performed by this method, but supplying an
         * invalid value may lead to an {@code URISyntaxException} when
         * {@link #build()} is called.
         *
         * @param userinfo The IRI userinfo. May be {@code null}, in which case
         *             the previously defined userinfo, if any, will be erased.
         *             If non-null, then any previously supplied
         *             {@link #authority(String) authority} will be
         *             erased.
         * @return this builder
         */
        public Builder userinfo(String userinfo) {
            this.userinfo = checkEncode(userinfo);
            if (userinfo != null) {
                authority = null;
            }
            return this;
        }

        /**
         * Sets the IRI <i>port</i> component.
         *
         * <p> No validation is performed by this method, but supplying an
         * invalid value may lead to an {@code URISyntaxException} when
         * {@link #build()} is called.
         *
         * @param port The IRI port. May be {@code -1}, in which case
         *             the previously specified port, if any, will be erased.
         *             If positive, then any previously supplied
         *             {@link #authority(String) authority} will be
         *             erased.
         * @return this builder
         */
        public Builder port(int port) {
            this.port = port;
            if (port != -1) {
                authority = null;
            }
            return this;
        }

        /**
         * Sets the IRI <i>authority</i> component.
         *
         * <p> If this builder has the {@link #QUOTE_ENCODED_CAPABILITY}, the
         * provided {@code authority} value will be pre-processed by calling
         * {@link #quoteEncodedOctets(String) quoteEncodedOctets(authority)}
         * before storing it in this builder instance.
         *
         * <p> No validation is performed by this method, but supplying an
         * invalid value may lead to an {@code URISyntaxException} when
         * {@link #build()} is called.
         *
         * @param authority The IRI pre-composed authority.
         *           May be {@code null}, in which case the previously
         *           specified {@link #authority(String) authority}, if any,
         *           will be erased. If non-null, then any previously
         *           supplied {@link #host(String) host}, {@link
         *           #userinfo userinfo}, and {@link #port port} will be
         *           erased.
         * @return this builder
         */
        public Builder authority(String authority) {
            this.authority = checkEncode(authority);
            if (authority != null) {
                this.host = null;
                this.port = -1;
                this.userinfo = null;
            }
            return this;
        }

        /**
         * Sets the IRI <i>path</i> component.
         *
         * <p> If this builder has the {@link #QUOTE_ENCODED_CAPABILITY}, the
         * provided {@code path} value will be pre-processed by calling
         * {@link #quoteEncodedOctets(String) quoteEncodedOctets(path)}
         * before storing it in this builder instance.
         *
         * <p> No validation is performed by this method, but supplying an
         * invalid value may lead to an {@code URISyntaxException} when
         * {@link #build()} is called.
         *
         * @param path The IRI path. May be {@code null}, in which case
         *             the previously defined path, if any, will be erased.
         * @return this builder
         */
        public Builder path(String path) {
            this.path = checkEncode(path);
            if (path != null) {
                this.opaque = null;
            }
            return this;
        }

        /**
         * Sets the IRI <i>path</i> component to an opaque path.
         *
         * <p> If this builder has the {@link #QUOTE_ENCODED_CAPABILITY}, the
         * provided {@code path} value will be pre-processed by calling
         * {@link #quoteEncodedOctets(String) quoteEncodedOctets(path)}
         * before storing it in this builder instance.
         *
         * <p> No validation is performed by this method, but supplying an
         * invalid value may lead to an {@code URISyntaxException} when
         * {@link #build()} is called.
         *
         * @param opaque The IRI opaque path. May be {@code null}, in which case
         *             the previously defined opaque path, if any, will be erased.
         *             If non-null, then any previously
         *             supplied {@link #path(String) path}, {@link
         *             #host(String) host}, {@link
         *             #userinfo userinfo}, {@link #port port} and
         *             {@link #authority(String) authority} will be erased.
         * @return this builder
         */
        public Builder opaque(String opaque) {
            this.opaque = checkEncode(opaque);
            if (opaque != null) {
                this.path = this.authority = this.host = this.userinfo = null;
                this.port = -1;
            }
            return this;
        }

        /**
         * Sets the IRI <i>query</i> component.
         *
         * <p> If this builder has the {@link #QUOTE_ENCODED_CAPABILITY}, the
         * provided {@code query} value will be pre-processed by calling
         * {@link #quoteEncodedOctets(String) quoteEncodedOctets(query)}
         * before storing it in this builder instance.
         *
         * <p> No validation is performed by this method, but supplying an
         * invalid value may lead to an {@code URISyntaxException} when
         * {@link #build()} is called.
         *
         * @param query The IRI query. May be {@code null}, in which case
         *             any previously defined query will be erased.
         * @return this builder
         */
        public Builder query(String query) {
            this.query = checkEncode(query);
            return this;
        }

        /**
         * Sets the IRI <i>fragment</i> component.
         *
         * <p> If this builder has the {@link #QUOTE_ENCODED_CAPABILITY}, the
         * provided {@code fragment} value will be pre-processed by calling
         * {@link #quoteEncodedOctets(String) quoteEncodedOctets(fragment)}
         * before storing it in this builder instance.
         *
         * <p> No validation is performed by this method, but supplying an
         * invalid value may lead to an {@code URISyntaxException} when
         * {@link #build()} is called.
         *
         * @param fragment The IRI query. May be {@code null}, in which case
         *             the previously defined fragment, if, any, will be erased.
         * @return this builder
         */
        public Builder fragment(String fragment) {
            this.fragment = checkEncode(fragment);
            return this;
        }

        /**
         * Builds an IRI from the components stored in this builder.
         *
         * <p> The IRI components will be encoded as specified for the
         * {@link #createHierarchical(String, String, String, String, String)
         * IRI.createHierarchical(scheme, authority, path, query, fragment)} method,
         * if a composed {@linkplain #authority(String) authority} component was
         * explicitly supplied, or by the
         * {@link #createOpaque(String, String, String, String)
         *  URI.createOpaque(scheme, opaque, query, fragment)} method, if an {@linkplain
         * #opaque(String) opaque path} component was explicitly supplied, or by the
         * {@link #createHierarchical(String, String, String, int, String, String, String)
         * IRI.createHierarchical(scheme, userinfo, host, port, path, query, fragment)}
         * method, otherwise.
         *
         * @return A new IRI built from the specified components.
         * @throws URISyntaxException If the resulting IRI is invalid and
         *         could not be constructed.
         * @throws IllegalArgumentException If the provided components are not
         *         consistent. For instance,
         *         if an opaque path was provided with no scheme;
         *         or if both a scheme and a path are given but the path is relative;
         *         or if a userinfo or port was provided with no host;
         *         or if no authority or host is provided and the {@code path}
         *         component starts with {@code "//"} or contains {@code ':'} before
         *         the first {@code '/'};
         *         or if a host or authority are provided without a scheme, but with a
         *         path which is not empty and does not start with {@code '/'}.
         *
         * @see #buildUnchecked()
         */
        public IRI build() throws URISyntaxException {
            if (opaque != null) {
                return IRI.createOpaque(scheme, opaque, query, fragment);
            } else if (authority == null) {
                return IRI.createHierarchical(scheme, userinfo, host, port, path, query, fragment);
            } else {
                // host and userinfo may be non-null if this builder was seeded with
                // an IRI whose authority component ended with ':'
                assert authority.endsWith(":") || host == null && userinfo == null;
                assert port == -1;
                return IRI.createHierarchical(scheme, authority, path, query, fragment);
            }
        }

        /**
         * Builds an IRI from the components stored in this builder.
         *
         * <p> This convenience factory method works as if by invoking the
         * {@link #build() build} method; any {@link URISyntaxException} thrown
         * by {@code build} is caught and wrapped in a new
         * {@link IllegalArgumentException} instance, which is then thrown.
         *
         * @apiNote
         * <p> This method is provided for use in situations where it is known
         * that the given components form a legal IRI, for example with constant
         * IRI literals declared within a program, and so it would be considered
         * a programming error for the IRI not to parse as such. The
         * {@code build} method, which throws {@link URISyntaxException}
         * directly, should be used in situations where an IRI is being created
         * from user input or from some other source that may be prone to
         * errors.
         *
         * @return A new IRI built from the specified components.
         * @throws IllegalArgumentException If the resulting IRI is invalid and
         *         could not be constructed.
         */
        public IRI buildUnchecked() {
            try {
                return build();
            } catch (URISyntaxException x) {
                throw new IllegalArgumentException(x.getMessage(), x);
            }
        }

        private String checkEncode(String param) {
            if (param == null) return null;
            return (capabilities & QUOTE_ENCODED_CAPABILITY) == 0
                    ? param : quoteEncodedOctets(param);
        }

        private String checkEncodeHost(String host) {
            if (host == null) return null;
            if ((capabilities & QUOTE_ENCODED_CAPABILITY) == 0)
                return host;
            HostType type = getHostType(host, false);
            return type.isLiteral() ? host : quoteEncodedOctets(host);
        }

    }

    /**
     * Returns a {@link java.net.URI URI} equivalent to this IRI.
     *
     * @apiNote
     *
     *    This method is provided to ease migration and
     *    retain compatibility with those APIs that only
     *    accept {@link java.net.URI java.net.URI} instances.
     *
     *    <p> Because {@code java.net.URI} supports an older version
     *    of the <i>Uniform Resource Identifier: Generic Syntax</i>
     *    RFC, then not all IRIs might be converted into URIs.
     *    For instance, IRIs of the form {@code "about:"} are not
     *    parsable by {@code java.net.URI}. In that case, this
     *    method will throw a {@code URISyntaxException}. </p>
     *
     *    <p> This implementation will additionally remove the superfluous
     *    colon at the end of the authority component, if the raw authority
     *    component ends with {@code ':'}.
     *    Thus, an IRI whose {@linkplain #toString() string form} is
     *    {@code "file://:/path"} (empty host, empty port), will be converted
     *    into a {@code java.net.URI} whose {@linkplain URI#toString()
     *    string form} is {@code "file:///path"}. </p>
     *
     * @return A {@code java.net.URI} instance equivalent to this IRI.
     *
     * @throws URISyntaxException if this IRI cannot be converted into a
     *         {@code java.net.URI} instance.
     */
    public URI toURI() throws URISyntaxException {
        String str;
        if (authority != null && authority.endsWith(":")) {
            str = buildString(new StringBuilder(),
                    scheme,
                    userInfo, host, port,
                    path, query, fragment).toString();
        } else {
            str = defineString();
        }
        return new URI(str);
    }

//    static {
//        SharedSecrets.setJavaNetUriAccess(
//            new JavaNetUriAccess() {
//                public IRI create(String scheme, String path) {
//                    return new IRI(scheme, path);
//                }
//            }
//        );
//    }
}
