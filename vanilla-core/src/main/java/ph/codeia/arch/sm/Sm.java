package ph.codeia.arch.sm;

import java.util.concurrent.Future;

/**
 * This file is a part of the vanilla project.
 */

public interface Sm {

    interface State<
            S extends State<S, A>,
            A extends Action<S, A, ?>>
    extends Iterable<Future<A>> {
        S async(Future<A> futureAction);
        Backlog backlog();
    }

    interface Action<
            S extends State<S, A>,
            A extends Action<S, A, C>,
            C> {
        S fold(S from, C client);
    }

}
