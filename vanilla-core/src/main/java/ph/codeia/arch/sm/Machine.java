package ph.codeia.arch.sm;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

import ph.codeia.arch.ErrorHandler;
import ph.codeia.meta.Untested;

/**
 * This file is a part of the vanilla project.
 */

@Untested
public abstract class Machine<
        S extends Sm.State<S, A>,
        A extends Sm.Action<S, A, C>,
        C>
implements ErrorHandler<C> {

    /**
     * Abstract machine factory that can also make a bound machine.
     *
     * Meant to be implemented in a nested class in a concrete {@link Machine}.
     *
     * @param <S> The state type.
     * @param <A> The action type.
     * @param <C> The receiver type.
     */
    public static abstract class Builder<
            S extends Sm.State<S, A>,
            A extends Sm.Action<S, A, C>,
            C> {
        protected final S state;
        protected ErrorHandler<C> handler;

        public Builder(S state) {
            this.state = state;
        }

        public Builder<S, A, C> withErrorHandler(ErrorHandler<C> handler) {
            this.handler = handler;
            return this;
        }

        public abstract Machine<S, A, C> build();

        public Bound<S, A, C> build(C client) {
            return build(IMMEDIATE, client);
        }

        public Bound<S, A, C> build(Executor executor, C client) {
            return new Bound<>(executor, client, build());
        }
    }

    /**
     * Partially applies the start and apply methods of {@link Machine} with a
     * common executor and receiver.
     *
     * You'd probably want to use this most of the time instead of a plain
     * {@link Machine}. You can still get the wrapped machine via {@link
     * Bound#machine} if you need to run something with a different executor
     * or receiver.
     *
     * The receiver instance is immediately wrapped in a weak reference before
     * being assigned to a member so this shouldn't cause the receiver to live
     * longer than it would have if it weren't bound. If your receiver isn't
     * an Android activity or fragment, make sure to have at least one strong
     * reference to the receiver somewhere or the wrapped machine would be
     * passing nulls to actions.
     *
     * @param <S> The state type.
     * @param <A> The action type.
     * @param <C> The receiver type.
     */
    public static class Bound<
            S extends Sm.State<S, A>,
            A extends Sm.Action<S, A, C>,
            C> {
        public final Machine<S, A, C> machine;
        private final WeakReference<C> client;
        private final Executor executor;

        public Bound(Executor executor, C client, Machine<S, A, C> machine) {
            this.machine = machine;
            this.client = new WeakReference<>(client);
            this.executor = executor;
        }

        /**
         * @see #start(Object)
         */
        public void start() {
            machine.start(client.get());
        }

        /**
         * @param action Action to perform when all pending actions complete.
         * @see #start(Executor, Object, Sm.Action)
         */
        public void start(A action) {
            machine.start(executor, client.get(), action);
        }

        /**
         * @see Machine#stop()
         */
        public void stop() {
            machine.stop();
        }

        /**
         * @param action The action to fold.
         * @see #apply(Executor, Object, Sm.Action)
         */
        public void apply(A action) {
            machine.apply(executor, client, action);
        }

        /**
         * Folds the action and runs any resulting future in the same thread.
         *
         * @param action The action to fold.
         * @see #apply(Executor, Object, Sm.Action)
         */
        public void applyNow(A action) {
            machine.apply(IMMEDIATE, client, action);
        }
    }

    public static final Executor IMMEDIATE = new Executor() {
        @Override
        public void execute(Runnable runnable) {
            runnable.run();
        }
    };

    private static final Runnable NOOP = new Runnable() {
        @Override
        public void run() {}
    };

    public static <T> Future<T> now(T t) {
        FutureTask<T> future = new FutureTask<>(NOOP, t);
        future.run();
        return future;
    }

    public static <T> Future<T> future(Callable<T> producer) {
        return new FutureTask<>(producer);
    }

    protected S state;
    protected boolean isRunning = false;

    protected Machine(S state) {
        this.state = state;
    }

    public S state() {
        return state;
    }

    public void stop() {
        isRunning = false;
    }

    public void start(C client) {
        isRunning = true;
        for (Iterator<Future<A>> it = state.iterator(); it.hasNext();) {
            Future<A> futureAction = it.next();
            if (futureAction.isDone()) {
                it.remove();
                try {
                    state = futureAction.get().fold(state, client);
                } catch (InterruptedException | ExecutionException e) {
                    handle(e, client);
                }
            }
        }
    }

    public void start(final Executor worker, C client, final A action) {
        start(client);
        final WeakReference<C> clientRef = new WeakReference<>(client);
        worker.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    state.backlog().await();
                    apply(worker, clientRef, action);
                } catch (InterruptedException e) {
                    handle(e, clientRef.get());
                }
            }
        });
    }

    public void apply(Executor worker, C client, A action) {
        apply(worker, new WeakReference<>(client), action);
    }

    protected void apply(
            final Executor worker,
            final WeakReference<C> clientRef,
            final A action) {
        main(new Runnable() {
            @Override
            public void run() {
                if (!isRunning) {
                    state = state.async(now(action));
                    return;
                }
                state = action.fold(state, clientRef.get());
                final Backlog work = state.backlog();
                List<Future<A>> generation = new ArrayList<>();
                synchronized (state) {
                    for (Iterator<Future<A>> it = state.iterator(); it.hasNext(); it.remove()) {
                        generation.add(it.next());
                    }
                }
                for (final Future<A> futureAction : generation) {
                    work.started();
                    worker.execute(new Runnable() {
                        @Override
                        public void run() {
                            if (futureAction instanceof RunnableFuture &&
                                    !futureAction.isDone()) {
                                ((RunnableFuture) futureAction).run();
                            }
                            try {
                                apply(worker, clientRef, futureAction.get());
                            } catch (InterruptedException | ExecutionException e) {
                                handle(e, clientRef.get());
                            } finally {
                                work.done();
                            }
                        }
                    });
                }
            }
        });
    }

    protected void main(Runnable block) {
        block.run();
    }
}
