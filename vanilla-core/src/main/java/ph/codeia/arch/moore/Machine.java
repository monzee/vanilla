package ph.codeia.arch.moore;

/*
 * This file is a part of the vanilla project.
 */

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

public class Machine<
        A extends Msm.Action<A, E>,
        E extends Msm.Effect<A, E>>
implements Msm.Machine<A, E> {

    public interface ExtendedAdapter<A extends Msm.Action<A, ?>> extends Adapter {
        boolean willMoveTo(A state);
        void didExec(A state);
    }

    public interface Adapter {
        void runOnUiThread(Runnable block);
        void handle(Throwable e);
    }

    private static class Join<A extends Msm.Action<A, ?>> {
        final A producer;
        final Future<?> join;

        private Join(A producer, Future<?> join) {
            this.producer = producer;
            this.join = join;
        }

        A cancel() {
            join.cancel(true);
            return producer;
        }
    }

    private final ExecutorService junction;
    private final E output;
    private final ExtendedAdapter<A> hook;
    private final Queue<Join<A>> inFlight = new ArrayDeque<>();
    private A state;

    public Machine(ExecutorService junction, E output, final Adapter hook) {
        this(junction, output, new ExtendedAdapter<A>() {
            @Override
            public boolean willMoveTo(A state) {
                return true;
            }

            @Override
            public void didExec(A state) {

            }

            @Override
            public void runOnUiThread(Runnable block) {
                hook.runOnUiThread(block);
            }

            @Override
            public void handle(Throwable e) {
                hook.handle(e);
            }
        });
    }

    public Machine(ExecutorService junction, E output, ExtendedAdapter<A> hook) {
        this.junction = junction;
        this.output = output;
        this.hook = hook;
    }

    @Override
    public boolean moveTo(A newState) {
        boolean ok = hook.willMoveTo(newState);
        if (ok) {
            state = newState;
        }
        return ok;
    }

    @Override
    public void exec(final A newState) {
        hook.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (moveTo(newState)) {
                        newState.apply(output);
                        hook.didExec(newState);
                    }
                } catch (Exception e) {
                    hook.handle(e);
                }
            }
        });
    }

    @Override
    public void await(final Future<A> task) {
        final AtomicReference<Join<A>> join = new AtomicReference<>();
        join.set(new Join<>(state, junction.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    exec(task.get());
                } catch (ExecutionException e) {
                    hook.handle(e);
                } catch (InterruptedException ignored) {
                } finally {
                    inFlight.remove(join.get());
                }
            }
        })));
        inFlight.add(join.get());
    }

    @Override
    public Iterable<A> stop() {
        final Queue<A> backlog = new ArrayDeque<>();
        for (Join<A> join : inFlight) {
            backlog.add(join.cancel());
        }
        backlog.add(state);
        inFlight.clear();
        return backlog;
    }

    @Override
    public A peek() {
        return state;
    }

}
