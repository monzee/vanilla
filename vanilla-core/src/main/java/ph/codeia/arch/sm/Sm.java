package ph.codeia.arch.sm;

import java.util.concurrent.Future;

/**
 * This file is a part of the vanilla project.
 */

public interface Sm {

    /**
     * A snapshot of the state machine.
     *
     * The state is the accumulation of all events or actions applied to the
     * system since it was started. It should define all the possible actions
     * that may be applied to the system at this point in time along with some
     * data that might be used to make that decision.
     *
     * The state also holds a queue of future actions that are waiting to be
     * applied by the machine. An action that produces a state might be split
     * into two parts by an {@link #async(Future)} call. The state keeps track
     * of running actions in the background and receives the completed future
     * when they return. The completed future is then folded into the state
     * by the machine if/when it is started.
     *
     * @param <S> The instantiated state type.
     * @param <A> The instantiated action type.
     */
    interface State<
            S extends State<S, A>,
            A extends Action<S, A, ?>>
    extends Iterable<Future<A>> {
        /**
         * @param futureAction The action to fold in a future step.
         * @return the same instance.
         */
        S async(Future<A> futureAction);

        /**
         * @return an object that's kinda like a reverse semaphore, or a
         * reusable countdown latch that can be incremented at any time. Used
         * when a machine is started to get notified when past async actions
         * have completed so that they may be folded by the new machine.
         */
        Backlog backlog();
    }

    /**
     * Encapsulates the input to the state machine.
     *
     * The input is used to compute the new state of the system and an output
     * which is manifested in the receiver object. The receiver contract is
     * unspecified. The action is free to do whatever it wants to the receiver.
     *
     * @param <S> The instantiated state type.
     * @param <A> The instantiated action type.
     * @param <C> The receiver type.
     */
    interface Action<
            S extends State<S, A>,
            A extends Action<S, A, C>,
            C> {
        /**
         * @param from The current state of the machine.
         * @param client The receiver object.
         * @return a new state object.
         */
        S fold(S from, C client);
    }

}
