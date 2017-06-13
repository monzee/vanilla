package ph.codeia.androidutils;

/*
 * This file is a part of the vanilla project.
 */

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;

import ph.codeia.arch.moore.Machine;
import ph.codeia.arch.moore.Msm;

public class AndroidMooreMachine<
        A extends Msm.Action<A, E>,
        E extends Msm.Effect<A, E>>
extends Machine<A, E> {

    private static final Machine.Adapter ANDROID_UI_ADAPTER = new Adapter() {
        private Handler mainHandler = new Handler(Looper.getMainLooper());

        @Override
        public void runOnUiThread(Runnable block) {
            if (Thread.currentThread() == mainHandler.getLooper().getThread()) {
                block.run();
            } else {
                mainHandler.post(block);
            }
        }

        @Override
        public void handle(final Throwable e) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    throw new RuntimeException(e);
                }
            });
        }
    };

    public AndroidMooreMachine(ExecutorService junction, E output) {
        super(junction, output, ANDROID_UI_ADAPTER);
    }

}
