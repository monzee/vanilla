package ph.codeia.values;

import org.junit.AfterClass;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

/**
 * This file is a part of the vanilla project.
 */

public class WaitTest {

    private static final ExecutorService E = Executors.newSingleThreadExecutor();

    @AfterClass
    public static void tearDown() {
        E.shutdown();
    }

    @Test(timeout = 1000)
    public void blocks_until_set() throws InterruptedException {
        Wait<Void> value = new Wait<>();
        AtomicBoolean flag = new AtomicBoolean(false);
        CountDownLatch checkpoint = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(1);
        E.execute(() -> {
            checkpoint.countDown();
            value.get();
            flag.set(true);
            done.countDown();
        });

        checkpoint.await();
        Thread.sleep(16);
        assertFalse(flag.get());

        value.set(null);
        done.await();
        assertTrue(flag.get());
    }

    @Test(timeout = 1000)
    public void returns_null_on_timeout() throws InterruptedException {
        Wait<String> value = new Wait<>(5);
        CountDownLatch done = new CountDownLatch(1);

        E.execute(() -> {
            assertNull(value.get());
            done.countDown();
        });

        Thread.sleep(16);
        value.set("abcdef");
        done.await();
        assertEquals("abcdef", value.get());
    }

    @Test
    public void can_only_be_set_once() {
        Wait<String> value = new Wait<>();
        value.set("abc");
        value.set("def");
        assertEquals("abc", value.get());
    }

}
