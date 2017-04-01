package ph.codeia.arch.sav;

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
        A extends Action<S, A, V>,
        V>
implements ErrorHandler<V> {

    public static abstract class Builder<
            S extends State<S, A>,
            A extends Action<S, A, V>,
            V> {
        protected final S state;
        protected ErrorHandler<V> handler;

        public Builder(S state) {
            this.state = state;
        }

        public Builder<S, A, V> withErrorHandler(ErrorHandler<V> handler) {
            this.handler = handler;
            return this;
        }

        public abstract Unit<S, A, V> build();

        public Fixed<S, A, V> build(V view, Executor executor) {
            return new Fixed<>(view, executor, build());
        }
    }

    public static class Fixed<
            S extends State<S, A>,
            A extends Action<S, A, V>,
            V> {
        public final Unit<S, A, V> unit;
        private final WeakReference<V> view;
        private final Executor executor;

        public Fixed(V view, Executor executor, Unit<S, A, V> unit) {
            this.unit = unit;
            this.view = new WeakReference<>(view);
            this.executor = executor;
        }

        public void start() {
            unit.start(view.get());
        }

        public void start(A action) {
            unit.start(executor, view, action);
        }

        public void stop() {
            unit.stop();
        }

        public void apply(A action) {
            unit.apply(executor, view, action);
        }

        public void applyNow(A action) {
            unit.apply(IMMEDIATE, view, action);
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

    private S state;
    private boolean isRunning = false;

    protected Unit(S state) {
        this.state = state;
    }

    public S state() {
        return state;
    }

    public void stop() {
        isRunning = false;
    }

    public void start(V view) {
        isRunning = true;
        for (Iterator<Future<A>> it = state.iterator(); it.hasNext();) {
            Future<A> futureAction = it.next();
            if (futureAction.isDone()) {
                it.remove();
                try {
                    state = futureAction.get().fold(state, view);
                } catch (InterruptedException | ExecutionException e) {
                    handle(e, view);
                }
            }
        }
    }

    public void start(Executor worker, V view, A action) {
        start(view);
        start(worker, new WeakReference<>(view), action);
    }

    public void apply(Executor worker, V view, A action) {
        apply(worker, new WeakReference<>(view), action);
    }

    protected void start(
            final Executor worker,
            final WeakReference<V> viewRef,
            final A action) {
        worker.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    state.backlog().await();
                    apply(worker, viewRef, action);
                } catch (InterruptedException e) {
                    handle(e, viewRef.get());
                }
            }
        });
    }

    protected void apply(
            final Executor worker,
            final WeakReference<V> viewRef,
            final A action) {
        main(new Runnable() {
            @Override
            public void run() {
                if (!isRunning) {
                    state = state.async(now(action));
                    return;
                }
                state = action.fold(state, viewRef.get());
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
                                apply(worker, viewRef, futureAction.get());
                            } catch (InterruptedException | ExecutionException e) {
                                handle(e, viewRef.get());
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
