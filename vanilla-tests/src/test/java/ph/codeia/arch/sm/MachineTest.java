package ph.codeia.arch.sm;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.*;

/**
 * This file is a part of the vanilla project.
 */

public class MachineTest {

    static class Impl<
            S extends Sm.State<S, A>,
            A extends Sm.Action<S, A, R>,
            R>
    extends Machine<S, A, R> {
        protected Impl(S state) {
            super(state);
            isRunning = true;
        }

        @Override
        public void handle(Throwable error, R client) {
            assert error instanceof RuntimeException;
            throw (RuntimeException) error;
        }
    }

    interface Action extends Sm.Action<State, Action, MachineTest> {}

    static class State extends RootState<State, Action> {
        int counter = 0;
    }

    static final ExecutorService BG = Executors.newSingleThreadExecutor();

    private Machine.Bound<State, Action, MachineTest> test;

    State expect(int expected, State s) {
        assertEquals(expected, s.counter);
        return s;
    }

    static Action inc(int n) {
        return (s, a) -> s.apply(() -> s.counter += n);
    }

    static Action hit(CountDownLatch done) {
        done.countDown();
        return (s, a) -> s;
    }

    @Before
    public void setup() {
        test = new Machine.Bound<>(Machine.IMMEDIATE, this, new Impl<>(new State()));
    }

    @AfterClass
    public static void tearDown() {
        BG.shutdown();
    }

    @Test
    public void strawman() {
        test.apply((s, a) -> s.plus(inc(1)));
        test.apply((s, a) -> a.expect(1, s));
    }

    @Test
    public void multiple_queued_actions_run_only_once() {
        test.apply((s, a) -> s.plus(inc(1)).plus(inc(10)).plus(inc(100)));
        test.apply((s, a) -> a.expect(111, s));
    }

    @Test(timeout = 1000)
    public void multiple_queued_actions_in_background_run_only_once() throws InterruptedException {
        Machine.Bound<State, Action, MachineTest> bg =
                new Machine.Bound<>(BG, this, new Impl<>(new State()));
        CountDownLatch done = new CountDownLatch(1);
        bg.apply((s, a) -> s.plus(inc(1)).plus(inc(10)).plus(inc(100)).async(() -> hit(done)));
        done.await();
        bg.apply((s, a) -> a.expect(111, s));
    }
}
