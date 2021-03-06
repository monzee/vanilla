package ph.codeia.arch.sm;

import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;

import ph.codeia.meta.Untested;

/**
 * This file is a part of the vanilla project.
 */

@Untested
@SuppressWarnings("unchecked")
public abstract class RootState<
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

    public S apply(Runnable action) {
        action.run();
        return (S) this;
    }

    protected <T extends RootState<S, A>> T join(T instance) {
        instance.futures = futures;
        instance.backlog = backlog;
        return instance;
    }
}
