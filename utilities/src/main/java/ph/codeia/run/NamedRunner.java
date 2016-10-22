package ph.codeia.run;

import ph.codeia.values.Do;

/**
 * This file is a part of the vanilla project.
 */

/**
 * Runner interface extension to allow passing keys so that things like result
 * caching may be done.
 */
public interface NamedRunner extends Runner {

    /**
     * Associates a {@link Do.Execute} block with a string key.
     */
    <T> Do.Execute<T> named(String key, Do.Execute<T> block);

    /**
     * Associates a {@link Do.Continue} block with a string key.
     */
    <T, U> Do.Continue<T, U> named(String key, Do.Continue<T, U> block);

}
