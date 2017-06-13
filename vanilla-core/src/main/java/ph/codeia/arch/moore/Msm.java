package ph.codeia.arch.moore;

/*
 * This file is a part of the vanilla project.
 */

import java.util.concurrent.Future;

/**
 * Moore state machine.
 * <p>
 * A Moore machine differs from the Mealy machine in one aspect: the entire
 * output can be derived from the state. A transition therefore is just a
 * function from state to state and there exists a function that can generate
 * an output from the state.</p>
 * <p>
 * In this implementation, the state is implicit
 * </p>
 */
public interface Msm {

    interface Action<A extends Action<A, E>, E extends Effect<A, E>> {
        /**
         * The body of the implementation should contain just a single call to
         * one of the methods of E.
         *
         * @param e the selector/pattern object.
         */
        void apply(E e);
    }

    interface Effect<A extends Action<A, E>, E extends Effect<A, E>> {
        void fold(Iterable<A> backlog);
    }

    interface Machine<A extends Action<A, E>, E extends Effect<A, E>> {
        /**
         * Sets the machine's current state but does not execute the action.
         *
         * @param newState the new state of the machine
         * @return false to cancel the transition.
         */
        boolean moveTo(A newState);

        /**
         * Sets the current state and executes the action to produce an output.
         *
         * @param newState the action to execute
         */
        void exec(A newState);

        /**
         * Remembers the current state and awaits the future action in the
         * background then executes it in the main thread.
         * <p>
         * This is meant to be called inside an {@link Effect} method that takes
         * a future action argument.</p>
         *
         * @param task the future state to execute.
         */
        void await(Future<A> task);

        /**
         * Cancels the tasks being awaited and returns an action that receives
         * an iterable of the actions that generated the currently pending
         * tasks.
         *
         * @return an action that calls {@link Effect#fold(Iterable)} with the
         * action iterable.
         */
        A stop();

        /**
         * @return the current state of/last action executed by the machine.
         */
        A peek();
    }
}
