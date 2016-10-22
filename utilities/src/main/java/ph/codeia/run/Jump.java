package ph.codeia.run;

import ph.codeia.meta.Experimental;
import ph.codeia.values.Do;
import ph.codeia.values.SimpleStore;
import ph.codeia.values.Store;

/**
 * This file is a part of the vanilla project.
 */

@Experimental
public class Jump implements LabelledRunner {

    private final Runner delegate;
    private final Store labels = new SimpleStore();

    public Jump(Runner delegate) {
        this.delegate = delegate;
    }

    public Jump() {
        this(PassThrough.RUNNER);
    }

    @Override
    public <T> Do.Execute<T> wrap(final Do.Execute<T> block) {
        return delegate.wrap(new Do.Execute<T>() {
            @Override
            public void begin(Do.Just<T> next) {
                boolean shouldJump = Control.JUMP.signalled();
                String label = Control.target();
                Control.clear();
                if (!shouldJump || label == null) {
                    block.begin(next);
                } else {
                    jump(label, null);
                }
            }
        });
    }

    @Override
    public <T, U> Do.Continue<T, U> wrap(final Do.Continue<T, U> block) {
        return delegate.wrap(new Do.Continue<T, U>() {
            @Override
            public void then(T value, Do.Just<U> next) {
                boolean shouldJump = Control.JUMP.signalled();
                String label = Control.target();
                Control.clear();
                if (!shouldJump || label == null) {
                    block.then(value, next);
                } else {
                    jump(label, value);
                }
            }
        });
    }

    @Override
    public <T> Do.Just<T> run(final Do.Just<T> block) {
        return delegate.run(new Do.Just<T>() {
            @Override
            public void got(T value) {
                boolean shouldJump = Control.JUMP.signalled();
                String label = Control.target();
                Control.clear();
                if (!shouldJump || label == null) {
                    block.got(value);
                } else {
                    jump(label, value);
                }
            }

        });
    }

    @Override
    public <T> Do.Execute<T> label(final String key, final Do.Execute<T> block) {
        return delegate.wrap(new Do.Execute<T>() {
            @Override
            public void begin(final Do.Just<T> next) {
                labels.put(key, new Do.Just<T>() {
                    @Override
                    public void got(T ignored) {
                        block.begin(next);
                    }
                });
                block.begin(next);
            }
        });
    }

    @Override
    public <T, U> Do.Continue<T, U> label(
            final String key,
            final Do.Continue<T, U> block
    ) {
        return delegate.wrap(new Do.Continue<T, U>() {
            @Override
            public void then(final T value, final Do.Just<U> next) {
                labels.put(key, new Do.Just<T>() {
                    @Override
                    public void got(T ignored) {
                        block.then(value, next);
                    }
                });
                block.then(value, next);
            }
        });
    }

    private <T> void jump(String label, T value) {
        try {
            Do.Just<T> it = labels.get(label, null);
            if (it == null) {
                throw new LabelNotFound(label);
            } else {
                it.got(value);
            }
        } catch (ClassCastException e) {
            throw new LabelNotFound(label + ": type mismatch");
        }
    }

}
