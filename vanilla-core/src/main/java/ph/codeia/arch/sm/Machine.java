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

        /**
         * @param state The initial state.
         */
        public Builder(S state) {
            this.state = state;
        }

        /**
         * @param handler The function to run when an async action throws.
         * @return the builder.
         */
        public Builder<S, A, C> withErrorHandler(ErrorHandler<C> handler) {
            this.handler = handler;
            return this;
        }

        /**
         * @return a Machine instance.
         */
        public abstract Machine<S, A, C> build();

        /**
         * @param client The receiver object to be passed to actions.
         * @return a Machine bound to this receiver instance.
         */
        public Bound<S, A, C> build(C client) {
            return build(IMMEDIATE, client);
        }

        /**
         * @param executor The context where actions are run.
         * @param client The receiver object to be passed to actions.
         * @return a Machine bound to this executor and receiver.
         */
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

    /**
     * @param state The initial state.
     */
    protected Machine(S state) {
        this.state = state;
    }

    /**
     * @return the current state.
     */
    public S state() {
        return state;
    }

    /**
     * Stops the machine.
     */
    public void stop() {
        isRunning = false;
    }

    /**
     * Starts the machine.
     *
     * Applies completed actions queued in the initial state.
     *
     * @param client The receiver object to be passed to actions.
     */
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

    /**
     * Waits for currently running actions to complete, starts the machine and
     * applies them.
     *
     * @param worker This must NOT be an immediate executor.
     * @param client The receiver object to be passed to actions.
     * @param action The action do when all running actions are completed. A
     *               no-op action is fine ({@code (state, action) -> state}),
     *               unfortunately that cannot be instantiated here because of
     *               the recursive type parameters so you have to pass one.
     */
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

    /**
     * Runs the action with the current state and the receiver presumably in the
     * main thread.
     *
     * The "main thread" and how it is accessed differs by platform so the
     * implementer must implement {@link #main(Runnable)}.
     *
     * @param worker The context to run async actions in.
     * @param client The receiver object to be passed to the action. This will
     *               be wrapped in a weak reference to prevent leakage by long-
     *               running actions.
     * @param action The action to run. The return value will become the new
     *               state of the machine.
     */
    public void apply(Executor worker, C client, A action) {
        apply(worker, new WeakReference<>(client), action);
    }

    /**
     * @see #apply(Executor, Object, Sm.Action)
     */
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
                List<Future<A>> generation = new ArrayList<>();
                for (Iterator<Future<A>> it = state.iterator(); it.hasNext(); it.remove()) {
                    generation.add(it.next());
                }
                final Backlog work = state.backlog();
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

    /**
     * Executes the action in the main thread of the platform.
     *
     * To preserve the invariants, all state manipulation must be done in the
     * same thread. For GUI platforms, there's usually a single thread dedicated
     * to UI manipulation and a way to execute blocks of code in that thread.
     * You'll probably want to do UI updates in your actions so the {@code main}
     * implementation should call the block in the UI thread.
     *
     * For others, you'd need a dedicated single thread executor for the machine
     * and run {@code block} in that context. Simply calling {@code block.run()}
     * here would only work if none of the actions are async or the executor
     * passed to {@link #apply(Executor, Object, Sm.Action)} is {@code
     * Runnable::run}. This means that the system is fully synchronous;
     * everything runs on the same thread from start to finish, even actions
     * added through {@link Sm.State#async(Future)}. For example, {@link
     * Stepper} ignores the executor passed to {@code apply} and runs
     * everything in the same thread as the caller, so its {@code main}
     * implementation simply calls {@code block.run()}. It is meant to be used
     * in tests so it is fair to assume that all {@code apply} calls will be in
     * the same thread.
     *
     * @param block Runnable wrapping an action
     */
    abstract protected void main(Runnable block);
}
