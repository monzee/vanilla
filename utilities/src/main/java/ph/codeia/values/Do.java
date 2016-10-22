package ph.codeia.values;

/**
 * This file is a part of the vanilla project.
 */

import ph.codeia.run.Runner;

/**
 * A bunch of single-method interfaces for "functional" programming.
 */
public interface Do {

    /**
     * Represents the result of a computation.
     *
     * Similar to a {@link java.util.concurrent.Future} but with a unified
     * checked type and not cancellable.
     *
     * @param <T> The type of the value
     */
    interface Try<T> {
        T get() throws Throwable;
    }

    /**
     * Produces a value.
     *
     * Like a {@link Try} or {@link java.util.concurrent.Callable} that
     * never throws.
     *
     * @param <T> The type of the value
     */
    interface Make<T> extends Try<T> {
        T get();
    }

    /**
     * Derives a value from some given value. Checked.
     *
     * @param <T> The type of the given value.
     * @param <U> The type of the derived value.
     */
    interface Map<T, U> {
        U from(T value) throws Throwable;
    }

    /**
     * Unchecked version of {@link Map}.
     */
    interface Convert<T, U> extends Map<T, U> {
        U from(T value);
    }

    /**
     * Binary version of {@link Convert}.
     */
    interface Combine2<T, A, B> {
        T from(A first, B second);
    }

    /**
     * Ternary version of {@link Convert}.
     */
    interface Combine3<T, A, B, C> {
        T from(A first, B second, C third);
    }

    /**
     * 4-ary version of {@link Convert}.
     */
    interface Combine4<T, A, B, C, D> {
        T from(A first, B second, C third, D fourth);
    }

    /**
     * An action that consumes value.
     *
     * @param <T> The type of the value.
     */
    interface Just<T> {
        void got(T value);
    }

    /**
     * An alias to {@link Just Just<Try<T>>}.
     */
    interface Maybe<T> extends Just<Try<T>> {}

    /**
     * An action that executes a continuation.
     *
     * @param <T> The type of the value expected by the continuation.
     */
    interface Execute<T> {
        void begin(Just<T> next);
    }

    /**
     * An action that computes a value and sends it to a continuation.
     *
     * @param <T> The type of the given value
     * @param <U> The type of the derived value and the type expected by
     *            the continuation.
     */
    interface Continue<T, U> {
        void then(T value, Just<U> next);
    }

}
