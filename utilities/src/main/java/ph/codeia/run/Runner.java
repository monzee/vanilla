package ph.codeia.run;

import ph.codeia.values.Do;

/**
 * This file is a part of the vanilla project.
 */

/**
 * Provides context to blocks that produce and consume values.
 *
 * This interface is used mainly to run code in a specific thread. It is
 * possible to run the producing code from the consuming code in separate
 * threads even in the same block, like in {@link Interleave}. It is also
 * possible to memoize the values produced by a block. There might be other
 * applications I haven't thought of yet. Maybe a periodic runner, although
 * I can't imagine yet how a loop can be stopped.
 */
public interface Runner {

    <T> Do.Execute<T> run(Do.Execute<T> block);

    <T, U> Do.Continue<T, U> run(Do.Continue<T, U> block);

    <T> Do.Just<T> run(Do.Just<T> block);

}
