package ph.codeia.run;

import ph.codeia.values.Do;

/**
 * This file is a part of the vanilla project.
 */

/**
 * Provides context to blocks. Doesn't actually run them, that's
 * {@link Seq#begin() Seq's} job.
 *
 * This interface is used mainly to run code in a specific thread. It is
 * possible to run the producing code and the consuming code in separate
 * threads even in the same block, like in {@link Interleave}. It is also
 * possible to memoize the values produced by a block. There might be other
 * applications I haven't thought of yet. Maybe a periodic runner, although
 * I can't imagine yet how a loop can be stopped.
 */
public interface Runner {

    /**
     * Decorates a {@link Do.Execute} block
     */
    <T> Do.Execute<T> apply(Do.Execute<T> block);

    /**
     * Decorates a {@link Do.Continue} block
     */
    <T, U> Do.Continue<T, U> apply(Do.Continue<T, U> block);

    /**
     * Decorates a {@link Do.Just} block.
     *
     * Changed the name to differentiate from {@link #apply(Do.Execute)} so
     * that lambdas don't have to be explicitly cast.
     */
    <T> Do.Just<T> run(Do.Just<T> block);

}
