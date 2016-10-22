package ph.codeia.run;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import ph.codeia.values.Do;

import static org.junit.Assert.*;

/**
 * This file is a part of the vanilla project.
 */

public class ExecutorContextTest {

    private static final ExecutorService EXEC = Executors.newSingleThreadExecutor();
    private static final ThreadLocal<String> S = new ThreadLocal<>();

    @BeforeClass
    public static void setup() throws InterruptedException {
        final CountDownLatch done = new CountDownLatch(1);
        EXEC.execute(() -> {
            S.set("worker");
            done.countDown();
        });
        done.await();
    }

    @AfterClass
    public static void tearDown() {
        EXEC.shutdown();
    }

    @Test
    public void straw_man() throws InterruptedException {
        Runner r = new ExecutorContext(EXEC);
        AtomicInteger counter = new AtomicInteger(0);
        S.set("caller");
        Seq.of(next -> {
            assertEquals("caller", S.get());
            counter.getAndIncrement();
            next.got(null);
        }).pipe((value, next) -> {
            assertEquals("caller", S.get());
            counter.getAndIncrement();
            next.got(null);
        }).begin(value -> {
            assertEquals("caller", S.get());
            counter.getAndIncrement();
        });
        assertEquals(3, counter.get());
        r.run((Do.Just<String>) name -> assertEquals(name, S.get())).got("worker");
    }

    @Test(timeout = 1000)
    public void wrapping_the_first_step_makes_the_whole_seq_run_in_the_same_context() throws InterruptedException {
        Runner r = new ExecutorContext(EXEC);
        final CountDownLatch done = new CountDownLatch(3);
        assertNotEquals("worker", S.get());
        Seq.of(r.run((Do.Execute<Void>) next -> {
            done.countDown();
            assertEquals("worker", S.get());
            next.got(null);
        })).pipe((value, next) -> {
            done.countDown();
            assertEquals("worker", S.get());
            next.got(null);
        }).begin(value -> {
            done.countDown();
            assertEquals("worker", S.get());
        });
        done.await();
    }

    @Test(timeout = 1000)
    public void wrapping_the_whole_seq_does_the_same() throws InterruptedException {
        Runner r = new ExecutorContext(EXEC);
        final CountDownLatch done = new CountDownLatch(3);
        assertNotEquals("worker", S.get());
        r.run(Seq.of(next -> {
            assertEquals("worker", S.get());
            done.countDown();
            next.got(null);
        }).pipe((value, next) -> {
            assertEquals("worker", S.get());
            done.countDown();
            next.got(null);
        })).begin(value -> {
            assertEquals("worker", S.get());
            done.countDown();
        });
        done.await();
    }

    @Test(timeout = 1000)
    public void wrapping_mid_sequence_does_not_affect_upstream_steps() throws InterruptedException {
        Runner r = new ExecutorContext(EXEC);
        final CountDownLatch done = new CountDownLatch(4);
        S.set("caller");
        Seq.of(next -> {
            assertEquals("caller", S.get());
            done.countDown();
            next.got(null);
        }).pipe((value, next) -> {
            assertEquals("caller", S.get());
            done.countDown();
            next.got(null);
        }).pipe(r.run((value, next) -> {
            assertEquals("worker", S.get());
            done.countDown();
            next.got(null);
        })).begin(value -> {
            assertEquals("worker", S.get());
            done.countDown();
        });
        done.await();
    }

}