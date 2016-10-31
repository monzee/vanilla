package ph.codeia.values;

/**
 * This file is a part of the vanilla project.
 */

import java.util.concurrent.TimeUnit;

/**
 * A single-value version of {@link Either} that never throws.
 *
 * {@link #get()} blocks until a value is set. The value can only be
 * set once.
 *
 * @param <T> The type of the value
 */
public class Wait<T> implements Do.Make<T> {

    private final long timeout;
    private boolean ready = false;
    private T value;

    public Wait(long timeout, TimeUnit unit) {
        this.timeout = unit.toNanos(timeout);
    }

    public Wait() {
        this(0, TimeUnit.SECONDS);
    }

    public Wait(long millis) {
        this(millis, TimeUnit.MILLISECONDS);
    }

    @Override
    public T get() {
        boolean timed = timeout > 0;
        long remaining = timeout;
        while (true) {
            if (ready || (timed && remaining <= 0)) {
                return value;
            } else synchronized (this) {
                try {
                    if (!timed) {
                        wait();
                    } else {
                        long start = System.nanoTime();
                        TimeUnit.NANOSECONDS.timedWait(this, remaining);
                        long elapsed = System.nanoTime() - start;
                        remaining -= elapsed;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return null;
                }
            }
        }
    }

    /**
     * Sets the value. Only the first call works, subsequent calls do nothing.
     */
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
