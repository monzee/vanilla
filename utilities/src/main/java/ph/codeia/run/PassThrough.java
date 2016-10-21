package ph.codeia.run;

import ph.codeia.values.Do;

/**
 * This file is a part of the vanilla project.
 */

public class PassThrough implements Runner {

    public static Runner RUNNER = new PassThrough();

    @Override
    public <T> Do.Executable<T> run(Do.Executable<T> block) {
        return block;
    }

    @Override
    public <T, U> Do.Continue<T, U> run(Do.Continue<T, U> block) {
        return block;
    }

    @Override
    public <T> Do.Just<T> run(Do.Just<T> block) {
        return block;
    }

}
