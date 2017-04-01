package ph.codeia.arch.sav;

import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;

/**
 * This file is a part of the vanilla project.
 */

public abstract class BaseState<
        S extends BaseState<S, A>,
        A extends Action<S, A, ?>>
implements State<S, A> {

    protected Queue<Future<A>> futures = new ConcurrentLinkedQueue<>();
    protected Backlog backlog = new Backlog();

    @Override
    public Backlog backlog() {
        return backlog;
    }

    @SuppressWarnings("unchecked")
    @Override
    public S async(Future<A> futureAction) {
        futures.add(futureAction);
        return (S) this;
    }

    public S async(Callable<A> action) {
        return async(Unit.future(action));
    }

    public S plus(A action) {
        return async(Unit.now(action));
    }

    @SuppressWarnings("unchecked")
    public S apply(Runnable action) {
        action.run();
        return (S) this;
    }

    protected S join(S instance) {
        instance.futures = futures;
        instance.backlog = backlog;
        return instance;
    }
}
