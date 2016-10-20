package ph.codeia.values;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * This file is a part of the vanilla project.
 */

public class Either<E extends Throwable, T> implements Do.Try<T> {

    public static <T> Do.Try<T> ok(T value) {
        Either<?, T> either = new Either<>();
        either.pass(value);
        return either;
    }

    public static <T> Do.Try<T> error(Throwable error) {
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

    public synchronized void fail(E error) {
        if (state == State.INCOMPLETE) {
            this.error = error;
            state = State.ERROR;
            notifyAll();
        }
    }

    public synchronized void pass(T value) {
        if (state == State.INCOMPLETE) {
            this.value = value;
            state = State.OK;
            notifyAll();
        }
    }

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
