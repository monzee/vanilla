package ph.codeia.values;

import org.junit.AfterClass;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

/**
 * This file is a part of the vanilla project.
 */

public class EitherTest {

    private static final ExecutorService EXEC = Executors.newSingleThreadExecutor();

    @AfterClass
    public static void tearDownClass() {
        EXEC.shutdown();
    }

    @Test
    public void immediate_get() throws Exception {
        Either<?, String> either = new Either<>();
        either.pass("foo");
        assertEquals("foo", either.get());
    }

    @Test
    public void immediate_get_shortcut() throws Exception {
        assertEquals("bar", Either.ok("bar").get());
    }

    @Test(expected = IllegalStateException.class)
    public void immediate_throw() throws InterruptedException {
        Either<IllegalStateException, ?> either = new Either<>();
        either.fail(new IllegalStateException());
        either.get();
    }

    @Test(expected = IllegalStateException.class)
    public void immediate_throw_shortcut() throws Exception {
        Either.error(new IllegalStateException()).get();
    }

    @Test
    public void does_not_throw_if_value_is_already_set() throws InterruptedException {
        Either<RuntimeException, String> either = new Either<>();
        either.pass("foo");
        either.fail(new RuntimeException());
        assertEquals("foo", either.get());
    }

    @Test(expected = RuntimeException.class)
    public void does_not_set_the_value_if_error_is_already_set() throws InterruptedException {
        Either<RuntimeException, String> either = new Either<>();
        either.fail(new RuntimeException());
        either.pass("foo");
        either.get();
    }

    @Test
    public void does_not_overwrite_the_value() throws Exception {
        Either<?, String> either = new Either<>();
        either.pass("foo");
        either.pass("bar");
        assertEquals("foo", either.get());
    }

    @Test
    public void does_not_overwrite_the_error() throws InterruptedException {
        Either<RuntimeException, ?> either = new Either<>();
        either.fail(new RuntimeException("foo"));
        either.fail(new RuntimeException("bar"));
        try {
            either.get();
        } catch (RuntimeException e) {
            assertEquals("foo", e.getMessage());
        }
    }

    @Test(timeout = 1000)
    public void get_blocks_until_value_is_set() throws Exception {
        final Either<?, Void> value = new Either<>();
        final CountDownLatch checkpoint = new CountDownLatch(1);
        final CountDownLatch done = new CountDownLatch(1);
        final AtomicBoolean flag = new AtomicBoolean(false);

        EXEC.execute(() -> {
            checkpoint.countDown();
            try {
                value.get();
            } catch (Exception e) {
                e.printStackTrace();
                fail("interrupted");
            }
            flag.set(true);
            done.countDown();
        });

        checkpoint.await();
        Thread.sleep(16);
        assertFalse(flag.get());

        value.pass(null);
        done.await();
    }

    @Test(timeout = 1000, expected = RuntimeException.class)
    public void get_blocks_until_the_error_is_set() throws InterruptedException {
        final Either<RuntimeException, ?> value = new Either<>();
        final Either<RuntimeException, ?> mirror = new Either<>();
        final CountDownLatch checkpoint = new CountDownLatch(1);
        final CountDownLatch done = new CountDownLatch(1);

        EXEC.execute(() -> {
            checkpoint.countDown();
            try {
                value.get();
                fail("should be unreachable");
            } catch (RuntimeException e) {
                mirror.fail(e);
                done.countDown();
            } catch (InterruptedException e) {
                e.printStackTrace();
                fail("interrupted");
            }
        });

        checkpoint.await();
        Thread.sleep(16);
        value.fail(new RuntimeException());
        done.await();
        mirror.get();
    }

}