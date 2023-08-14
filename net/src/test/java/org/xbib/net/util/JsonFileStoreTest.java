package org.xbib.net.util;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JsonFileStoreTest {

    private static final Logger logger = Logger.getLogger(JsonFileStoreTest.class.getName());

    private static JsonFileStore jsonFileStore;

    @BeforeAll
    public static void before() throws IOException {
        Path path = Files.createTempDirectory("jsonstore");
        jsonFileStore = new JsonFileStore("test", path, 3600L);
    }

    @AfterAll
    public static void after() throws IOException {
        if (jsonFileStore != null) {
            jsonFileStore.destroy();
        }
    }

    @Test
    public void testJsonFileSTore() throws IOException {
        singleReadWriteCycle(jsonFileStore, "1", Map.of("a", "b"));
    }

    @Test
    public void consumeJsonFileStore() throws IOException {
        for (int i =0; i < 100; i++) {
            String key = RandomUtil.randomString(32);
            singleReadWriteCycle(jsonFileStore, key, Map.of("a", "b"));
        }
        jsonFileStore.readAll("", map -> logger.log(Level.INFO, "map = " + map));
    }

    @Test
    public void consumeJsonFileStoreByPrefix() throws IOException {
        String prefix = "DE-38";
        for (int i =0; i < 10; i++) {
            String key = RandomUtil.randomString(32);
            jsonFileStore.write(prefix, key, Map.of("a", "b"));
        }
        for (int i =0; i < 10; i++) {
            String key = RandomUtil.randomString(32);
            jsonFileStore.write("", key, Map.of("a", "b"));
        }
        jsonFileStore.readAll(prefix, map -> logger.log(Level.INFO, "map = " + map));
    }

    private void singleReadWriteCycle(JsonFileStore jsonFileStore, String key, Map<String, Object> map) throws IOException {
        jsonFileStore.write(key, map);
        Map<String, Object> readedMap = jsonFileStore.read(key);
        assertEquals(map, readedMap);
    }
}
