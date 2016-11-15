package ph.codeia.androidutilstests;

import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import ph.codeia.androidutils.AndroidChannel;
import ph.codeia.signal.Channel;

import static org.junit.Assert.*;

/**
 * This file is a part of the vanilla project.
 *
 * @author mon
 */

@RunWith(AndroidJUnit4.class)
@SmallTest
public class AndroidChannelTest {

    private static final ThreadLocal<String> S = new ThreadLocal<>();

    private final Object lock = new Object();
    private boolean done = false;

    @Test(timeout = 1000)
    public void runs_listener_in_the_ui_thread() throws InterruptedException {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> S.set("main"));
        Channel<String> ch = new AndroidChannel<>();
        ch.link(s -> {
            assertEquals(s, S.get());
            synchronized (lock) {
                done = true;
                lock.notifyAll();
            }
        });
        ch.send("main");
        while (!done) synchronized (lock) {
            lock.wait();
        }
    }

}
