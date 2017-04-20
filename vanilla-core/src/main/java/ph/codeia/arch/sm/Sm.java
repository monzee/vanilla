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
     * into two parts by an {@link #async(Future)} call. The synchronous half is
     * immediately applied. The other half is run and synchronously awaited by
     * the machine in a background thread and then applied in the main thread
     * afterwards. It can be viewed as a transition that produces an output that
     * is asynchronously fed back to the machine.
     *
     * Since the machine might be stopped and/or abandoned while an action is
     * in flight, the state has to track the number of running actions in the
     * background which a future machine may observe in a background thread when
     * it starts. For this to work, all state objects must share the same future
     * action queue and backlog tracker. This can be accomplished by 1) using a
     * single mutable state object throughout, or 2) making new states copy or
     * share the previous state's future queue and backlog. The former is
     * simpler, but the default implementation provides a way to do the latter
     * if you really want immutable states.
     *
     * Recommendation: use the root state as a mutable container of a product
     * of immutable states (enums or visitor types). The root state can also
     * hold miscellaneous data associated with some of the substates if you
     * decide to use enums as state which cannot have their own data unlike
     * visitor types or sum types in functional languages.
     *
     * @param <S> The instantiated state type.
     * @param <A> The instantiated action type.
     * @see RootState for the default implementation which also adds some
     * convenience functions.
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
         * @return an object that's kinda like a reversible countdown latch that
         * can be incremented at any time. Used when a machine is started to get
         * notified when past async actions have completed so that they may be
         * folded by a future machine.
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
