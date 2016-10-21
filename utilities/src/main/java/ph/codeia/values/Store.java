package ph.codeia.values;

/**
 * This file is a part of the vanilla project.
 */

/**
 * Stores values.
 */
public interface Store {

    /**
     * Saves a value; overwrites if key exists.
     */
    void put(String key, Object value);

    /**
     * Saves a value; does not run the factory nor overwrite an existing key.
     *
     * @param lazyValue Will be run immediately in the same thread as the
     *                  caller if needed.
     */
    void softPut(String key, Do.Make<Object> lazyValue);

    /**
     * Gets a value; returns the given value if not present.
     */
    <T> T get(String key, T defaultValue);

    /**
     * Gets a value; saves the result of the factory if not present.
     *
     * @param lazyValue Will not be run if the key exists. Will be run
     *                  immediately in the caller thread otherwise.
     */
    <T> T hardGet(String key, Do.Make<T> lazyValue);

}
