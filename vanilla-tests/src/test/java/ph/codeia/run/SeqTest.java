package ph.codeia.run;

import org.junit.AfterClass;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import ph.codeia.values.Do;
import ph.codeia.values.Either;

import static org.junit.Assert.*;

/**
 * This file is a part of the vanilla project.
 */

public class SeqTest {

    @Test
    public void straw_man() throws InterruptedException {
        final AtomicBoolean done = new AtomicBoolean(false);
        Seq.<String> of(next -> {
            next.got("foo");
        }).<String> pipe((result, next) -> {
            next.got(result + "bar");
        }).<String> pipe((result, next) -> {
            next.got(result + "baz");
        }).begin(result -> {
            done.set(true);
            assertEquals("foobarbaz", result);
        });
        assertTrue(done.get());
    }

    @Test
    public void single_step_executed_multiple_times() throws InterruptedException {
        int n = 5;
        final AtomicInteger done = new AtomicInteger(n);
        Seq<?, String> seq = Seq.of(next -> next.got("foobar"));
        Do.Just<String> terminal = result -> {
            done.decrementAndGet();
            assertEquals("foobar", result);
        };
        while (n --> 0) {
            seq.begin(terminal);
        }
        assertEquals(0, done.get());
    }

    @Test
    public void type_changing() {
        Seq.<String> of(next -> {
            next.got("foobarbaz");
        }).<Integer> pipe((result, next) -> {
            next.got(result.length());
        }).begin(result -> {
            assertEquals(9, result.intValue());
        });
    }

    private static ExecutorService EXEC = Executors.newSingleThreadExecutor();

    @AfterClass
    public static void tearDownClass() {
        EXEC.shutdown();
    }

    @Test(timeout = 1000)
    public void async_step_in_the_middle() throws InterruptedException {
        final CountDownLatch done = new CountDownLatch(1);
        Seq.<String> of(next -> {
            next.got("baz");
        }).<String> pipe((result, next) -> EXEC.execute(() -> {
            try {
                Thread.sleep(16);
                next.got("BAR" + result);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        })).<String> pipe((result, next) -> {
            next.got("foo" + result);
        }).begin(result -> {
            assertEquals("fooBARbaz", result);
            done.countDown();
        });
        done.await();
    }

    @Test
    public void error_handling() throws InterruptedException {
        final AtomicBoolean done = new AtomicBoolean(false);
        Seq.<Do.Try<String>> of(next -> {
            next.got(Either.error(new Throwable("hi")));
        }).begin(result -> {
            try {
                result.get();
                fail("should be unreachable");
            } catch (Throwable e) {
                assertEquals("hi", e.getMessage());
            } finally {
                done.set(true);
            }
        });
        assertTrue(done.get());
    }

    @Test
    public void execute_no_receiver() throws InterruptedException {
        int n = 5;
        final AtomicInteger done = new AtomicInteger(n);
        Seq<?, Void> seq = Seq.of(next -> done.decrementAndGet());
        while (n --> 0) {
            seq.begin();
        }
        assertEquals(0, done.get());
    }

    private static final ThreadLocal<String> NAME = new ThreadLocal<>();

    @Test(timeout = 1000)
    public void async_causes_downstream_steps_to_switch_threads() throws InterruptedException {
        NAME.set("main");
        final CountDownLatch nameSet = new CountDownLatch(1);
        EXEC.execute(() -> {
            NAME.set("background");
            nameSet.countDown();
        });
        nameSet.await();
        final CountDownLatch done = new CountDownLatch(4);
        Seq.<Integer> of(next -> {
            assertEquals("main", NAME.get());
            done.countDown();
            next.got(100);
        }).<String> pipe((result, next) -> EXEC.execute(() -> {
            assertEquals("background", NAME.get());
            done.countDown();
            next.got("abcde-" + result);
        })).<String> pipe((result, next) -> {
            assertEquals("background", NAME.get());
            done.countDown();
            next.got(result + "-vwxyz");
        }).begin(result -> {
            assertEquals("background", NAME.get());
            assertEquals("abcde-100-vwxyz", result);
            done.countDown();
        });
        done.await();
    }

    @Test
    public void you_are_not_obliged_to_go_down_the_happy_path() throws InterruptedException {
        final AtomicBoolean happyPath = new AtomicBoolean();
        final AtomicInteger count = new AtomicInteger();
        Seq<?, Void> branch = Seq.<Boolean> of(next -> {
            next.got(happyPath.get());
        }).pipe((result, next) -> {
            count.incrementAndGet();
            if (result) {
                next.got(null);
            }
        }).pipe((result, next) -> {
            count.incrementAndGet();
            next.got(null);
        });
        Do.Just<Void> last = result -> count.incrementAndGet();

        happyPath.set(false);
        branch.begin(last);
        assertEquals(1, count.get());

        count.set(0);
        happyPath.set(true);
        branch.begin(last);
        assertEquals(3, count.get());
    }

}
