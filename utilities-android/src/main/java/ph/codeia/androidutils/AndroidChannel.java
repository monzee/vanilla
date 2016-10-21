package ph.codeia.androidutils;

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
public class AndroidChannel<T> extends SimpleChannel<T> {

    @Override
    public Link link(Do.Just<T> listener) {
        return super.link(AndroidRunner.UI.run(listener));
    }

}
