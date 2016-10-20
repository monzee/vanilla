package ph.codeia.values;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * This file is a part of the vanilla project.
 */

public class SimpleChannel<T> implements Channel<T> {

    private final Set<Do.Just<T>> listeners =
            Collections.synchronizedSet(new LinkedHashSet<Do.Just<T>>());

    @Override
    public void send(T message) {
        synchronized (listeners) {
            for (Do.Just<T> listener : listeners) {
                listener.got(message);
            }
        }
    }

    @Override
    public void unlinkAll() {
        listeners.clear();
    }

    @Override
    public Link link(Do.Just<T> listener) {
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

}
