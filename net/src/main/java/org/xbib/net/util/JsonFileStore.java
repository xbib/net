package org.xbib.net.util;

import org.xbib.net.Store;
import org.xbib.net.PercentEncoder;
import org.xbib.net.PercentEncoders;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class JsonFileStore implements Store<Map<String, Object>> {

    private static final Logger logger = Logger.getLogger(JsonFileStore.class.getName());

    private final String name;

    private final ReentrantReadWriteLock lock;

    private final Path path;

    public JsonFileStore(String name, Path path) throws IOException {
        this.name = name;
        this.path = path;
        this.lock = new ReentrantReadWriteLock();
        Files.createDirectories(path);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public long size() throws IOException {
        AtomicLong size = new AtomicLong(0L);
        try (Stream<Path> stream = Files.walk(path)) {
            stream.forEach(p -> {
                if (Files.isRegularFile(p) && Files.isReadable(p)) {
                    size.incrementAndGet();
                }
            });
            return size.get();
        }
    }

    @Override
    public void readAll(String prefix, Consumer<Map<String, Object>> consumer) throws IOException {
        Objects.requireNonNull(consumer);
        try (Stream<Path> stream = Files.walk(path.resolve(prefix))) {
            stream.forEach(p -> {
                try {
                    consumer.accept(read(p.getFileName().toString()));
                } catch (IOException e) {
                    logger.log(Level.WARNING, "i/o error while consuming: " + e.getMessage(), e);
                }
            });
        }
    }

    @Override
    public Map<String, Object> read(String key) throws IOException {
        ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
        try {
            readLock.lock();
            PercentEncoder percentEncoder = PercentEncoders.getUnreservedEncoder(StandardCharsets.UTF_8);
            Path p = path.resolve(percentEncoder.encode(key));
            if (Files.isRegularFile(p) && Files.isReadable(p)) {
                return JsonUtil.toMap(Files.readString(p));
            } else {
                return null;
            }
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public void write(String key, Map<String, Object> map) throws IOException {
        ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();
        try {
            writeLock.lock();
            PercentEncoder percentEncoder = PercentEncoders.getUnreservedEncoder(StandardCharsets.UTF_8);
            Path p = path.resolve(percentEncoder.encode(key));
            if (!Files.exists(p.getParent())) {
                Files.createDirectories(p.getParent());
            }
            try (Writer writer = Files.newBufferedWriter(p, StandardOpenOption.CREATE_NEW)) {
                writer.write(JsonUtil.toString(map));
            }
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void write(String prefix, String key, Map<String, Object> map) throws IOException {
        ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();
        try {
            writeLock.lock();
            PercentEncoder percentEncoder = PercentEncoders.getUnreservedEncoder(StandardCharsets.UTF_8);
            Path p = path.resolve(prefix).resolve(percentEncoder.encode(key));
            if (!Files.exists(p.getParent())) {
                Files.createDirectories(p.getParent());
            }
            try (Writer writer = Files.newBufferedWriter(p, StandardOpenOption.CREATE_NEW)) {
                writer.write(JsonUtil.toString(map));
            }
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void update(String key, Map<String, Object> map) throws IOException {
        ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();
        try {
            writeLock.lock();
            PercentEncoder percentEncoder = PercentEncoders.getUnreservedEncoder(StandardCharsets.UTF_8);
            Path p = path.resolve(percentEncoder.encode(key));
            if (!Files.exists(p.getParent())) {
                Files.createDirectories(p.getParent());
            }
            try (Writer writer = Files.newBufferedWriter(p, StandardOpenOption.CREATE)) {
                writer.write(JsonUtil.toString(map));
            }
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void update(String prefix, String key, Map<String, Object> map) throws IOException {
        ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();
        try {
            writeLock.lock();
            PercentEncoder percentEncoder = PercentEncoders.getUnreservedEncoder(StandardCharsets.UTF_8);
            Path p = path.resolve(prefix).resolve(percentEncoder.encode(key));
            if (!Files.exists(p.getParent())) {
                Files.createDirectories(p.getParent());
            }
            try (Writer writer = Files.newBufferedWriter(p, StandardOpenOption.CREATE)) {
                writer.write(JsonUtil.toString(map));
            }
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void remove(String key) throws IOException {
        ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();
        try {
            writeLock.lock();
            PercentEncoder percentEncoder = PercentEncoders.getUnreservedEncoder(StandardCharsets.UTF_8);
            Files.deleteIfExists(path.resolve(percentEncoder.encode(key)));
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void purge(long expiredAfterSeconds) throws IOException {
        if (path != null && expiredAfterSeconds > 0L) {
            Instant instant = Instant.now();
            try (Stream<Path> stream = Files.walk(path)) {
                stream.forEach(p -> {
                    try {
                        Duration duration = Duration.between(Files.getLastModifiedTime(p).toInstant(), instant);
                        if (duration.toSeconds() > expiredAfterSeconds) {
                            Files.delete(p);
                        }
                    } catch (IOException e) {
                        logger.log(Level.WARNING, "i/o error while purge: " + e.getMessage(), e);
                    }
                });
            }
        }
    }

    @Override
    public void destroy() throws IOException {
        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
