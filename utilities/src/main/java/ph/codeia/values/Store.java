package ph.codeia.values;

/**
 * This file is a part of the vanilla project.
 */

public interface Store {

    void put(String key, Object value);

    void softPut(String key, Do.Make<Object> lazyValue);

    <T> T get(String key, T defaultValue);

    <T> T hardGet(String key, Do.Make<T> lazyValue);

}
