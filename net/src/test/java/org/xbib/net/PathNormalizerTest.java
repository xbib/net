package org.xbib.net;

import org.junit.jupiter.api.Test;
import org.xbib.net.PathNormalizer;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PathNormalizerTest {

    @Test
    void normalizeNullPath() {
        assertEquals("/", PathNormalizer.normalize(null));
    }

    @Test
    void normalizeEmptyPath() {
        assertEquals("/", PathNormalizer.normalize(""));
    }

    @Test
    void normalizeSlashPath() {
        assertEquals("/", PathNormalizer.normalize("/"));
    }

    @Test
    void normalizeDoubleSlashPath() {
        assertEquals("/", PathNormalizer.normalize("//"));
    }

    @Test
    void normalizeTripleSlashPath() {
        assertEquals("/", PathNormalizer.normalize("///"));
    }

    @Test
    void normalizePathWithPoint() {
        assertEquals("/", PathNormalizer.normalize("/."));
    }

    @Test
    void normalizePathWithPointAndElement() {
        assertEquals("/a", PathNormalizer.normalize("/./a"));
    }

    @Test
    void normalizePathWithTwoPointsAndElement() {
        assertEquals("/a", PathNormalizer.normalize("/././a"));
    }

    @Test
    void normalizePathWithDoublePoint() {
        assertEquals("/", PathNormalizer.normalize("/.."));
        assertEquals("/", PathNormalizer.normalize("/../.."));
        assertEquals("/", PathNormalizer.normalize("/../../.."));
    }

    @Test
    void normalizePathWithFirstElementAndDoublePoint() {
        assertEquals("/", PathNormalizer.normalize("/a/.."));
        assertEquals("/", PathNormalizer.normalize("/a/../.."));
        assertEquals("/", PathNormalizer.normalize("/a/../../.."));
    }

    @Test
    void normalizePathWithTwoElementsAndDoublePoint() {
        assertEquals("/b", PathNormalizer.normalize("/a/../b"));
        assertEquals("/b", PathNormalizer.normalize("/a/../../b"));
        assertEquals("/b", PathNormalizer.normalize("/a/../../../b"));
    }

    @Test
    void doNotnormalizeEmbeddedSemicolon() {
        assertEquals("/auth/cert;foo=bar/smartcard.xhtml", PathNormalizer.normalize("/auth/cert;foo=bar/smartcard.xhtml"));
    }
}
