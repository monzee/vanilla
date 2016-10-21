package ph.codeia.run;

import java.util.concurrent.Executor;

import ph.codeia.values.Do;

/**
 * This file is a part of the vanilla project.
 */

/**
 * Runs a block in a runnable passed to an {@link Executor}.
 */
public class ExecutorRunner implements Runner {

    private final Executor context;

    public ExecutorRunner(Executor context) {
        this.context = context;
    }

    @Override
    public <T> Do.Execute<T> run(final Do.Execute<T> block) {
        return new Do.Execute<T>() {
            @Override
            public void begin(final Do.Just<T> next) {
                context.execute(new Runnable() {
                    @Override
                    public void run() {
                        block.begin(next);
                    }
                });
            }
        };
    }

    @Override
    public <T, U> Do.Continue<T, U> run(final Do.Continue<T, U> block) {
        return new Do.Continue<T, U>() {
            @Override
            public void then(final T value, final Do.Just<U> next) {
                context.execute(new Runnable() {
                    @Override
                    public void run() {
                        block.then(value, next);
                    }
                });
            }
        };
    }

    @Override
    public <T> Do.Just<T> run(final Do.Just<T> block) {
        return new Do.Just<T>() {
            @Override
            public void got(final T value) {
                context.execute(new Runnable() {
                    @Override
                    public void run() {
                        block.got(value);
                    }
                });
            }
        };
    }

}
