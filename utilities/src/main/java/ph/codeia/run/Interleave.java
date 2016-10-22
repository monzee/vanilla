package ph.codeia.run;

import ph.codeia.values.Do;

/**
 * This file is a part of the vanilla project.
 */

/**
 * Runs the producer and the consumer parts of a block in separate runners.
 *
 * Producers are run in the worker context, consumers in the main.
 */
public class Interleave implements Runner {

    private final Runner worker;
    private final Runner main;

    public Interleave(Runner worker, Runner main) {
        this.worker = worker;
        this.main = main;
    }

    /**
     * Returns a new async runner with the contexts flipped.
     */
    public Interleave inverse() {
        return new Interleave(main, worker);
    }

    @Override
    public <T> Do.Execute<T> run(final Do.Execute<T> block) {
        return worker.run(new Do.Execute<T>() {
            @Override
            public void begin(Do.Just<T> next) {
                block.begin(main.run(next));
            }
        });
    }

    @Override
    public <T, U> Do.Continue<T, U> run(final Do.Continue<T, U> block) {
        return worker.run(new Do.Continue<T, U>() {
            @Override
            public void then(T value, Do.Just<U> next) {
                block.then(value, main.run(next));
            }
        });
    }

    @Override
    public <T> Do.Just<T> run(Do.Just<T> block) {
        return main.run(block);
    }

}
