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
    public <T> Do.Executable<T> once(String key, Do.Executable<T> block) {
        return delegate.run((Do.Executable<T>) next -> next.got(store.hardGet(key, () -> {
            Wait<T> result = new Wait<>();
            block.start(result::set);
            return result.get();
        })));
    }

    @Override
    public <T, U> Do.Continue<T, U> once(String key, Do.Continue<T, U> block) {
        return delegate.run((value, next) -> next.got(store.hardGet(key, () -> {
            Wait<U> result = new Wait<>();
            block.then(value, result::set);
            return result.get();
        })));
    }

}
