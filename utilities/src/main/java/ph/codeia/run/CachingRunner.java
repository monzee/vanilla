package ph.codeia.run;

import ph.codeia.values.Do;

/**
 * This file is a part of the vanilla project.
 */

public interface CachingRunner extends Runner {

    <T> Do.Executable<T> once(String key, Do.Executable<T> block);
    <T, U> Do.Continue<T, U> once(String key, Do.Continue<T, U> block);

}
