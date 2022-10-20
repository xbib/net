package org.xbib.net.security.ssl.util;

import org.xbib.net.security.ssl.exception.GenericIOException;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.stream.Collectors;

public final class IOUtils {

    private IOUtils() {}

    static String getContent(InputStream inputStream) {
        try (InputStreamReader inputStreamReader = new InputStreamReader(ValidationUtils.requireNotNull(inputStream,
                ValidationUtils.GENERIC_EXCEPTION_MESSAGE.apply("InputStream")), StandardCharsets.UTF_8);
             BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {

            return bufferedReader.lines()
                    .collect(Collectors.joining(System.lineSeparator()));
        } catch (Exception e) {
            throw new GenericIOException(e);
        }
    }

    static byte[] copyToByteArray(InputStream inputStream) {
        try(ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > -1) {
                outputStream.write(buffer, 0, length);
            }
            outputStream.flush();
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new GenericIOException(e);
        }
    }

    static void closeSilently(AutoCloseable autoCloseable) {
        try {
            autoCloseable.close();
        } catch (Exception ignored) {
            //ignore exception
        }
    }

    static InputStream getResourceAsStream(String name) {
        return IOUtils.class.getClassLoader().getResourceAsStream(name);
    }

    static InputStream getFileAsStream(Path path) {
        try {
            return Files.newInputStream(path, StandardOpenOption.READ);
        } catch (IOException e) {
            throw new GenericIOException(e);
        }
    }

}
