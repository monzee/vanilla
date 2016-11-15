package ph.codeia.androidutilstests;

import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import ph.codeia.androidutils.AndroidRunner;
import ph.codeia.run.Runner;
import ph.codeia.values.Do;

import static org.junit.Assert.*;

/**
 * This file is a part of the vanilla project.
 *
 * @author mon
 */

@RunWith(AndroidJUnit4.class)
@SmallTest
public class AndroidRunnerTest {

    private static final ThreadLocal<String> S = new ThreadLocal<>();
    private static final Do.Just<Object> NOOP = e -> {};

    @Test
    public void ui_runner_runs_code_in_the_ui_thread() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> S.set("main"));
        S.set("test");
        Runner ui = AndroidRunner.UI;
        Do.Just<Object> check = e -> assertEquals(e, S.get());
        ui.run(check).got("main");
        ui.apply(next -> check.got("main")).begin(NOOP);
        ui.apply((e, next) -> {
            check.got("main");
            assertEquals("asdf", e);
        }).then("asdf", NOOP);
    }

    private int counter = 0;

    @Test
    public void serial_runner_does_not_run_tasks_concurrently() {
        Runner r = AndroidRunner.ASYNC_SERIAL.get();
        counter = 0;
        for (int i = 0; i < 50; i++) {
            final int j = i;
            r.<Integer>apply(next -> next.got(counter++))
                    .begin(n -> assertEquals(j, n.intValue()));
        }
    }

    private final Object mutex = new Object();

    @Test(timeout = 1000)
    public void pool_runner_runs_tasks_concurrently() throws InterruptedException {
        Runner r = AndroidRunner.ASYNC_POOL.get();
        counter = 0;

        r.apply(next -> {
            synchronized (mutex) {
                while (counter == 0) try {
                    mutex.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                counter++;
                mutex.notifyAll();
            }
        }).begin(NOOP);

        r.apply(next -> {
            synchronized (mutex) {
                counter++;
                mutex.notifyAll();
            }
        }).begin(NOOP);

        while (counter < 2) synchronized (mutex) {
            mutex.wait();
        }
        assertEquals(2, counter);
    }

}
