package ph.codeia.arch.sm;

import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;

/**
 * This file is a part of the vanilla project.
 */

public abstract class BaseState<
        S extends Sm.State<S, A>,
        A extends Sm.Action<S, A, ?>>
implements Sm.State<S, A> {

    protected transient Queue<Future<A>> futures = new ConcurrentLinkedQueue<>();
    protected transient Backlog backlog = new Backlog();

    @Override
    public Iterator<Future<A>> iterator() {
        return futures.iterator();
    }

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
        return async(Machine.future(action));
    }

    public S plus(A action) {
        return async(Machine.now(action));
    }

    @SuppressWarnings("unchecked")
    public S apply(Runnable action) {
        action.run();
        return (S) this;
    }

    @SuppressWarnings("unchecked")
    protected S join(BaseState<S, A> instance) {
        instance.futures = futures;
        instance.backlog = backlog;
        return (S) instance;
    }
}
