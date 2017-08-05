package ph.codeia.androidutils;

/*
 * This file is a part of the vanilla project.
 */

import java.util.concurrent.ExecutorService;

import ph.codeia.arch.moore.Machine;
import ph.codeia.arch.moore.Msm;
import ph.codeia.meta.Experimental;
import ph.codeia.values.Do;

@Experimental
public class AndroidMooreMachine<
        A extends Msm.Action<A, E>,
        E extends Msm.Effect<A, E>>
extends Machine<A, E> {

    public static final Dispatcher UI_DISPATCHER = new Dispatcher() {
        private final Do.Just<Runnable> runner = AndroidRunner.UI.run(new Do.Just<Runnable>() {
            @Override
            public void got(Runnable value) {
                value.run();
            }
        });

        @Override
        public void runOnUiThread(Runnable block) {
            runner.got(block);
        }

        @Override
        public void handle(final Throwable e) {
            runner.got(new Runnable() {
                @Override
                public void run() {
                    throw new RuntimeException(e);
                }
            });
        }
    };

    public AndroidMooreMachine(ExecutorService junction, E output) {
        super(junction, output, UI_DISPATCHER);
    }

}
