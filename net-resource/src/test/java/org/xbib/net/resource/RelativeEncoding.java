package org.xbib.net.resource;
/*
 * Copyright (c) 2003, 2019 Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4866303
 * @summary URI.resolve escapes characters in parameter URI
 */

import java.io.File;
import java.net.URISyntaxException;
import java.util.Objects;

public class RelativeEncoding {
    public static void main(String[] args) {
        try {
            IRI one = IRI.parseIRI("Relative%20with%20spaces");
            // TODO: add a File.toIRI() method?
            IRI two = IRI.parseIRI((new File("/tmp/dir with spaces/File with spaces")).toURI().toString());
            IRI three = two.resolve(one);
            if (three.isOpaque())
                throw new RuntimeException("Bad encoding on IRI.resolve: should not be opaque");
            System.out.println(String.format("resolved path is: \"%s\" [\"%s\"]",
                    three.getPath(), three.getRawPath()));
            checkEquals("/tmp/dir with spaces/Relative with spaces",
                    three.getPath(), "path");
            checkEquals("/tmp/dir%20with%20spaces/Relative%20with%20spaces",
                    three.getRawPath(), "raw path");
        } catch (URISyntaxException e) {
            throw new RuntimeException("Unexpected exception: " + e);
        }
    }

    static void checkEquals(String expected, String found, String name) {
        if (!Objects.equals(expected, found)) {
            throw new RuntimeException(
                    String.format("Unexpected %s: \"%s\"\n\t expected \"%s\"",
                            name, found, expected));
        }
    }
}
