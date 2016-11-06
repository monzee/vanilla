package ph.codeia.run;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.*;

/**
 * This file is a part of the vanilla project.
 */

public class InterleaveTest {

    private static final ThreadLocal<String> S = new ThreadLocal<>();
    private static final ExecutorService BG = Executors.newSingleThreadExecutor();
    private static final ExecutorService FG = Executors.newSingleThreadExecutor();

    @BeforeClass
    public static void setup() throws InterruptedException {
        final CountDownLatch done = new CountDownLatch(2);
        BG.execute(() -> {
            S.set("worker");
            done.countDown();
        });
        FG.execute(() -> {
            S.set("main");
            done.countDown();
        });
        done.await();
    }

    @AfterClass
    public static void tearDown() {
        BG.shutdown();
        FG.shutdown();
    }

    private final Runner worker = new ExecutorContext(BG);
    private final Runner main = new ExecutorContext(FG);

    @Test(timeout = 1000)
    public void straw_man() throws InterruptedException {
        Runner r = new Interleave(worker, main);
        final CountDownLatch done = new CountDownLatch(2);
        S.set("caller");
        r.apply(next -> {
            assertEquals("worker", S.get());
            done.countDown();
            next.got(null);
        }).begin(next -> {
            assertEquals("main", S.get());
            done.countDown();
        });
        done.await();
    }

    @Test(timeout = 1000)
    public void wrapping_the_first_step_executes_it_in_worker_and_downstream_in_main() throws InterruptedException {
        Runner r = new Interleave(worker, main);
        final CountDownLatch done = new CountDownLatch(3);
        S.set("caller");
        Seq.of(r.apply(next -> {
            assertEquals("worker", S.get());
            done.countDown();
            next.got(null);
        })).pipe((value, next) -> {
            assertEquals("main", S.get());
            done.countDown();
            next.got(null);
        }).begin(value -> {
            assertEquals("main", S.get());
            done.countDown();
        });
        done.await();
    }

    @Test(timeout = 1000)
    public void wrapping_mid_sequence_does_the_same() throws InterruptedException {
        Runner r = new Interleave(worker, main);
        final CountDownLatch done = new CountDownLatch(3);
        S.set("caller");
        Seq.of(next -> {
            assertEquals("caller", S.get());
            done.countDown();
            next.got(null);
        }).pipe(r.apply((value, next) -> {
            assertEquals("worker", S.get());
            done.countDown();
            next.got(null);
        })).begin(value -> {
            assertEquals("main", S.get());
            done.countDown();
        });
        done.await();
    }

    @Test(timeout = 1000)
    public void wrapping_the_whole_seq_makes_everything_run_in_worker_except_the_last() throws InterruptedException {
        Runner r = new Interleave(worker, main);
        final CountDownLatch done = new CountDownLatch(3);
        S.set("caller");
        r.apply(Seq.of(next -> {
            assertEquals("worker", S.get());
            done.countDown();
            next.got(null);
        }).pipe((value, next) -> {
            assertEquals("worker", S.get());
            done.countDown();
            next.got(null);
        })).begin(value -> {
            assertEquals("main", S.get());
            done.countDown();
        });
        done.await();
    }

}