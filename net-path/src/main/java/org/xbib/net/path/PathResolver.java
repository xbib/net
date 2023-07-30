package org.xbib.net.path;

import org.xbib.net.Parameter;
import org.xbib.net.ParameterException;

public interface PathResolver<T> {

    void resolve(String method, String path, ResultListener<T> listener) throws ParameterException;

    interface Builder<T> {

        Builder<T> add(String method, String path, T value);

        PathResolver<T> build();
    }

    interface Result<T> {

        T getValue();

        Parameter getParameter();

        String getMethod();
    }

    @FunctionalInterface
    interface ResultListener<T> {

        void onResult(Result<T> result) throws ParameterException;

    }
}
