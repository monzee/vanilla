package ph.codeia.arch.sm;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
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

        @Override
        protected void main(Runnable block) {
            block.run();
        }
    }

    interface Action extends Sm.Action<State, Action, MachineTest> {}

    static class State extends RootState<State, Action> {
        int counter = 0;
        boolean hit;

        State expect(int expected) {
            assertEquals(expected, counter);
            return this;
        }

    }

    static final Action NOOP = (s, r) -> s;
    static final ExecutorService BG = Executors.newSingleThreadExecutor();

    private Machine.Bound<State, Action, MachineTest> test;

    static Action inc(int n) {
        return (s, r) -> s.apply(() -> s.counter += n);
    }

    static Action hit(CountDownLatch done) {
        done.countDown();
        return NOOP;
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
        test.apply((s, r) -> s.plus(inc(1)));
        assertFalse(test.machine.state().hit);
        test.apply((s, r) -> {
            s.hit = true;
            return s.expect(1);
        });
        assertTrue(test.machine.state().hit);
    }

    @Test
    public void actions_receive_the_current_state_of_the_machine() {
        State state = new State();
        Machine<State, Action, MachineTest> machine = new Impl<>(state);
        machine.apply(Machine.IMMEDIATE, this, (s, r) -> {
            assertSame(s, state);
            return s;
        });
    }

    @Test
    public void actions_can_change_the_state_of_the_machine() {
        State newState = new State();
        test.apply((s, r) -> {
            assertNotSame(s, newState);
            return newState;
        });
        assertSame(newState, test.machine.state());
    }

    @Test
    public void actions_receive_the_receiver_instance_passed_to_apply() {
        Machine<State, Action, MachineTest> machine = test.machine;
        machine.apply(Machine.IMMEDIATE, this, (s, r) -> {
            assertSame(r, this);
            return s.plus(inc(1));
        });
        machine.apply(Machine.IMMEDIATE, (MachineTest) null, (s, r) -> {
            assertNull(r);
            return s.plus(inc(10));
        });
        test.apply((s, r) -> s.expect(11));
    }

    @Test
    public void actions_get_enqueued_when_applied_while_stopped() {
        test.stop();
        test.apply((s, r) -> s.apply(() -> s.hit = true));
        assertFalse(test.machine.state().hit);
    }

    @Test
    public void queued_actions_get_applied_when_started() {
        test.stop();
        test.apply((s, r) -> s.apply(() -> s.hit = true));
        test.start();
        assertTrue(test.machine.state().hit);
    }

    @Test(timeout = 1000)
    public void async_actions_that_finish_while_stopped_get_applied_when_started()
            throws InterruptedException, BrokenBarrierException {
        State state = new State();
        Machine.Bound<State, Action, MachineTest> bg =
                new Machine.Bound<>(BG, this, new Impl<>(state));
        CyclicBarrier barrier = new CyclicBarrier(2);
        bg.apply((s, r) -> s.async(() -> {
            barrier.await();
            return inc(1);
        }).async(() -> inc(10)).async(() -> {
            barrier.await();
            return NOOP;
        }));
        bg.stop();
        barrier.await();  // first async is unblocked
        barrier.await();  // block until third async returns
        assertEquals(0, state.counter);
        CountDownLatch started = new CountDownLatch(1);
        bg.start(hit(started));
        started.await();
        assertEquals(11, state.counter);
    }

    @Test
    public void multiple_queued_actions_run_only_once() {
        test.apply((s, r) -> s.plus(inc(1)).plus(inc(10)).plus(inc(100)));
        test.apply((s, r) -> s.expect(111));
    }

    @Test(timeout = 1000)
    public void multiple_queued_async_actions_run_only_once() throws InterruptedException {
        Machine.Bound<State, Action, MachineTest> bg =
                new Machine.Bound<>(BG, this, new Impl<>(new State()));
        CountDownLatch done = new CountDownLatch(3);
        bg.apply((s, r) -> s.async(() -> {
            done.countDown();
            return inc(1);
        }).async(() -> {
            done.countDown();
            return inc(10);
        }).async(() -> {
            done.countDown();
            return inc(100);
        }));
        done.await();
        bg.apply((s, r) -> s.expect(111));
    }
}
