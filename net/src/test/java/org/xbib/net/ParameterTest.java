package org.xbib.net;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ParameterTest {

    @Test
    public void testEmptyBuilder() {
        Parameter parameter = Parameter.builder().build();
        assertNotNull(parameter);
        assertEquals("DEFAULT", parameter.getDomain());
        assertFalse(parameter.containsKey("DEFAULT", "param1"));
    }

    @Test
    public void testSingleParameter() {
        Parameter parameter = Parameter.builder()
                .add("Hello", "World")
                .build();
        assertNotNull(parameter);
        assertEquals("DEFAULT", parameter.getDomain());
        assertTrue(parameter.containsKey("DEFAULT", "Hello"));
    }

    @Test
    public void testDuplicateParameter() {
        Parameter parameter = Parameter.builder()
                .enableDuplicates()
                .add("Hello", "World")
                .add("Hello", "World")
                .add("Hello", "World")
                .build();
        assertNotNull(parameter);
        assertEquals("DEFAULT", parameter.getDomain());
        assertTrue(parameter.containsKey("DEFAULT", "Hello"));
        assertEquals(List.of("World", "World", "World"), parameter.getAll("DEFAULT", "Hello"));
    }

    @Test
    public void testHttpHeaderParameter() {
        Parameter parameter = Parameter.builder()
                .charset(StandardCharsets.US_ASCII)
                .lowercase()
                .domain("HEADER")
                .add("Content-Type", "text/plain")
                .add("Accept", "*/*")
                .add("Connection", "close")
                .build();
        assertNotNull(parameter);
        assertEquals("HEADER", parameter.getDomain());
        assertTrue(parameter.containsKey("HEADER", "content-type"));
        assertEquals(List.of("close"), parameter.getAll("HEADER", "connection"));
    }

    @Test
    public void testQueryParameters() {
        Map<String, Object> map = Map.of(
                "version", "1.1",
                "operation", "searchRetrieve",
                "recordSchema", "MARC21plus-1-xml",
                "query", "iss = 00280836"
        );
        Parameter parameter = Parameter.builder()
                .enableSort()
                .enableQueryString(true)
                .add(map)
                .build();
        assertEquals("operation=searchRetrieve&query=iss%20%3D%2000280836&recordSchema=MARC21plus-1-xml&version=1.1",
                parameter.getAsQueryString());
    }

    @Test
    public void testParameters() {
        Map<String, Object> map = Map.of(
                "version", "1.1",
                "operation", "searchRetrieve",
                "recordSchema", "MARC21plus-1-xml",
                "query", "iss = 00280836"
        );
        Parameter parameter = Parameter.builder()
                .enableSort()
                .enableQueryString(true)
                .add(map)
                .build();
        assertEquals("operation=searchRetrieve&query=iss%20%3D%2000280836&recordSchema=MARC21plus-1-xml&version=1.1",
                parameter.getAsQueryString());
    }

    @Test
    public void testMutatorQueryParameters() {
        URL url = URL.from("http://localhost");
        String requestPath = "/path";
        Map<String, Object> map = Map.of(
                "version", "1.1",
                "operation", "searchRetrieve",
                "recordSchema", "MARC21plus-1-xml",
                "query", "iss = 00280836"
        );
        Parameter httpParameter = Parameter.builder()
                .enableSort()
                .enableQueryString(true)
                .add(map)
                .build();
        URLBuilder mutator = url.mutator();
        mutator.path(requestPath);
        httpParameter.stream("DEFAULT").forEach(e -> mutator.queryParam(e.getKey(), e.getValue()));
        url = mutator.build();
        assertEquals("http://localhost/path?operation=searchRetrieve&query=iss%20%3D%2000280836&recordSchema=MARC21plus-1-xml&version=1.1",
                url.toExternalForm());
    }

    @Test
    void testSimpleParse() {
        ParameterBuilder queryParameters = Parameter.builder();
        String body = "a=b&c=d&e=f";
        queryParameters.addPercentEncodedBody(body);
        Parameter parameter = queryParameters.build();
        assertEquals("b", parameter.getAll("DEFAULT", "a").get(0));
        assertEquals("d", parameter.getAll("DEFAULT", "c").get(0));
        assertEquals("f", parameter.getAll("DEFAULT", "e").get(0));
    }

    @Test
    void testParseExceedingParamLimit() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            ParameterBuilder queryParameters = Parameter.builder().limit(100);
            List<String> list = new ArrayList<>();
            for (int i = 0; i < 200; i++) {
                list.add("a" + i + "=b" + i);
            }
            String body = String.join("&", list);
            queryParameters.addPercentEncodedBody(body);
            Parameter parameter = queryParameters.build();
            assertEquals("b0", parameter.getAll("DEFAULT", "a0").get(0));
            assertEquals("b99", parameter.getAll("DEFAULT", "a99").get(0));
            assertEquals("[]", parameter.getAll("DEFAULT", "a100").toString());
        });
    }

    @Test
    void testSubDomains() {
        Parameter p1 = Parameter.builder().domain("A").add("a", "a").build();
        Parameter p2 = Parameter.builder().domain("B").add("b", "b").build();
        Parameter p3 = Parameter.builder().domain("C").add("c", "c").build();
        Parameter p = Parameter.builder()
                .add(p1)
                .add(p2)
                .add(p3)
                .build();
        assertEquals("[a]", p.get("A", "a").toString());
        assertEquals("[b]", p.get("B", "b").toString());
        assertEquals("[c]", p.get("C", "c").toString());
    }
}
