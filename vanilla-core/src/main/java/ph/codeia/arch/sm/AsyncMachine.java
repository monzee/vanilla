package ph.codeia.arch.sm;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import ph.codeia.arch.ErrorHandler;
import ph.codeia.arch.LogLevel;
import ph.codeia.arch.Logger;
import ph.codeia.meta.Untested;

/**
 * This file is a part of the vanilla project.
 */

/**
 * Default {@link Machine} implementation.
 *
 * @param <S> The state type.
 * @param <A> The action type.
 * @param <R> The receiver type.
 */
@Untested
public class AsyncMachine<
        S extends Sm.State<S, A>,
        A extends Sm.Action<S, A, R>,
        R>
extends Machine<S, A, R> {

    /**
     * Provides a single thread executor by default.
     *
     * @param <S> The state type.
     * @param <A> The action type.
     * @param <R> The receiver type.
     */
    public static class Builder<
            S extends Sm.State<S, A>,
            A extends Sm.Action<S, A, R>,
            R>
    extends Machine.Builder<S, A, R> {

        private Executor mainExecutor = Executors.newSingleThreadExecutor();

        public Builder(S state) {
            super(state);
        }

        /**
         * Sets the main executor of the machine.
         *
         * @param mainExecutor This must be a single thread executor.
         * @return the same instance.
         * @see Executors#newSingleThreadExecutor()
         */
        public Builder<S, A, R> withExecutor(Executor mainExecutor) {
            this.mainExecutor = mainExecutor;
            return this;
        }

        @Override
        public Machine<S, A, R> build() {
            return new AsyncMachine<>(mainExecutor, state, handler);
        }
    }

    private final Executor mainExecutor;
    private final ErrorHandler<R> handler;

    /**
     * @param mainExecutor This must be a single thread executor.
     * @param state The initial state.
     * @param handler The error handler; may be null.
     * @see Executors#newSingleThreadExecutor()
     */
    public AsyncMachine(Executor mainExecutor, S state, ErrorHandler<R> handler) {
        super(state);
        this.mainExecutor = mainExecutor;
        this.handler = handler;
    }

    @Override
    public void handle(final Throwable error, R client) {
        if (handler != null) {
            handler.handle(error, client);
        } else if (client != null && client instanceof Logger) {
            LogLevel.E.to((Logger) client, error);
        } else {
            main(new Runnable() {
                @Override
                public void run() {
                    throw new RuntimeException(error);
                }
            });
        }
    }

    @Override
    protected void main(Runnable block) {
        mainExecutor.execute(block);
    }
}
