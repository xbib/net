package org.xbib.net;

import java.util.List;
import org.xbib.datastructures.common.Pair;

/**
 * Headers should never be a hash map. Never.
 */
public interface Headers {

    String get(CharSequence name);

    List<String> getAll(CharSequence name);

    List<Pair<String, String>> entries();
}
