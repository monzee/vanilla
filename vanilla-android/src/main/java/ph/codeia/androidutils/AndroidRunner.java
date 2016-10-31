package ph.codeia.androidutils;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;

import java.util.concurrent.Executor;

import ph.codeia.run.ExecutorContext;
import ph.codeia.run.Interleave;
import ph.codeia.run.Runner;
import ph.codeia.values.Lazy;

/**
 * This file is a part of the vanilla project.
 */

/**
 * Provides a static runner that runs blocks in the UI thread.
 *
 * Also provides {@link Lazy lazy} static {@link Interleave} instances that
 * use the static {@link AsyncTask} executors.
 */
public interface AndroidRunner {

    Runner UI = new ExecutorContext(new Executor() {

        private final Handler uiHandler = new Handler(Looper.getMainLooper());

        @Override
        public void execute(@NonNull Runnable runnable) {
            if (Thread.currentThread() == uiHandler.getLooper().getThread()) {
                runnable.run();
            } else {
                uiHandler.post(runnable);
            }
        }

    });

    Lazy<Runner> ASYNC_POOL = new Lazy<Runner>() {
        @Override
        protected Runner value() {
            return new Interleave(
                    new ExecutorContext(AsyncTask.THREAD_POOL_EXECUTOR),
                    UI
            );
        }
    };

    Lazy<Runner> ASYNC_SERIAL = new Lazy<Runner>() {
        @Override
        protected Runner value() {
            return new Interleave(
                    new ExecutorContext(AsyncTask.SERIAL_EXECUTOR),
                    UI
            );
        }
    };

}
