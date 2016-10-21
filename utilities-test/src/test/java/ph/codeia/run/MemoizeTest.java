package ph.codeia.run;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import ph.codeia.values.Do;
import ph.codeia.values.SimpleStore;
import ph.codeia.values.Store;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

/**
 * This file is a part of the vanilla project.
 */

public class MemoizeTest {

    private static final ThreadLocal<String> S = new ThreadLocal<>();
    private static final ExecutorService BG = Executors.newSingleThreadExecutor();
    private static final ExecutorService FG = Executors.newSingleThreadExecutor();
    private static final Runner ASYNC = new Interleave(
            new ExecutorRunner(BG),
            new ExecutorRunner(FG)
    );

    @BeforeClass
    public static void staticSetup() throws InterruptedException {
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

    private Store cache;

    @Before
    public void setup() {
        cache = new SimpleStore();
    }

    @Test
    public void straw_man() {
        CachingRunner r = new Memoize(cache);
        assertNull(cache.get("key", null));
        final AtomicBoolean called = new AtomicBoolean(false);

        r.once("key", (Do.Execute<String>) next -> {
            next.got("FOO");
            called.set(true);
        }).begin(value -> {
            assertEquals("FOO", value);
        });

        assertTrue(called.get());
        assertEquals("FOO", cache.get("key", null));

        called.set(false);
        final AtomicBoolean done = new AtomicBoolean(false);
        r.once("key", (Do.Execute<String>) next -> {
            next.got("BAR");
            called.set(true);
        }).begin(value -> {
            assertNotEquals("BAR", value);
            done.set(true);
        });

        assertFalse(called.get());
        assertTrue(done.get());
        assertEquals("FOO", cache.get("key", null));
    }

    @Test
    public void memoize_mid_sequence_calls_upstream_but_ignores_the_passed_value() {
        CachingRunner r = new Memoize(cache);
        Do.Continue<String, String> mid = r.once("key", (value, next) -> {
            next.got(value + "bar");
        });

        Seq.<String> of(next -> {
            next.got("foo");
        }).pipe(mid).begin(value -> {
            assertEquals("foobar", value);
        });

        final AtomicBoolean called = new AtomicBoolean(false);
        final AtomicReference<String> result = new AtomicReference<>();
        Seq.<String> of(next -> {
            next.got("FOO");
            called.set(true);
        }).pipe(mid).begin(value -> {
            assertThat(value, not(containsString("FOO")));
            result.set(value);
        });
        assertTrue(called.get());
        assertEquals("foobar", result.get());
    }

    @Test
    public void wrapped_block_never_called_more_than_once() {
        CachingRunner r = new Memoize(cache);
        final AtomicInteger count = new AtomicInteger(0);
        Do.Continue<String, String> mid = r.once("key", (value, next) -> {
            count.incrementAndGet();
            next.got(value + "bar");
        });

        AtomicReference<String> result = new AtomicReference<>();
        Seq.<String> of(next -> {
            next.got("foo");
        }).pipe(mid).pipe(mid).pipe(mid).pipe(mid).begin(result::set);

        assertEquals(1, count.get());
        assertEquals("foobar", result.get());
    }

    @Test
    public void block_is_never_called_if_the_cache_already_has_a_stored_value() {
        CachingRunner r = new Memoize(cache);
        cache.put("key", "NOPE");

        AtomicBoolean called = new AtomicBoolean(false);
        AtomicReference<String> result = new AtomicReference<>();
        r.once("key", (Do.Just<String> next) -> {
            called.set(true);
            next.got("yes");
        }).begin(result::set);

        assertFalse(called.get());
        assertEquals("NOPE", result.get());
    }

    @Test
    public void block_with_same_key_is_never_called() {
        CachingRunner r = new Memoize(cache);
        final AtomicBoolean called = new AtomicBoolean(false);

        AtomicReference<String> result = new AtomicReference<>();
        Seq.<String> of(next -> {
            next.got("foo");
        }).<String> pipe(r.once("key", (value, next) -> {
            next.got(value + "bar");
        })).<String> pipe((value, next) -> {
            next.got(value + "baz");
        }).<String> pipe(r.once("key", (value, next) -> {
            called.set(true);
            next.got(value + "quux");
        })).begin(result::set);

        assertFalse(called.get());
        assertEquals("foobar", result.get());
    }

    @Test(timeout = 1000)
    public void async_delegate_does_not_deadlock() throws InterruptedException {
        CachingRunner r = new Memoize(ASYNC, cache);
        final CountDownLatch done = new CountDownLatch(1);
        final AtomicReference<String> result = new AtomicReference<>();

        FG.execute(() -> {
            assertEquals("main", S.get());
            r.once("K", (Do.Execute<String>) next -> {
                assertEquals("worker", S.get());
                try {
                    Thread.sleep(16);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    fail("interrupted");
                }
                next.got("sadflkj zxcvlkj asdf");
            }).begin(s -> {
                assertEquals("main", S.get());
                result.set(s);
                done.countDown();
            });
        });

        done.await();
        assertEquals(cache.get("K", null), result.get());
    }

}