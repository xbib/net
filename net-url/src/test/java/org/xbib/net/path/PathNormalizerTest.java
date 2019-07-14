package org.xbib.net.path;

import org.junit.jupiter.api.Test;

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
}
