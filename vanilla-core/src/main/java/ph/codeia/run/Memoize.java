package ph.codeia.run;

import ph.codeia.values.Do;
import ph.codeia.values.Store;
import ph.codeia.values.Wait;

/**
 * This file is a part of the vanilla project.
 */

/**
 * Stores the values produced by a block in a {@link Store}.
 *
 * Blocks passed to {@link #label(String, Do.Continue)} and
 * {@link #label(String, Do.Execute)} may be executed at most once. They will
 * never be called if the key exists in the backing store.
 */
public class Memoize implements LabelledRunner {

    private final Runner delegate;
    private final Store store;

    public Memoize(Runner delegate, Store store) {
        this.delegate = delegate;
        this.store = store;
    }

    public Memoize(Store store) {
        this(PassThrough.RUNNER, store);
    }

    @Override
    public <T> Do.Execute<T> apply(Do.Execute<T> block) {
        return delegate.apply(block);
    }

    @Override
    public <T, U> Do.Continue<T, U> apply(Do.Continue<T, U> block) {
        return delegate.apply(block);
    }

    @Override
    public <T> Do.Just<T> run(Do.Just<T> block) {
        return delegate.run(block);
    }

    @Override
    public <T> Do.Execute<T> label(final String key, final Do.Execute<T> block) {
        return delegate.apply(new Do.Execute<T>() {
            @Override
            public void begin(Do.Just<T> next) {
                next.got(store.hardGet(key, new Do.Make<T>() {
                    @Override
                    public T get() {
                        final Wait<T> result = new Wait<>();
                        block.begin(new Do.Just<T>() {
                            @Override
                            public void got(T value) {
                                result.set(value);
                            }
                        });
                        return result.get();
                    }
                }));
            }
        });
    }

    @Override
    public <T, U> Do.Continue<T, U> label(final String key, final Do.Continue<T, U> block) {
        return delegate.apply(new Do.Continue<T, U>() {
            @Override
            public void then(final T value, Do.Just<U> next) {
                next.got(store.hardGet(key, new Do.Make<U>() {
                    @Override
                    public U get() {
                        final Wait<U> result = new Wait<>();
                        block.then(value, new Do.Just<U>() {
                            @Override
                            public void got(U it) {
                                result.set(it);
                            }
                        });
                        return result.get();
                    }
                }));
            }
        });
    }

}
