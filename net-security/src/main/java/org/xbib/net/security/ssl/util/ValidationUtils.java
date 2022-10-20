package org.xbib.net.security.ssl.util;

import java.util.List;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public final class ValidationUtils {

    public static final UnaryOperator<String> GENERIC_EXCEPTION_MESSAGE = objectType -> String.format("No valid %s has been provided. %s must be present, but was absent.", objectType, objectType);

    private ValidationUtils() {
    }

    public static <T> T requireNotNull(T maybeNull, String message) {
        return requireNotNull(maybeNull, () -> new IllegalArgumentException(message));
    }

    public static <T> T requireNotNull(T maybeNull, Supplier<RuntimeException> exceptionSupplier) {
        if (maybeNull == null) {
            throw exceptionSupplier.get();
        }
        return maybeNull;
    }

    public static <T> List<T> requireNotEmpty(List<T> maybeNull, String message) {
        return requireNotEmpty(maybeNull, () -> new IllegalArgumentException(message));
    }

    public static <T> List<T> requireNotEmpty(List<T> maybeNull, Supplier<RuntimeException> exceptionSupplier) {
        if (maybeNull == null || maybeNull.isEmpty()) {
            throw exceptionSupplier.get();
        }
        return maybeNull;
    }

    public static String requireNotBlank(String maybeNull, String message) {
        return requireNotBlank(maybeNull, () -> new IllegalArgumentException(message));
    }

    public static String requireNotBlank(String maybeNull, Supplier<RuntimeException> exceptionSupplier) {
        if (StringUtils.isBlank(maybeNull)) {
            throw exceptionSupplier.get();
        }
        return maybeNull;
    }
}
