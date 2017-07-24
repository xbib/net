package org.xbib.net.path;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 *
 */
public class PathNormalizerTest {

    @Test
    public void normalizeNullPath() {
        assertEquals("/", PathNormalizer.normalize(null));
    }

    @Test
    public void normalizeEmptyPath() {
        assertEquals("/", PathNormalizer.normalize(""));
    }

    @Test
    public void normalizeSlashPath() {
        assertEquals("/", PathNormalizer.normalize("/"));
    }

    @Test
    public void normalizeDoubleSlashPath() {
        assertEquals("/", PathNormalizer.normalize("//"));
    }

    @Test
    public void normalizeTripleSlashPath() {
        assertEquals("/", PathNormalizer.normalize("///"));
    }

    @Test
    public void normalizePathWithPoint() {
        assertEquals("/", PathNormalizer.normalize("/."));
    }

    @Test
    public void normalizePathWithPointAndElement() {
        assertEquals("/a", PathNormalizer.normalize("/./a"));
    }

    @Test
    public void normalizePathWithTwoPointsAndElement() {
        assertEquals("/a", PathNormalizer.normalize("/././a"));
    }

    @Test
    public void normalizePathWithDoublePoint() {
        assertEquals("/", PathNormalizer.normalize("/.."));
        assertEquals("/", PathNormalizer.normalize("/../.."));
        assertEquals("/", PathNormalizer.normalize("/../../.."));
    }

    @Test
    public void normalizePathWithFirstElementAndDoublePoint() {
        assertEquals("/", PathNormalizer.normalize("/a/.."));
        assertEquals("/", PathNormalizer.normalize("/a/../.."));
        assertEquals("/", PathNormalizer.normalize("/a/../../.."));
    }

    @Test
    public void normalizePathWithTwoElementsAndDoublePoint() {
        assertEquals("/b", PathNormalizer.normalize("/a/../b"));
        assertEquals("/b", PathNormalizer.normalize("/a/../../b"));
        assertEquals("/b", PathNormalizer.normalize("/a/../../../b"));
    }

}
