/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

/**
 * Represents an abstract Resource Identifier reference.
 *
 * <p> The particular conformance with any standards, parsing and construction
 * behaviour, and operational aspects of the Resource Identifier are specified
 * by a concrete subtype. This class does not have a public accessible
 * constructor, therefore the only valid subtypes are those that are defined
 * by the Java Platform. The only known subtypes are {@link URI} ( that conforms
 * to the obsoleted <i>RFC 2396</i>) and {@link IRI} ( that conforms to the
 * more recent <i>RFC 3986 / STD 66</i> and <i>RFC 3987</i> ).
 *
 * <p> The components of the Resource Identifier are retrievable through the
 * methods of this class. Both the raw and decoded forms of the components are
 * retrievable. The raw form of a component is the value of the component
 * without any interpretation or conversation. The decoded form of a component
 * is the value of the raw form after a single decoding pass that decodes UTF-8
 * percent-encoded triplets. A concrete subtype will further define the specific
 * set of allowable characters within each component.
 *
 */
public abstract class ResourceIdentifier {

    /* package-private */ ResourceIdentifier() { }

    /**
     * Tells whether or not this resource identifier is absolute.
     *
     * <p> A resource identifier is absolute if, and only if, it has a
     * scheme component.
     *
     * @return  {@code true} if, and only if, this resource identifier
     *          is absolute
     */
    public abstract boolean isAbsolute();

    /**
     * Tells whether or not this resource identifier is considered opaque.
     *
     * @return  {@code true} if, and only if, this resource identifier
     *          is considered opaque
     */
    public abstract boolean isOpaque();

    /**
     * Returns the scheme component of this resource identifier.
     *
     * @return  The scheme component of this resource identifier,
     *          or {@code null} if the scheme is undefined
     */
    public abstract String getScheme();

    /**
     * Returns the raw authority component of this resource identifier.
     *
     * @return  The raw authority component of this resource identifier,
     *          or {@code null} if the authority is undefined
     */
    public abstract String getRawAuthority();

    /**
     * Returns the decoded authority component of this resource identifier.
     *
     * @return  The decoded authority component of this resource identifier,
     *          or {@code null} if the authority is undefined
     */
    public abstract String getAuthority();

    /**
     * Returns the raw user-information component of this resource identifier.
     *
     * @return  The raw user-information component of this resource identifier,
     *          or {@code null} if the user information is undefined
     */
    public abstract String getRawUserInfo();

    /**
     * Returns the decoded user-information component of this resource identifier.
     *
     * @return  The decoded user-information component of this resource identifier,
     *          or {@code null} if the user information is undefined
     */
    public abstract String getUserInfo();

    /**
     * Returns the host component of this resource identifier.
     *
     * @return  The host component of this resource identifier,
     *          or {@code null} if the host is undefined, or does
     *          not parse as a syntactically valid internet name.
     */
    public abstract String getHost();

    /**
     * Returns the port number of this resource identifier.
     *
     * @return  The port component of this resource identifier,
     *          or {@code -1} if the port is undefined
     */
    public abstract int getPort();

    /**
     * Returns the raw path component of this resource identifier.
     *
     * @apiNote
     *
     * Different subclasses may return {@code null} on different
     * conditions. For instance {@code java.net.URI} will always
     * return {@code null} if the URI is opaque, while {@code
     * java.net.IRI} will simply return the opaque path.
     *
     * @return  The path component of this resource identifier,
     *          or {@code null} if the path is undefined
     */
    public abstract String getRawPath();

    /**
     * Returns the decoded path component of this resource identifier.
     *
     * @apiNote
     *
     * Different subclasses may return {@code null} on different
     * conditions. For instance {@code java.net.URI} will always
     * return {@code null} if the URI is opaque, while {@code
     * java.net.IRI} will simply return the decoded opaque path.
     *
     * @return  The decoded path component of this resource identifier,
     *          or {@code null} if the path is undefined
     */
    public abstract String getPath();

    /**
     * Returns the raw query component of this resource identifier.
     *
     * @return  The raw query component of this resource identifier,
     *          or {@code null} if the query is undefined
     */
    public abstract String getRawQuery();

    /**
     * Returns the decoded query component of this resource identifier.
     *
     * @apiNote
     *
     * Different subclasses may return {@code null} on different
     * conditions. For instance {@code java.net.URI} will always
     * return {@code null} if the URI is opaque.
     *
     * @return  The decoded query component of this resource identifier,
     *          or {@code null} if the query is undefined
     */
    public abstract String getQuery();

    /**
     * Returns the raw fragment component of this resource identifier.
     *
     * @return  The raw fragment component of this resource identifier,
     *          or {@code null} if the fragment is undefined
     */
    public abstract String getRawFragment();

    /**
     * Returns the decoded fragment component of this resource identifier.
     *
     * @return  The decoded fragment component of this URresource identifierI,
     *          or {@code null} if the fragment is undefined
     */
    public abstract String getFragment();

    /**
     * Returns the content of this resource identifier as a US-ASCII string.
     *
     * @return  The string form of this resource identifier, encoded as needed
     *          so that it only contains characters in the US-ASCII charset
     */
    public abstract String toASCIIString();

}
