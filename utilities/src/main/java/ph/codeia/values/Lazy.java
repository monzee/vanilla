package ph.codeia.values;

/**
 * This file is a part of the vanilla project.
 */

/**
 * A value that is computed only once.
 *
 * @param <T> The type of the value.
 */
public abstract class Lazy<T> implements Do.Make<T> {

    /**
     * Convenience factory method for java 1.8+
     */
    public static <T> Lazy<T> of(Do.Make<T> value) {
        return new Lazy<T>() {
            @Override
            protected T value() {
                return value.get();
            }
        };
    }

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

    /**
     * The factory method.
     *
     * It is more convenient to extend and implement this if lambdas
     * aren't available.
     *
     * @return Nullable. Will never be called again even if this returns null.
     */
    protected abstract T value();

}
