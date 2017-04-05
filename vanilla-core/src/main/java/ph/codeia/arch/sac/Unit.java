package ph.codeia.arch.sac;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

import ph.codeia.arch.ErrorHandler;

/**
 * This file is a part of the vanilla project.
 */

public abstract class Unit<
        S extends State<S, A>,
        A extends Action<S, A, C>,
        C>
implements ErrorHandler<C> {

    public static abstract class Builder<
            S extends State<S, A>,
            A extends Action<S, A, C>,
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

        public abstract Unit<S, A, C> build();

        public Fixed<S, A, C> build(C client) {
            return build(IMMEDIATE, client);
        }

        public Fixed<S, A, C> build(Executor executor, C client) {
            return new Fixed<>(executor, client, build());
        }
    }

    public static class Fixed<
            S extends State<S, A>,
            A extends Action<S, A, C>,
            C> {
        public final Unit<S, A, C> unit;
        private final WeakReference<C> client;
        private final Executor executor;

        public Fixed(Executor executor, C client, Unit<S, A, C> unit) {
            this.unit = unit;
            this.client = new WeakReference<>(client);
            this.executor = executor;
        }

        public void start() {
            unit.start(client.get());
        }

        public void start(A action) {
            unit.start(executor, client.get(), action);
        }

        public void stop() {
            unit.stop();
        }

        public void apply(A action) {
            unit.apply(executor, client, action);
        }

        public void applyNow(A action) {
            unit.apply(IMMEDIATE, client, action);
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

    protected Unit(S state) {
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
                for (Iterator<Future<A>> it = state.iterator(); it.hasNext();) {
                    final Future<A> futureAction = it.next();
                    it.remove();
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
