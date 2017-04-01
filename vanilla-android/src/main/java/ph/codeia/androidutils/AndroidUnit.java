package ph.codeia.androidutils;

import ph.codeia.arch.ErrorHandler;
import ph.codeia.arch.LogLevel;
import ph.codeia.arch.sav.Action;
import ph.codeia.arch.sav.State;
import ph.codeia.arch.sav.Unit;
import ph.codeia.values.Do;

/**
 * This file is a part of the vanilla project.
 */

public class AndroidUnit<
        S extends State<S, A>,
        A extends Action<S, A, V>,
        V>
extends Unit<S, A, V> {

    public static class Builder<
            S extends State<S, A>,
            A extends Action<S, A, V>,
            V>
    extends Unit.Builder<S, A, V> {
        public Builder(S state) {
            super(state);
        }

        @Override
        public AndroidUnit<S, A, V> build() {
            return new AndroidUnit<>(state, handler);
        }
    }

    private static final Do.Just<Runnable> MAIN =
            AndroidRunner.UI.run(new Do.Just<Runnable>() {
                @Override
                public void got(Runnable value) {
                    value.run();
                }
            });

    private final ErrorHandler<V> errorHandler;

    public AndroidUnit(S state) {
        this(state, null);
    }

    public AndroidUnit(S state, ErrorHandler<V> errorHandler) {
        super(state);
        this.errorHandler = errorHandler;
    }

    @Override
    public void handle(final Throwable error, V view) {
        if (errorHandler != null) {
            errorHandler.handle(error, view);
        } else if (view != null && view instanceof LogLevel.Log) {
            LogLevel.E.to((LogLevel.Log) view, error);
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