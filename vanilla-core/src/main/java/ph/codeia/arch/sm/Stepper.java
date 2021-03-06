package ph.codeia.arch.sm;

import java.util.Iterator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.RunnableFuture;

import ph.codeia.arch.ErrorHandler;
import ph.codeia.meta.Untested;

/**
 * This file is a part of the vanilla project.
 */

/**
 * A machine that synchronously applies actions one at a time.
 *
 * Meant to be used in tests so that you could do assertions in between async
 * actions.
 *
 * @param <S> The state type.
 * @param <A> The action type.
 * @param <C> The receiver type.
 */
@Untested
public class Stepper<
        S extends Sm.State<S, A>,
        A extends Sm.Action<S, A, C>,
        C>
extends Machine<S, A, C> {

    private final ErrorHandler<C> errorHandler;

    public Stepper(S state) {
        this(state, null);
    }

    public Stepper(S state, ErrorHandler<C> errorHandler) {
        super(state);
        this.errorHandler = errorHandler;
    }

    @Override
    public void apply(Executor worker, C client, A action) {
        apply(client, action);
    }

    @Override
    public void handle(Throwable error, C client) {
        if (errorHandler != null) {
            errorHandler.handle(error, client);
        } else {
            throw new RuntimeException(error);
        }
    }

    @Override
    protected void main(Runnable block) {
        block.run();
    }

    /**
     * Computes a new state but does not process the resulting future queue.
     *
     * @param client The output channel.
     * @param action The action to fold into the current state.
     */
    public void apply(C client, A action) {
        state = action.fold(state, client);
    }

    /**
     * Takes one future action and calls {@code #apply(C, A)} with it.
     *
     * @param client The output channel.
     * @return false if the future queue is empty; true otherwise.
     */
    public boolean step(C client) {
        Iterator<Future<A>> it = state.iterator();
        if (!it.hasNext()) {
            return false;
        }
        try {
            Future<A> future = it.next();
            it.remove();
            if (future instanceof RunnableFuture) {
                ((RunnableFuture) future).run();
            }
            apply(client, future.get());
            return true;
        } catch (InterruptedException | ExecutionException e) {
            handle(e, client);
            return true;
        }
    }

    /**
     * Folds all queued future actions into state.
     *
     * @param client The output channel.
     */
    @SuppressWarnings("StatementWithEmptyBody")
    public void drain(C client) {
        while (step(client));
    }
}
