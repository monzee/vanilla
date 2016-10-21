package ph.codeia.androidutils;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;

import ph.codeia.run.ExecutorRunner;
import ph.codeia.run.Interleave;
import ph.codeia.run.Runner;
import ph.codeia.values.Do;
import ph.codeia.values.Lazy;

/**
 * This file is a part of the vanilla project.
 */

/**
 * Runs blocks in the UI thread.
 *
 * Also provides a static instance and {@link Lazy lazy} static
 * {@link Interleave} instances that use the static {@link AsyncTask} executors.
 */
public class AndroidRunner implements Runner {

    public static final Runner UI = new AndroidRunner();

    public static final Lazy<Runner> ASYNC_POOL = new Lazy<Runner>() {
        @Override
        protected Runner value() {
            return new Interleave(
                    new ExecutorRunner(AsyncTask.THREAD_POOL_EXECUTOR),
                    UI
            );
        }
    };

    public static final Lazy<Runner> ASYNC_SERIAL = new Lazy<Runner>() {
        @Override
        protected Runner value() {
            return new Interleave(
                    new ExecutorRunner(AsyncTask.SERIAL_EXECUTOR),
                    UI
            );
        }
    };

    private static final Handler UI_HANDLER = new Handler(Looper.getMainLooper());

    @Override
    public <T> Do.Execute<T> run(final Do.Execute<T> block) {
        return new Do.Execute<T>() {
            @Override
            public void begin(final Do.Just<T> next) {
                if (Thread.currentThread() == UI_HANDLER.getLooper().getThread()) {
                    block.begin(next);
                } else {
                    UI_HANDLER.post(new Runnable() {
                        @Override
                        public void run() {
                            block.begin(next);
                        }
                    });
                }
            }
        };
    }

    @Override
    public <T, U> Do.Continue<T, U> run(final Do.Continue<T, U> block) {
        return new Do.Continue<T, U>() {
            @Override
            public void then(final T value, final Do.Just<U> next) {
                if (Thread.currentThread() == UI_HANDLER.getLooper().getThread()) {
                    block.then(value, next);
                } else {
                    UI_HANDLER.post(new Runnable() {
                        @Override
                        public void run() {
                            block.then(value, next);
                        }
                    });
                }
            }
        };
    }

    @Override
    public <T> Do.Just<T> run(final Do.Just<T> block) {
        return new Do.Just<T>() {
            @Override
            public void got(final T value) {
                if (Thread.currentThread() == UI_HANDLER.getLooper().getThread()) {
                    block.got(value);
                } else {
                    UI_HANDLER.post(new Runnable() {
                        @Override
                        public void run() {
                            block.got(value);
                        }
                    });
                }
            }
        };
    }

}
