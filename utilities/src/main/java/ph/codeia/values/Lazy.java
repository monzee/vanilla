package ph.codeia.values;

/**
 * This file is a part of the vanilla project.
 */

public abstract class Lazy<T> implements Do.Make<T> {

    private boolean ready = false;
    private T value;

    @Override
    public T get() {
        if (ready) {
            return value;
        }
        synchronized (this) {
            if (!ready) {
                value = value();
                ready = true;
            }
        }
        return value;
    }

    protected abstract T value();

    public static <T> Lazy<T> of(Do.Make<T> value) {
        return new Lazy<T>() {
            @Override
            protected T value() {
                return value.get();
            }
        };
    }

}
