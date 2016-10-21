package ph.codeia.values;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * This file is a part of the vanilla project.
 */

/**
 * An implementation of {@link Do.Try} that can be converted to a
 * {@link Future}.
 *
 * Calls to {@link #get()} will block the thread until a value or an error
 * is set.
 *
 * @param <E> The error type
 * @param <T> The success type
 */
public class Either<E extends Throwable, T> implements Do.Try<T> {

    /**
     * Creates an Either from a {@link Callable}.
     */
    public static <T> Either<Exception, T> of(Callable<T> value) {
        Either<Exception, T> either = new Either<>();
        try {
            either.pass(value.call());
        } catch (Exception e) {
            either.fail(e);
        }
        return either;
    }

    public static <T> Either<?, T> ok(T value) {
        Either<?, T> either = new Either<>();
        either.pass(value);
        return either;
    }

    public static <T> Either<Throwable, T> error(Throwable error) {
        Either<Throwable, T> either = new Either<>();
        either.fail(error);
        return either;
    }

    private enum State { INCOMPLETE, ERROR, OK }

    private State state = State.INCOMPLETE;
    private E error;
    private T value;

    @Override
    public T get() throws E, InterruptedException {
        while (true) {
            switch (state) {
                case OK:
                    return value;
                case ERROR:
                    throw error;
                case INCOMPLETE:
                    synchronized (this) {
                        wait();
                    }
            }
        }
    }

    /**
     * Completes the computation with an error.
     */
    public synchronized void fail(E error) {
        if (state == State.INCOMPLETE) {
            this.error = error;
            state = State.ERROR;
            notifyAll();
        }
    }

    /**
     * Completes the computation.
     */
    public synchronized void pass(T value) {
        if (state == State.INCOMPLETE) {
            this.value = value;
            state = State.OK;
            notifyAll();
        }
    }

    /**
     * Creates a future.
     *
     * @return {@link Future#cancel(boolean) cancel} does nothing. Mostly useful
     *         for the {@link Future#get(long, TimeUnit) timed get} and
     *         compatibility with other code that consumes futures.
     */
    public Future<T> toFuture() {
        return new Future<T>() {
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                return false;
            }

            @Override
            public boolean isCancelled() {
                return false;
            }

            @Override
            public boolean isDone() {
                return state != State.INCOMPLETE;
            }

            @Override
            public T get() throws InterruptedException, ExecutionException {
                try {
                    return Either.this.get();
                } catch (InterruptedException e) {
                    throw e;
                } catch (Throwable e) {
                    throw new ExecutionException(e);
                }
            }

            @Override
            public T get(long timeout, TimeUnit unit)
                    throws InterruptedException, ExecutionException, TimeoutException
            {
                if (timeout <= 0) {
                    return get();
                }
                long remaining = unit.toNanos(timeout);
                while (true) {
                    switch (state) {
                        case OK:
                            return value;
                        case ERROR:
                            throw new ExecutionException(error);
                        case INCOMPLETE:
                            if (remaining <= 0) {
                                throw new TimeoutException();
                            }
                            synchronized (Either.this) {
                                long aWhileAgo = System.nanoTime();
                                TimeUnit.NANOSECONDS.timedWait(Either.this, remaining);
                                long elapsed = System.nanoTime() - aWhileAgo;
                                remaining -= elapsed;
                            }
                    }
                }
            }
        };
    }

}
