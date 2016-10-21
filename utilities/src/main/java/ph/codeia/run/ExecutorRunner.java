package ph.codeia.run;

import java.util.concurrent.Executor;

import ph.codeia.values.Do;

/**
 * This file is a part of the vanilla project.
 */

public class ExecutorRunner implements Runner {

    private final Executor context;

    public ExecutorRunner(Executor context) {
        this.context = context;
    }

    @Override
    public <T> Do.Executable<T> run(Do.Executable<T> block) {
        return next -> context.execute(() -> block.start(next));
    }

    @Override
    public <T, U> Do.Continue<T, U> run(Do.Continue<T, U> block) {
        return (value, next) -> context.execute(() -> block.then(value, next));
    }

    @Override
    public <T> Do.Just<T> run(Do.Just<T> block) {
        return value -> context.execute(() -> block.got(value));
    }

}
