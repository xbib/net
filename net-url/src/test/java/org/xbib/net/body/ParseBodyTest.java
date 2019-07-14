package org.xbib.net.body;

import org.junit.jupiter.api.Test;
import org.xbib.net.QueryParameters;

import java.nio.charset.MalformedInputException;
import java.nio.charset.UnmappableCharacterException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ParseBodyTest {

    @Test
    void testSimpleParse() throws MalformedInputException, UnmappableCharacterException {
        QueryParameters queryParameters = new QueryParameters();
        String body = "a=b&c=d&e=f";
        queryParameters.addPercentEncodedBody(body);
        assertEquals("b", queryParameters.get("a").get(0));
        assertEquals("d", queryParameters.get("c").get(0));
        assertEquals("f", queryParameters.get("e").get(0));
    }

    @Test
    void testManyParse() throws MalformedInputException, UnmappableCharacterException {
        QueryParameters queryParameters = new QueryParameters(100);
        List<String> list = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            list.add("a" + i + "=b" + i );
        }
        String body = String.join("&", list);
        queryParameters.addPercentEncodedBody(body);
        assertEquals("b0", queryParameters.get("a0").get(0));
        assertEquals("b99", queryParameters.get("a99").get(0));
        assertEquals("[]", queryParameters.get("a100").toString());
    }
}
