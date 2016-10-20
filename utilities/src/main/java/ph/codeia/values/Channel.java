package ph.codeia.values;

/**
 * This file is a part of the vanilla project.
 */

public interface Channel<T> {

    interface Link {
        void unlink();
    }

    void send(T message);

    void unlinkAll();

    Link link(Do.Just<T> listener);

}
