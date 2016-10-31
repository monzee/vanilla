package ph.codeia.androidutils;

import ph.codeia.signal.Channel;
import ph.codeia.signal.SimpleChannel;
import ph.codeia.values.Do;

/**
 * This file is a part of the vanilla project.
 */

/**
 * Ensures that the listeners are called in the UI thread.
 *
 * {@inheritDoc}
 */
public class AndroidChannel<T> implements Channel<T> {

    private final Channel<T> delegate;

    public AndroidChannel(Channel<T> delegate) {
        this.delegate = delegate;
    }

    public AndroidChannel() {
        this(new SimpleChannel<T>());
    }

    @Override
    public void send(T message) {
        delegate.send(message);
    }

    @Override
    public void unlinkAll() {
        delegate.unlinkAll();
    }

    @Override
    public Link link(Do.Just<T> listener) {
        return delegate.link(AndroidRunner.UI.run(listener));
    }

}
