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
        assertFalse(parameter.containsKey("param1", "DEFAULT"));
    }

    @Test
    public void testSingleParameter() {
        Parameter parameter = Parameter.builder()
                .add("Hello", "World")
                .build();
        assertNotNull(parameter);
        assertTrue(parameter.containsKey("Hello", "DEFAULT"));
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
        assertTrue(parameter.containsKey("Hello", "DEFAULT"));
        assertEquals(List.of("World", "World", "World"), parameter.getAll("Hello", "DEFAULT"));
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
        assertTrue(parameter.containsKey("content-type", "HEADER"));
        assertEquals(List.of("close"), parameter.getAll( "connection", "HEADER"));
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
        httpParameter.forEach(e -> mutator.queryParam(e.getKey(), e.getValue()));
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
        assertEquals("b", parameter.getAll("a", "DEFAULT").get(0));
        assertEquals("d", parameter.getAll("c", "DEFAULT").get(0));
        assertEquals("f", parameter.getAll("e", "DEFAULT").get(0));
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
            assertEquals("b0", parameter.getAll("a0", "DEFAULT").get(0));
            assertEquals("b99", parameter.getAll("a99", "DEFAULT").get(0));
            assertEquals("[]", parameter.getAll("a100", "DEFAULT").toString());
        });
    }

    @Test
    void testDomains() {
        Parameter p1 = Parameter.builder().domain("A").add("a", "a").build();
        Parameter p2 = Parameter.builder().domain("B").add("b", "b").build();
        Parameter p3 = Parameter.builder().domain("C").add("c", "c").build();
        Parameter p = Parameter.builder()
                .add(p1)
                .add(p2)
                .add(p3)
                .build();
        assertEquals("a", p.get("a", "A").toString());
        assertEquals("b", p.get("b", "B").toString());
        assertEquals("c", p.get("c", "C").toString());
        assertEquals("[] A -> [a=a] B -> [b=b] C -> [c=c]", p.toString());
        assertEquals("a", p.get("a", "A", "B", "C"));
        assertEquals("b", p.get("b", "A", "B", "C"));
        assertEquals("c", p.get("c", "A", "B", "C"));
        assertTrue(p.isPresent("A"));
        assertTrue(p.isPresent("B"));
        assertTrue(p.isPresent("C"));
        assertFalse(p.isPresent("D"));
    }
}
