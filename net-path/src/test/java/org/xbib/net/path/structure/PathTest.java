package org.xbib.net.path.structure;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class PathTest {

    @Test
    void testComparator() {
        Comparator<Path> comparator = new PathComparator("/hotels/new");
        assertEquals(0, comparator.compare(null, null));
        assertEquals(1, comparator.compare(null, Path.of("/hotels/new")));
        assertEquals(-1, comparator.compare(Path.of("/hotels/new"), null));
        assertEquals(0, comparator.compare(Path.of("/hotels/new"), Path.of("/hotels/new")));
        assertEquals(-1, comparator.compare(Path.of("/hotels/new"), Path.of("/hotels/*")));
        assertEquals(1, comparator.compare(Path.of("/hotels/*"), Path.of("/hotels/new")));
        assertEquals(0, comparator.compare(Path.of("/hotels/*"), Path.of("/hotels/*")));
        assertEquals(-1, comparator.compare(Path.of("/hotels/new"), Path.of("/hotels/{hotel}")));
        assertEquals(1, comparator.compare(Path.of("/hotels/{hotel}"), Path.of("/hotels/new")));
        assertEquals(0, comparator.compare(Path.of("/hotels/{hotel}"), Path.of("/hotels/{hotel}")));
        assertEquals(-1, comparator.compare(Path.of("/hotels/{hotel}/booking"), Path.of("/hotels/{hotel}/bookings/{booking}")));
        assertEquals(1, comparator.compare(Path.of("/hotels/{hotel}/bookings/{booking}"), Path.of("/hotels/{hotel}/booking")));
        assertEquals(-1, comparator.compare(Path.of("/hotels/{hotel}/bookings/{booking}/cutomers/{customer}"), Path.of("/**")));
        assertEquals(1, comparator.compare(Path.of("/**"), Path.of("/hotels/{hotel}/bookings/{booking}/cutomers/{customer}")));
        assertEquals(0, comparator.compare(Path.of("/**"), Path.of("/**")));
        assertEquals(-1, comparator.compare(Path.of("/hotels/{hotel}"), Path.of("/hotels/*")));
        assertEquals(1, comparator.compare(Path.of("/hotels/*"), Path.of("/hotels/{hotel}")));
        assertEquals(-1, comparator.compare(Path.of("/hotels/*"), Path.of("/hotels/*/**")));
        assertEquals(1, comparator.compare(Path.of("/hotels/*/**"), Path.of("/hotels/*")));
        assertEquals(-1, comparator.compare(Path.of("/hotels/new"), Path.of("/hotels/new.*")));
        assertEquals(2, comparator.compare(Path.of("/hotels/{hotel}"), Path.of("/hotels/{hotel}.*")));
        assertEquals(-1, comparator.compare(Path.of("/hotels/{hotel}/bookings/{booking}/cutomers/{customer}"), Path.of("/hotels/**")));
        assertEquals(1, comparator.compare(Path.of("/hotels/**"), Path.of("/hotels/{hotel}/bookings/{booking}/cutomers/{customer}")));
        assertEquals(1, comparator.compare(Path.of("/hotels/foo/bar/**"), Path.of("/hotels/{hotel}")));
        assertEquals(-1, comparator.compare(Path.of("/hotels/{hotel}"), Path.of("/hotels/foo/bar/**")));
        assertEquals(2, comparator.compare(Path.of("/hotels/**/bookings/**"), Path.of("/hotels/**")));
        assertEquals(-2, comparator.compare(Path.of("/hotels/**"), Path.of("/hotels/**/bookings/**")));
        assertEquals(1, comparator.compare(Path.of("/**"), Path.of("/hotels/{hotel}")));
        assertEquals(1, comparator.compare(Path.of("/hotels"), Path.of("/hotels2")));
        assertEquals(-1, comparator.compare(Path.of("*"), Path.of("*/**")));
        assertEquals(1, comparator.compare(Path.of("*/**"), Path.of("*")));
    }

    @Test
    void testPatternComparatorSort() {
        Comparator<Path> comparator = new PathComparator("/hotels/new");

        List<Path> paths = new ArrayList<>();
        paths.add(null);
        paths.add(Path.of("/hotels/new"));
        paths.sort(comparator);
        assertEquals("/hotels/new", paths.get(0).getPathSpec());
        assertNull(paths.get(1));

        paths = new ArrayList<>();
        paths.add(Path.of("/hotels/new"));
        paths.add(null);
        paths.sort(comparator);
        assertEquals("/hotels/new", paths.get(0).getPathSpec());
        assertNull(paths.get(1));

        paths = new ArrayList<>();
        paths.add(Path.of("/hotels/*"));
        paths.add(Path.of("/hotels/new"));
        paths.sort(comparator);
        assertEquals("/hotels/new", paths.get(0).getPathSpec());
        assertEquals("/hotels/*", paths.get(1).getPathSpec());

        paths = new ArrayList<>();
        paths.add(Path.of("/hotels/new"));
        paths.add(Path.of("/hotels/*"));
        paths.sort(comparator);
        assertEquals("/hotels/new", paths.get(0).getPathSpec());
        assertEquals("/hotels/*", paths.get(1).getPathSpec());

        paths = new ArrayList<>();
        paths.add(Path.of("/hotels/**"));
        paths.add(Path.of("/hotels/*"));
        paths.sort(comparator);
        assertEquals("/hotels/*", paths.get(0).getPathSpec());
        assertEquals("/hotels/**", paths.get(1).getPathSpec());

        paths = new ArrayList<>();
        paths.add(Path.of("/hotels/*"));
        paths.add(Path.of("/hotels/**"));
        paths.sort(comparator);
        assertEquals("/hotels/*", paths.get(0).getPathSpec());
        assertEquals("/hotels/**", paths.get(1).getPathSpec());

        paths = new ArrayList<>();
        paths.add(Path.of("/hotels/{hotel}"));
        paths.add(Path.of("/hotels/new"));
        paths.sort(comparator);
        assertEquals("/hotels/new", paths.get(0).getPathSpec());
        assertEquals("/hotels/{hotel}", paths.get(1).getPathSpec());

        paths = new ArrayList<>();
        paths.add(Path.of("/hotels/new"));
        paths.add(Path.of("/hotels/{hotel}"));
        paths.sort(comparator);
        assertEquals("/hotels/new", paths.get(0).getPathSpec());
        assertEquals("/hotels/{hotel}", paths.get(1).getPathSpec());

        paths = new ArrayList<>();
        paths.add(Path.of("/hotels/*"));
        paths.add(Path.of("/hotels/{hotel}"));
        paths.add(Path.of("/hotels/new"));
        paths.sort(comparator);
        assertEquals("/hotels/new", paths.get(0).getPathSpec());
        assertEquals("/hotels/{hotel}", paths.get(1).getPathSpec());
        assertEquals("/hotels/*", paths.get(2).getPathSpec());

        paths = new ArrayList<>();
        paths.add(Path.of("/hotels/ne*"));
        paths.add(Path.of("/hotels/n*"));
        Collections.shuffle(paths);
        paths.sort(comparator);
        assertEquals("/hotels/ne*", paths.get(0).getPathSpec());
        assertEquals("/hotels/n*", paths.get(1).getPathSpec());

        paths = new ArrayList<>();
        comparator = new PathComparator("/hotels/new.html");
        paths.add(Path.of("/hotels/new.*"));
        paths.add(Path.of("/hotels/{hotel}"));
        Collections.shuffle(paths);
        paths.sort(comparator);
        assertEquals("/hotels/new.*", paths.get(0).getPathSpec());
        assertEquals("/hotels/{hotel}", paths.get(1).getPathSpec());

        paths = new ArrayList<>();
        comparator = new PathComparator("/web/endUser/action/login.html");
        paths.add(Path.of("/**/login.*"));
        paths.add(Path.of("/**/endUser/action/login.*"));
        paths.sort(comparator);
        assertEquals("/**/endUser/action/login.*", paths.get(0).getPathSpec());
        assertEquals("/**/login.*", paths.get(1).getPathSpec());
    }
}
