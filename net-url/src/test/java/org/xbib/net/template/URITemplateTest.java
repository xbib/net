package org.xbib.net.template;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import org.junit.jupiter.api.Test;
import org.xbib.net.template.expression.ExpressionType;
import org.xbib.net.template.expression.TemplateExpression;
import org.xbib.net.template.expression.URITemplateExpression;
import org.xbib.net.template.parse.ExpressionParser;
import org.xbib.net.template.parse.URITemplateParser;
import org.xbib.net.template.parse.VariableSpecParser;
import org.xbib.net.template.vars.Variables;
import org.xbib.net.template.vars.specs.ExplodedVariable;
import org.xbib.net.template.vars.specs.PrefixVariable;
import org.xbib.net.template.vars.specs.SimpleVariable;
import org.xbib.net.template.vars.specs.VariableSpec;
import org.xbib.net.template.vars.specs.VariableSpecType;
import org.xbib.net.template.vars.values.ListValue;
import org.xbib.net.template.vars.values.MapValue;
import org.xbib.net.template.vars.values.NullValue;
import org.xbib.net.template.vars.values.ScalarValue;
import org.xbib.net.template.vars.values.VariableValue;

import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class URITemplateTest {

    @Test
    void simpleTest() {
        String[] strings = new String[]{
                "foo", "%33foo", "foo%20", "foo_%20bar", "FoOb%02ZAZE287", "foo.bar", "foo_%20bar.baz%af.r"
        };
        for (String s : strings) {
            CharBuffer buffer = CharBuffer.wrap(s).asReadOnlyBuffer();
            VariableSpec varspec = VariableSpecParser.parse(buffer);
            assertEquals(varspec.getName(), s);
            assertSame(varspec.getType(), VariableSpecType.SIMPLE);
            assertFalse(buffer.hasRemaining());
        }
    }

    @Test
    void invalidTest() {
        String[] strings = new String[]{"", "%", "foo..bar", ".", "foo%ra", "foo%ar"};
        for (String s : strings) {
            try {
                CharBuffer buffer = CharBuffer.wrap(s).asReadOnlyBuffer();
                VariableSpecParser.parse(buffer);
                fail("No exception thrown");
            } catch (Exception ex) {
                assertTrue(ex instanceof IllegalArgumentException);
            }
        }
    }

    @Test
    void literalTest() {
        Variables vars = Variables.builder().build();
        String[] strings = new String[]{"foo", "%23foo", "%23foo%24", "foo%24", "f%c4oo", "http://slashdot.org",
                "x?y=e", "urn:d:ze:/oize#/e/e", "ftp://ftp.foo.com/ee/z?a=b#e/dz",
                "http://z.t/hello%20world"};
        for (String s : strings) {
            CharBuffer buffer = CharBuffer.wrap(s).asReadOnlyBuffer();
            List<URITemplateExpression> list = URITemplateParser.parse(buffer);
            assertEquals(list.get(0).expand(vars), s);
            assertFalse(buffer.hasRemaining());
        }
    }

    @Test
    void parsingEmptyInputGivesEmptyList() {
        CharBuffer buffer = CharBuffer.wrap("").asReadOnlyBuffer();
        List<URITemplateExpression> list = URITemplateParser.parse(buffer);
        assertTrue(list.isEmpty());
        assertFalse(buffer.hasRemaining());
    }

    @Test
    @SuppressWarnings("unchecked")
    void parseExpressions() {
        List<Object[]> list = new ArrayList<>();
        String input;
        ExpressionType type;
        List<VariableSpec> varspecs;

        input = "{foo}";
        type = ExpressionType.SIMPLE;
        varspecs = Collections.singletonList(new SimpleVariable("foo"));
        list.add(new Object[]{input, type, varspecs});

        input = "{foo,bar}";
        type = ExpressionType.SIMPLE;
        varspecs = Arrays.asList(new SimpleVariable("foo"), new SimpleVariable("bar"));
        list.add(new Object[]{input, type, varspecs});

        input = "{+foo}";
        type = ExpressionType.RESERVED;
        varspecs = Collections.singletonList(new SimpleVariable("foo"));
        list.add(new Object[]{input, type, varspecs});

        input = "{.foo:10,bar*}";
        type = ExpressionType.NAME_LABELS;
        varspecs = Arrays.asList(new PrefixVariable("foo", 10), new ExplodedVariable("bar"));
        list.add(new Object[]{input, type, varspecs});

        for (Object[] o : list) {
            CharBuffer buffer = CharBuffer.wrap((CharSequence) o[0]).asReadOnlyBuffer();
            URITemplateExpression actual = new ExpressionParser().parse(buffer);
            assertFalse(buffer.hasRemaining());
            URITemplateExpression expected = new TemplateExpression((ExpressionType) o[1], (List<VariableSpec>) o[2]);
            assertEquals(actual, expected);
        }
    }

    @Test
    void parseInvalidExpressions() {
        try {
            CharBuffer buffer = CharBuffer.wrap("{foo").asReadOnlyBuffer();
            new ExpressionParser().parse(buffer);
            fail("No exception thrown");
        } catch (Exception ex) {
            assertTrue(ex instanceof IllegalArgumentException);
        }
        try {
            CharBuffer buffer = CharBuffer.wrap("{foo#bar}").asReadOnlyBuffer();
            new ExpressionParser().parse(buffer);
            fail("No exception thrown");
        } catch (Exception ex) {
            assertTrue(ex instanceof IllegalArgumentException);
        }
    }

    @Test
    void parsePrefixes() {
        String[] strings = new String[]{"foo:323", "%33foo:323", "foo%20:323", "foo_%20bar:323", "FoOb%02ZAZE287:323",
                "foo.bar:323", "foo_%20bar.baz%af.r:323"};
        for (String s : strings) {
            CharBuffer buffer = CharBuffer.wrap(s).asReadOnlyBuffer();
            VariableSpec varspec = VariableSpecParser.parse(buffer);
            assertEquals(varspec.getName(), s.substring(0, s.indexOf(':')));
            assertSame(varspec.getType(), VariableSpecType.PREFIX);
            assertFalse(buffer.hasRemaining());
        }
    }

    @Test
    void parseInvalidPrefixes() {
        String[] strings = new String[]{"foo:", "foo:-1", "foo:a", "foo:10001", "foo:2147483648"};
        for (String s : strings) {
            try {
                VariableSpecParser.parse(CharBuffer.wrap(s).asReadOnlyBuffer());
                fail("No exception thrown!!");
            } catch (Exception ex) {
                assertTrue(ex instanceof IllegalArgumentException);
            }
        }
    }

    @Test
    void parseExploded() {
        String[] strings = new String[]{"foo*", "%33foo*", "foo%20*", "foo_%20bar*", "FoOb%02ZAZE287*", "foo.bar*",
                "foo_%20bar.baz%af.r*"};
        for (String s : strings) {
            CharBuffer buffer = CharBuffer.wrap(s).asReadOnlyBuffer();
            VariableSpec varspec = VariableSpecParser.parse(buffer);
            assertEquals(varspec.getName(), s.substring(0, s.length() - 1));
            assertSame(varspec.getType(), VariableSpecType.EXPLODED);
            assertFalse(buffer.hasRemaining());
        }
    }

    @Test
    void parseExceptions() {
        String[] strings = new String[]{"foo%", "foo%r", "foo%ra", "foo%ar", "foo<", "foo{"};
        for (String s : strings) {
            try {
                URITemplateParser.parse(s);
                fail("No exception thrown!!");
            } catch (Exception ex) {
                assertTrue(ex instanceof IllegalArgumentException);
            }
        }
    }

    @Test
    void testExamples() throws Exception {
        JsonNode data = fromResource("/spec-examples.json");
        List<Map<String, Object>> list = new ArrayList<>();
        for (JsonNode node : data) {
            Variables.Builder builder = Variables.builder();
            Iterator<Map.Entry<String, JsonNode>> it = node.get("variables").fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> entry = it.next();
                builder.add(entry.getKey(), fromJson(entry.getValue()));
            }
            for (JsonNode n : node.get("testcases")) {
                Map<String, Object> m = new HashMap<>();
                m.put("tmpl", n.get(0).textValue());
                m.put("vars", builder.build());
                m.put("resultNode", n.get(1));
                list.add(m);
            }
        }
        for (Map<String, Object> e : list) {
            URITemplate template = new URITemplate((String) e.get("tmpl"));
            String actual = template.toString((Variables) e.get("vars"));
            JsonNode resultNode = (JsonNode) e.get("resultNode");
            if (resultNode.isTextual()) {
                assertEquals(resultNode.textValue(), actual);
            } else {
                if (!resultNode.isArray()) {
                    throw new IllegalArgumentException("didn't expect that");
                }
                boolean found = false;
                for (JsonNode node : resultNode) {
                    if (node.textValue().equals(actual)) {
                        found = true;
                    }
                }
                assertTrue(found);
            }
        }
    }

    @Test
    void testExamplesBySection() throws Exception {
        JsonNode data = fromResource("/spec-examples-by-section.json");
        List<Map<String, Object>> list = new ArrayList<>();
        for (JsonNode node : data) {
            Variables.Builder builder = Variables.builder();
            Iterator<Map.Entry<String, JsonNode>> it = node.get("variables").fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> entry = it.next();
                builder.add(entry.getKey(), fromJson(entry.getValue()));
            }
            for (JsonNode n : node.get("testcases")) {
                Map<String, Object> m = new HashMap<>();
                m.put("tmpl", n.get(0).textValue());
                m.put("vars", builder.build());
                m.put("resultNode", n.get(1));
                list.add(m);
            }
        }
        for (Map<String, Object> e : list) {
            URITemplate template = new URITemplate((String) e.get("tmpl"));
            String actual = template.toString((Variables) e.get("vars"));
            JsonNode resultNode = (JsonNode) e.get("resultNode");
            if (resultNode.isTextual()) {
                assertEquals(resultNode.textValue(), actual);
            } else {
                if (!resultNode.isArray()) {
                    throw new IllegalArgumentException("didn't expect that");
                }
                boolean found = false;
                for (JsonNode node : resultNode) {
                    if (node.textValue().equals(actual)) {
                        found = true;
                    }
                }
                assertTrue(found);
            }
        }
    }

    @Test
    void extendedTests() throws Exception {
        JsonNode data = fromResource("/extended-tests.json");
        List<Map<String, Object>> list = new ArrayList<>();
        for (JsonNode node : data) {
            Variables.Builder builder = Variables.builder();
            Iterator<Map.Entry<String, JsonNode>> it = node.get("variables").fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> entry = it.next();
                builder.add(entry.getKey(), fromJson(entry.getValue()));
            }
            for (JsonNode n : node.get("testcases")) {
                Map<String, Object> m = new HashMap<>();
                m.put("tmpl", n.get(0).textValue());
                m.put("vars", builder.build());
                m.put("resultNode", n.get(1));
                list.add(m);
            }
        }
        for (Map<String, Object> e : list) {
            URITemplate template = new URITemplate((String) e.get("tmpl"));
            String actual = template.toString((Variables) e.get("vars"));
            JsonNode resultNode = (JsonNode) e.get("resultNode");
            if (resultNode.isTextual()) {
                assertEquals(resultNode.textValue(), actual);
            } else {
                if (!resultNode.isArray()) {
                    throw new IllegalArgumentException("didn't expect that");
                }
                boolean found = false;
                for (JsonNode node : resultNode) {
                    if (node.textValue().equals(actual)) {
                        found = true;
                    }
                }
                assertTrue(found);
            }
        }
    }

    @Test
    void negativeTests() throws Exception {
        JsonNode data = fromResource("/negative-tests.json");
        JsonNode node =  data.get("Failure Tests").get("variables");
        Variables.Builder builder = Variables.builder();
        Iterator<Map.Entry<String, JsonNode>> it = node.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> entry = it.next();
            builder.add(entry.getKey(), fromJson(entry.getValue()));
        }
        List<Map<String, Object>> list = new ArrayList<>();
        for (JsonNode n : data.get("Failure Tests").get("testcases")) {
            Map<String, Object> m = new HashMap<>();
            m.put("tmpl", n.get(0).textValue());
            m.put("vars", builder.build());
            list.add(m);
        }
        for (Map<String, Object> e : list) {
            try {
                new URITemplate((String) e.get("tmpl")).toString((Variables) e.get("vars"));
                fail("no exception thrown");
            } catch (Exception ex) {
                assertTrue(ex instanceof IllegalArgumentException);
            }
        }
    }

    @Test
    void expansionTest() throws Exception {
        String[] strings = new String[]{"/rfcExamples.json", "/strings.json", "/multipleStrings.json",
                "/lists.json", "/multipleLists.json"};
        for (String s : strings) {
            JsonNode data = fromResource(s);
            Variables.Builder builder = Variables.builder();
            Iterator<Map.Entry<String, JsonNode>> it = data.get("vars").fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> entry = it.next();
                if (!entry.getValue().isNull()) {
                    builder.add(entry.getKey(), fromJson(entry.getValue()));
                }
            }
            List<Map<String, Object>> list = new ArrayList<>();
            it = data.get("tests").fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> entry = it.next();
                Map<String, Object> m = new HashMap<>();
                m.put("tmpl", entry.getKey());
                m.put("vars", builder.build());
                m.put("expected", entry.getValue().textValue());
                list.add(m);
            }
            for (Map<String, Object> e : list) {
                String actual = new URITemplate((String) e.get("tmpl")).toString((Variables) e.get("vars"));
                assertEquals(e.get("expected"), actual);
            }
        }
    }

    private JsonNode fromResource(String path) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ObjectReader reader = mapper.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, true)
                .readerFor(JsonNode.class);
        return reader.readValue(getClass().getResourceAsStream(path));
    }

    private static VariableValue fromJson(JsonNode node) {
        if (node.isTextual()) {
            return new ScalarValue(node.textValue());
        }
        if (node.isArray()) {
            ListValue.Builder builder = ListValue.builder();
            for (JsonNode n : node) {
                builder.add(n.textValue());
            }
            return builder.build();
        }
        if (node.isObject()) {
            MapValue.Builder builder = MapValue.builder();
            Iterator<Map.Entry<String, JsonNode>> it = node.fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> entry = it.next();
                builder.put(entry.getKey(), entry.getValue().textValue());
            }
            return builder.build();
        }
        if (node.isNull()) {
            return new NullValue();
        }
        throw new IllegalArgumentException("cannot bind JSON to variable value: " + node + " class " + node.getClass());
    }
}
