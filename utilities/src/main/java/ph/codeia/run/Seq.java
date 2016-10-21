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
public class Seq<T, U> implements Do.Executable<U> {

    private static final Do.Executable<Void> NIL = next -> next.got(null);

    private static final Do.Just NOOP = ignored -> {};

    /**
     * Starts a computation.
     *
     * @param block An action that takes a continuation.
     */
    public static <T> Seq<?, T> of(Do.Executable<T> block) {
        return new Seq<>(NIL, (ignored, next) -> block.start(next));
    }

    private final Do.Executable<T> prev;
    private final Do.Continue<T, U> step;

    private Seq(Do.Executable<T> prev, Do.Continue<T, U> step) {
        this.prev = prev;
        this.step = step;
    }

    /**
     * Continues a computation.
     *
     * @param next The next step in the computation.
     * @param <V> The type of the value produced by the next step.
     */
    public <V> Seq<U, V> andThen(Do.Continue<U, V> next) {
        return new Seq<>(this, next);
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
    public void start(Do.Just<U> next) {
        prev.start(value -> step.then(value, next));
    }

    /**
     * Executes a computation and ignores the final value produced.
     */
    public void start() {
        start(NOOP);
    }

}
