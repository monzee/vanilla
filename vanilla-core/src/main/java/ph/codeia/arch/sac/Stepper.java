package ph.codeia.arch.sac;

import java.util.Iterator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.RunnableFuture;

/**
 * This file is a part of the vanilla project.
 */

public class Stepper<
        S extends State<S, A>,
        A extends ph.codeia.arch.sac.Action<S, A, C>,
        C>
extends ph.codeia.arch.sac.Unit<S, A, C> {
    public Stepper(S state) {
        super(state);
    }

    @Override
    public void apply(Executor worker, C client, A action) {
        apply(client, action);
    }

    @Override
    public void handle(Throwable error, C client) {
        throw new RuntimeException(error);
    }

    public void apply(C client, A action) {
        state = action.fold(state, client);
    }

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
            return false;
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    public void drain(C client) {
        while (step(client));
    }
}
