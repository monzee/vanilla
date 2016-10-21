package ph.codeia.run;

import ph.codeia.values.Do;

/**
 * This file is a part of the vanilla project.
 */

public class Interleave implements Runner {

    private final Runner worker;
    private final Runner main;

    public Interleave(Runner worker, Runner main) {
        this.worker = worker;
        this.main = main;
    }

    @Override
    public <T> Do.Executable<T> run(Do.Executable<T> block) {
        return worker.run((Do.Executable<T>) next -> block.start(main.run(next)));
    }

    @Override
    public <T, U> Do.Continue<T, U> run(Do.Continue<T, U> block) {
        return worker.run((value, next) -> block.then(value, main.run(next)));
    }

    @Override
    public <T> Do.Just<T> run(Do.Just<T> block) {
        return main.run(block);
    }

}
