package ph.codeia.run;

import ph.codeia.values.Do;
import ph.codeia.values.Store;
import ph.codeia.values.Wait;

/**
 * This file is a part of the vanilla project.
 */

public class Memoize implements CachingRunner {

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
    public <T> Do.Executable<T> run(Do.Executable<T> block) {
        return delegate.run(block);
    }

    @Override
    public <T, U> Do.Continue<T, U> run(Do.Continue<T, U> block) {
        return delegate.run(block);
    }

    @Override
    public <T> Do.Just<T> run(Do.Just<T> block) {
        return delegate.run(block);
    }

    @Override
    public <T> Do.Executable<T> once(final String key, final Do.Executable<T> block) {
        return delegate.run(new Do.Executable<T>() {
            @Override
            public void start(Do.Just<T> next) {
                next.got(store.hardGet(key, new Do.Make<T>() {
                    @Override
                    public T get() {
                        final Wait<T> result = new Wait<>();
                        block.start(new Do.Just<T>() {
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
    public <T, U> Do.Continue<T, U> once(final String key, final Do.Continue<T, U> block) {
        return delegate.run(new Do.Continue<T, U>() {
            @Override
            public void then(final T value, Do.Just<U> next) {
                next.got(store.hardGet(key, new Do.Make<U>() {
                    @Override
                    public U get() {
                        final Wait<U> result = new Wait<>();
                        block.then(value, new Do.Just<U>() {
                            @Override
                            public void got(U value1) {
                                result.set(value1);
                            }
                        });
                        return result.get();
                    }
                }));
            }
        });
    }

}
