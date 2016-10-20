package ph.codeia.values;

/**
 * This file is a part of the vanilla project.
 */

/**
 * A simple observer pattern contract.
 *
 * @param <T> The type of the messages sent.
 */
public interface Channel<T> {

    /**
     * A token object that unregisters a listener.
     */
    interface Link {
        void unlink();
    }

    /**
     * Calls every listener with this value.
     */
    void send(T message);

    /**
     * Unregisters all listeners.
     */
    void unlinkAll();

    /**
     * Registers a listener.
     *
     * @return Save this value to unregister this listener later.
     */
    Link link(Do.Just<T> listener);

}
