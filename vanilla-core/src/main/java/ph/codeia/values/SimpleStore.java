package ph.codeia.values;

import java.lang.ref.SoftReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * This file is a part of the vanilla project.
 */

/**
 * Stores soft references.
 */
public class SimpleStore implements Store {

    private final Map<String, SoftReference<Object>> items =
            Collections.synchronizedMap(new HashMap<String, SoftReference<Object>>());

    @Override
    public void put(String key, Object value) {
        items.put(key, new SoftReference<>(value));
    }

    @Override
    public void softPut(String key, Do.Make<Object> lazyValue) {
        synchronized (items) {
            if (!items.containsKey(key)) {
                put(key, lazyValue.get());
            }
        }
    }

    @Override
    public <T> T get(String key, T fallback) {
        if (items.containsKey(key)) {
            //noinspection unchecked
            return (T) items.get(key).get();
        }
        return fallback;
    }

    @Override
    public <T> T hardGet(String key, Do.Make<T> lazyValue) {
        if (items.containsKey(key)) {
            //noinspection unchecked
            return (T) items.get(key).get();
        }
        synchronized (items) {
            T value = lazyValue.get();
            put(key, value);
            return value;
        }
    }

}
