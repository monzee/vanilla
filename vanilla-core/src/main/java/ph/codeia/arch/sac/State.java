package ph.codeia.arch.sac;

import java.util.concurrent.Future;

/**
 * This file is a part of the vanilla project.
 */

public interface State<
        S extends State<S, A>,
        A extends Action<S, A, ?>>
extends Iterable<Future<A>> {
    S async(Future<A> futureAction);
    Backlog backlog();
}
