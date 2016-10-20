package ph.codeia.values;

import java.lang.ref.WeakReference;

/**
 * This file is a part of the vanilla project.
 */

public class Replay<T> implements Channel<T> {

    private final Channel<T> delegate;
    private WeakReference<T> lastValue;

    public Replay(Channel<T> delegate) {
        this.delegate = delegate;
    }

    @Override
    public void send(T message) {
        lastValue = new WeakReference<>(message);
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
