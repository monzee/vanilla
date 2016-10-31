package ph.codeia.signal;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import ph.codeia.values.Do;

/**
 * This file is a part of the vanilla project.
 */

/**
 * A simple channel that keeps a {@link LinkedHashSet} of listeners.
 *
 * The listeners are called in temporal order whenever a message is sent.
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
    public Link link(final Do.Just<T> listener) {
        listeners.add(listener);
        return new Link() {
            @Override
            public void unlink() {
                listeners.remove(listener);
            }
        };
    }

}
