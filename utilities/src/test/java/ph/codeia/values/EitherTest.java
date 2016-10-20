package ph.codeia.values;

import org.junit.AfterClass;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
    public void immediate_get() throws Throwable {
        Either<?, String> either = new Either<>();
        either.pass("foo");
        assertEquals("foo", either.get());
    }

    @Test
    public void immediate_get_shortcut() throws Throwable {
        assertEquals("bar", Either.ok("bar").get());
    }

    @Test(expected = IllegalStateException.class)
    public void immediate_throw() throws InterruptedException {
        Either<IllegalStateException, ?> either = new Either<>();
        either.fail(new IllegalStateException());
        either.get();
    }

    @Test(expected = IllegalStateException.class)
    public void immediate_throw_shortcut() throws Throwable {
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
    public void does_not_overwrite_the_value() throws Throwable {
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

    @Test
    public void get_blocks_until_value_is_set() throws Throwable {
        final Either<?, String> either = new Either<>();
        final Either<?, String> mirror = new Either<>();
        final CountDownLatch signal = new CountDownLatch(1);
        EXEC.execute(() -> {
            try {
                mirror.pass(either.get());
            } catch (Throwable throwable) {
                throwable.printStackTrace();
                fail("interrupted");
            } finally {
                signal.countDown();
            }
        });
        assertFalse(signal.await(50, TimeUnit.MILLISECONDS));
        either.pass("foo");
        assertTrue(signal.await(50, TimeUnit.MILLISECONDS));
        assertEquals("foo", mirror.get());
    }

    @Test(expected = RuntimeException.class)
    public void get_blocks_until_the_error_is_set() throws InterruptedException {
        final Either<RuntimeException, ?> either = new Either<>();
        final Either<RuntimeException, ?> mirror = new Either<>();
        final CountDownLatch signal = new CountDownLatch(1);
        EXEC.execute(() -> {
            try {
                either.get();
            } catch (RuntimeException e) {
                mirror.fail(e);
            } catch (InterruptedException e) {
                e.printStackTrace();
                fail("interrupted");
            } finally {
                signal.countDown();
            }
        });
        assertFalse(signal.await(50, TimeUnit.MILLISECONDS));
        either.fail(new RuntimeException("asdfasdf"));
        assertTrue(signal.await(50, TimeUnit.MILLISECONDS));
        mirror.get();
    }

}