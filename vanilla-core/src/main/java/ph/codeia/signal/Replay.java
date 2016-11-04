package ph.codeia.signal;

import java.lang.ref.SoftReference;

import ph.codeia.values.Do;

/**
 * This file is a part of the vanilla project.
 */

/**
 * A {@link Channel} that remembers the last value sent.
 *
 * The last value is sent immediately to newly-registered listeners
 * if present.
 */
public class Replay<T> implements Channel<T> {

    private final Channel<T> delegate;
    private SoftReference<T> lastValue;

    public Replay(Channel<T> delegate) {
        this.delegate = delegate;
    }

    public Replay() {
        this(new SimpleChannel<T>());
    }

    @Override
    public void send(T message) {
        lastValue = new SoftReference<>(message);
        delegate.send(message);
    }

    @Override
    public void unlinkAll() {
        delegate.unlinkAll();
    }

    @Override
    public Link link(Do.Just<T> listener) {
        if (lastValue != null) {
            T value = lastValue.get();
            if (value != null) {
                listener.got(value);
            }
        }
        return delegate.link(listener);
    }

}
