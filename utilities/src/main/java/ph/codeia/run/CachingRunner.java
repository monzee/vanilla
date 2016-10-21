package ph.codeia.run;

import ph.codeia.values.Do;

/**
 * This file is a part of the vanilla project.
 */

/**
 * Runner interface extension to allow passing keys so that the results
 * may be indexed.
 */
public interface CachingRunner extends Runner {

    <T> Do.Execute<T> once(String key, Do.Execute<T> block);

    <T, U> Do.Continue<T, U> once(String key, Do.Continue<T, U> block);

}
