package ph.codeia.values;

import org.junit.AfterClass;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

/**
 * This file is a part of the vanilla project.
 */

public class DoSeqTest implements Do {

    @Test(timeout = 1000)
    public void straw_man() throws InterruptedException {
        final CountDownLatch done = new CountDownLatch(1);
        Seq.<String> start(next -> {
            next.got("foo");
        }).<String> andThen((result, next) -> {
            next.got(result + "bar");
        }).<String> andThen((result, next) -> {
            next.got(result + "baz");
        }).execute(result -> {
            done.countDown();
            assertEquals("foobarbaz", result);
        });
        done.await();
    }

    @Test(timeout = 1000)
    public void single_step_executed_multiple_times() throws InterruptedException {
        int n = 5;
        final CountDownLatch done = new CountDownLatch(n);
        Seq<?, String> seq = Seq.start(next -> next.got("foobar"));
        Just<String> terminal = result -> {
            done.countDown();
            assertEquals("foobar", result);
        };
        while (n --> 0) {
            seq.execute(terminal);
        }
        done.await();
    }

    @Test
    public void type_changing() {
        Seq.<String> start(next -> {
            next.got("foobarbaz");
        }).<Integer> andThen((result, next) -> {
            next.got(result.length());
        }).execute(result -> {
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
        Seq.<String> start(next -> {
            next.got("baz");
        }).<String> andThen((result, next) -> {
            EXEC.execute(() -> {
                try {
                    Thread.sleep(50);
                    next.got("BAR" + result);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }).<String> andThen((result, next) -> {
            next.got("foo" + result);
        }).execute(result -> {
            assertEquals("fooBARbaz", result);
            done.countDown();
        });
        done.await();
    }

    @Test(timeout = 1000)
    public void error_handling() throws InterruptedException {
        final CountDownLatch done = new CountDownLatch(1);
        Seq.<Try<String>> start(next -> {
            next.got(Either.error(new Throwable("hi")));
        }).execute(result -> {
            try {
                result.get();
                fail("should be unreachable");
            } catch (Throwable e) {
                assertEquals("hi", e.getMessage());
            } finally {
                done.countDown();
            }
        });
        done.await();
    }

    @Test(timeout = 1000)
    public void execute_no_receiver() throws InterruptedException {
        int n = 5;
        final CountDownLatch done = new CountDownLatch(n);
        Seq<?, Void> seq = Seq.start(next -> done.countDown());
        while (n --> 0) {
            seq.execute();
        }
        done.await();
    }

    @Test(timeout = 1000)
    public void async_causes_downstream_steps_to_switch_threads() throws InterruptedException {
        final Thread mainThread = Thread.currentThread();
        final AtomicReference<Thread> bgThread = new AtomicReference<>();
        final CountDownLatch done = new CountDownLatch(4);
        Seq.<Integer> start(next -> {
            assertSame(mainThread, Thread.currentThread());
            done.countDown();
            next.got(100);
        }).<String> andThen((result, next) -> EXEC.execute(() -> {
            bgThread.set(Thread.currentThread());
            done.countDown();
            next.got("abcde-" + result);
        })).<String> andThen((result, next) -> {
            assertSame(bgThread.get(), Thread.currentThread());
            done.countDown();
            next.got(result + "-vwxyz");
        }).execute(result -> {
            assertSame(bgThread.get(), Thread.currentThread());
            assertEquals("abcde-100-vwxyz", result);
            done.countDown();
        });
        done.await();
    }

    @Test
    public void you_are_not_obliged_to_go_down_the_happy_path() throws InterruptedException {
        final AtomicBoolean happyPath = new AtomicBoolean();
        final AtomicInteger count = new AtomicInteger();
        Seq<?, Void> branch = Seq.<Boolean> start(next -> {
            next.got(happyPath.get());
        }).andThen((result, next) -> {
            count.incrementAndGet();
            if (result) {
                next.got(null);
            }
        }).andThen((result, next) -> {
            count.incrementAndGet();
            next.got(null);
        });
        Just<Void> last = result -> count.incrementAndGet();

        happyPath.set(false);
        branch.execute(last);
        assertEquals(1, count.get());

        count.set(0);
        happyPath.set(true);
        branch.execute(last);
        assertEquals(3, count.get());
    }

}
