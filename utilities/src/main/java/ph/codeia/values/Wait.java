package ph.codeia.values;

/**
 * This file is a part of the vanilla project.
 */

/**
 * A single-value version of {@link Either} that never throws.
 *
 * {@link #get()} blocks until a value is set. The value can only be
 * set once.
 *
 * @param <T> The type of the value
 */
public class Wait<T> implements Do.Make<T> {

    private boolean ready = false;
    private T value;

    @Override
    public T get() {
        if (ready) {
            return value;
        }
        synchronized (this) {
            while (!ready) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }
        return value;
    }

    public void set(T value) {
        if (ready) {
            return;
        }
        synchronized (this) {
            this.value = value;
            ready = true;
            notifyAll();
        }
    }

}
