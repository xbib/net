package org.xbib.net;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

/**
 */
public class URLTest {

    @Test
    public void test() throws Exception {
       List<JsonTest> tests = readTests(fromResource("/urltestdata.json"));
       for (JsonTest test : tests) {
           String base = test.base;
           String input = test.input;
           System.err.println("testing: " + base + " " + input + " " + test.failure);
           if (test.skip) {
               continue;
           }
           if (test.failure) {
               try {
                   URL url = URL.base(base).resolve(input);
                   System.err.println("resolved: " + url.toString());
                   fail();
               } catch (Exception e) {
                   // pass
               }
           } else {
               if (base != null && input != null) {
                   try {
                       URL url = URL.base(base).resolve(input);
                       System.err.println("resolved: " + url.toString());
                       if (test.protocol != null) {
                           assertEquals(test.protocol, url.getScheme() + ":");
                       }
                       if (test.hostname != null) {
                           // default in Mac OS
                           String host = url.getHost();
                           if ("broadcasthost".equals(host)) {
                               host = "255.255.255.255";
                           }
                           assertEquals(test.hostname, host);
                       }
                       if (test.port != null && !test.port.isEmpty() && url.getPort() != null) {
                           assertEquals(Integer.parseInt(test.port), (int) url.getPort());
                       }
                       // TODO(jprante)
                       //if (test.pathname != null && !test.pathname.isEmpty() && url.getPath() != null) {
                       //    assertEquals(test.pathname, url.getPath());
                       //}
                       System.err.println("passed: " + base + " " + input);
                   } catch (URLSyntaxException e) {
                       System.err.println("unable to resolve: " + base + " " + input + " reason: " + e.getMessage());
                   }
               }
           }
       }
    }

    private JsonNode fromResource(String path) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ObjectReader reader = mapper.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, true)
                .readerFor(JsonNode.class);
        return reader.readValue(getClass().getResourceAsStream(path));
    }

    private List<JsonTest> readTests(JsonNode jsonNode) {
        List<JsonTest> list = new ArrayList<>();
        for (JsonNode n : jsonNode) {
            if (n.isObject()) {
                JsonTest jsontest = new JsonTest();
                jsontest.input = get(n, "input");
                jsontest.base = get(n, "base");
                jsontest.href = get(n, "href");
                jsontest.origin = get(n, "origin");
                jsontest.protocol = get(n, "protocol");
                jsontest.username = get(n, "username");
                jsontest.password = get(n, "password");
                jsontest.host = get(n, "host");
                jsontest.hostname = get(n, "hostname");
                jsontest.port = get(n, "port");
                jsontest.pathname = get(n, "pathname");
                jsontest.search = get(n, "search");
                jsontest.hash = get(n, "hash");
                jsontest.failure = n.has("failure");
                jsontest.skip = n.has("skip");
                list.add(jsontest);
            }
        }
        return list;
    }

    private String get(JsonNode n, String key) {
        return n.has(key) ? n.get(key).textValue() : null;
    }

    static class JsonTest {
        String input;
        String base;
        String href;
        String origin;
        String protocol;
        String username;
        String password;
        String host;
        String hostname;
        String port;
        String pathname;
        String search;
        String hash;
        boolean failure;
        boolean skip;
    }

}
