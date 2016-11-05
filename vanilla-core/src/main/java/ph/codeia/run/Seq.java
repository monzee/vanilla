package ph.codeia.run;

import ph.codeia.values.Do;

/**
 * This file is a part of the vanilla project.
 */

/**
 * A step in a CPS sequence. May be executed now or passed to another step.
 *
 * @param <T> The type of the result of the previous step
 * @param <U> The type of the value computed by this step and to be sent
 *            to the next step.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class Seq<T, U> implements Do.Execute<U> {

    private static final Do.Execute<Void> NIL = new Do.Execute<Void>() {
        @Override
        public void begin(Do.Just<Void> next) {
            next.got(null);
        }
    };

    private static final Do.Just NOOP = new Do.Just() {
        @Override
        public void got(Object ignored) {}
    };

    /**
     * Starts a computation.
     *
     * @param block An action that takes a continuation.
     * @param <T> The type of the value expected by the continuation.
     * @return An executable, extendable sequence.
     */
    public static <T> Seq<?, T> of(final Do.Execute<T> block) {
        return new Seq<>(NIL, new Do.Continue<Void, T>() {
            @Override
            public void then(Void ignored, Do.Just<T> next) {
                block.begin(next);
            }
        });
    }

    /**
     * Shorthand to eliminate the "Do.Try" from the signature.
     *
     * Because this type is used often and the type param is often required
     * because it can't be inferred when the seq is not assigned to a variable
     * (the common use case). {@code Seq.<String>tryOf(...)} is a lot easier
     * to read than {@code Seq.<Do.Try<String>>tryOf(...)}.
     *
     * @param block An action that takes a wrapped value or error.
     * @param <T> The type of the wrapped value.
     * @return An executable, extendable sequence.
     */
    public static <T> Seq<?, Do.Try<T>> tryOf(Do.Execute<Do.Try<T>> block) {
        return of(block);
    }

    private final Do.Execute<T> prev;
    private final Do.Continue<T, U> step;

    private Seq(Do.Execute<T> prev, Do.Continue<T, U> step) {
        this.prev = prev;
        this.step = step;
    }

    /**
     * Continues a computation.
     *
     * @param next The next step in the computation.
     * @param <V> The type of the value produced by the next step.
     * @return An executable, extendable sequence.
     */
    public <V> Seq<U, V> pipe(Do.Continue<U, V> next) {
        return new Seq<>(this, next);
    }

    /**
     * Shorthand to eliminate the "Do.Try" from the signature.
     *
     * Same rationale as {@link #tryOf(Do.Execute)}.
     *
     * @param next A continuation that takes a wrapped value or error.
     * @param <V> The type of the wrapped value.
     * @return An executable, extendable sequence.
     */
    public <V> Seq<U, Do.Try<V>> tryPipe(Do.Continue<U, Do.Try<V>> next) {
        return pipe(next);
    }

    /**
     * Executes a computation.
     *
     * Multiple calls redo the computation. No caching is done here but
     * each step may do their own caching.
     *
     * @param next The last step in the computation.
     */
    @Override
    public void begin(final Do.Just<U> next) {
        prev.begin(new Do.Just<T>() {
            @Override
            public void got(T value) {
                step.then(value, next);
            }
        });
    }

    /**
     * Executes a computation and ignores the final value produced.
     */
    public void begin() {
        begin(NOOP);
    }

}
