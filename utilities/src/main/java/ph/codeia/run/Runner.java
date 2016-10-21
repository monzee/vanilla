package ph.codeia.run;

import ph.codeia.values.Do;

/**
 * This file is a part of the vanilla project.
 */

public interface Runner {

    <T> Do.Execute<T> run(Do.Execute<T> block);
    <T, U> Do.Continue<T, U> run(Do.Continue<T, U> block);
    <T> Do.Just<T> run(Do.Just<T> block);

}
