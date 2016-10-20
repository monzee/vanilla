package ph.codeia.values;

import org.junit.AfterClass;
import org.junit.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.*;

/**
 * This file is a part of the vanilla project.
 */

public class EitherToFutureTest {

    private static final ExecutorService EXEC = Executors.newSingleThreadExecutor();

    @AfterClass
    public static void tearDownClass() {
        EXEC.shutdown();
    }

    @Test
    public void future_get_works() throws ExecutionException, InterruptedException {
        Either<?, String> e = new Either<>();
        e.pass("foo");
        assertEquals("foo", e.toFuture().get());
    }

    @Test(expected = ExecutionException.class)
    public void converts_throwable_to_execution_exception() throws ExecutionException, InterruptedException {
        Either<Exception, ?> e = new Either<>();
        e.fail(new Exception());
        e.toFuture().get();
    }

    @Test
    public void recover_original_exception() throws InterruptedException {
        Either<Exception, ?> e = new Either<>();
        e.fail(new Exception("foobar"));
        try {
            e.toFuture().get();
            fail("should be unreachable");
        } catch (ExecutionException ex) {
            assertEquals("foobar", ex.getCause().getMessage());
        }
    }

    @Test(expected = ExecutionException.class)
    public void timed_get() throws InterruptedException, ExecutionException, TimeoutException {
        Either<Exception, String> e = new Either<>();
        e.pass("foo");
        assertEquals("foo", e.toFuture().get(1, TimeUnit.NANOSECONDS));
        e = new Either<>();
        e.fail(new Exception());
        e.toFuture().get(1, TimeUnit.NANOSECONDS);
    }

    @Test(expected = ExecutionException.class)
    public void async_timed_get() throws InterruptedException, ExecutionException, TimeoutException {
        Either<?, String> e = new Either<>();
        EXEC.execute(() -> e.pass("foo"));
        assertEquals("foo", e.toFuture().get(1000, TimeUnit.MILLISECONDS));
        Either<Exception, ?> f = new Either<>();
        EXEC.execute(() -> f.fail(new Exception()));
        f.toFuture().get(1000, TimeUnit.MILLISECONDS);
    }

    @Test(expected = TimeoutException.class)
    public void timeout_when_never_completed() throws InterruptedException, ExecutionException, TimeoutException {
        Either<?, ?> e = new Either<>();
        e.toFuture().get(1, TimeUnit.NANOSECONDS);
    }

}
