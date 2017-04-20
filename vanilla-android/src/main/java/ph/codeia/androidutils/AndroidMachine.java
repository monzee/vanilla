package ph.codeia.androidutils;

import ph.codeia.arch.ErrorHandler;
import ph.codeia.arch.LogLevel;
import ph.codeia.arch.Logger;
import ph.codeia.arch.sm.Sm;
import ph.codeia.arch.sm.Machine;
import ph.codeia.meta.Untested;
import ph.codeia.values.Do;

/**
 * This file is a part of the vanilla project.
 */

/**
 * A state {@link Machine} that runs actions in the main looper.
 *
 * If no {@link ErrorHandler} is passed, the default implementation simply
 * rethrows in the main thread any exception thrown in async actions so that
 * you get a crash instead of just silently logged errors. No doubt you'd
 * want to replace this with something more robust for release versions. You
 * can use {@link Builder#withErrorHandler(ErrorHandler)} to change the error
 * handler.
 *
 * @param <S> The state type.
 * @param <A> The action type.
 * @param <C> The client type.
 */
@Untested
public class AndroidMachine<
        S extends Sm.State<S, A>,
        A extends Sm.Action<S, A, C>,
        C>
extends Machine<S, A, C> {

    public static class Builder<
            S extends Sm.State<S, A>,
            A extends Sm.Action<S, A, C>,
            C>
    extends Machine.Builder<S, A, C> {
        public Builder(S state) {
            super(state);
        }

        @Override
        public AndroidMachine<S, A, C> build() {
            return new AndroidMachine<>(state, handler);
        }
    }

    private static final Do.Just<Runnable> MAIN =
            AndroidRunner.UI.run(new Do.Just<Runnable>() {
                @Override
                public void got(Runnable value) {
                    value.run();
                }
            });

    private final ErrorHandler<C> errorHandler;

    public AndroidMachine(S state) {
        this(state, null);
    }

    public AndroidMachine(S state, ErrorHandler<C> errorHandler) {
        super(state);
        this.errorHandler = errorHandler;
    }

    @Override
    public void handle(final Throwable error, C client) {
        if (errorHandler != null) {
            errorHandler.handle(error, client);
        } else if (client != null && client instanceof Logger) {
            LogLevel.E.to((Logger) client, error);
        } else {
            main(new Runnable() {
                @Override
                public void run() {
                    if (error instanceof RuntimeException) {
                        throw (RuntimeException) error;
                    } else {
                        throw new RuntimeException(error);
                    }
                }
            });
        }
    }

    @Override
    protected void main(Runnable block) {
        MAIN.got(block);
    }
}