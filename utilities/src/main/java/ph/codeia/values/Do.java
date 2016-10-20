package ph.codeia.values;

/**
 * This file is a part of the vanilla project.
 */

public interface Do {

    interface Try<T> {
        T get() throws Throwable;
    }

    interface Make<T> extends Try<T> {
        T get();
    }

    interface Map<T, U> {
        U from(T value) throws Throwable;
    }

    interface Convert<T, U> extends Map<T, U> {
        U from(T value);
    }

    interface Combine2<T, A, B> {
        T from(A first, B second);
    }

    interface Combine3<T, A, B, C> {
        T from(A first, B second, C third);
    }

    interface Combine4<T, A, B, C, D> {
        T from(A first, B second, C third, D fourth);
    }

    interface Just<T> {
        void got(T value);
    }

    interface Maybe<T> extends Just<Try<T>> {}

    interface Executable<T> {
        void execute(Just<T> next);
    }

    interface Continue<T, U> {
        void then(T value, Just<U> next);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    class Seq<T, U> implements Executable<U> {

        private static final Executable<Void> NIL = next -> next.got(null);

        private static final Just NOOP = ignored -> {};

        public static <T> Seq<?, T> start(Executable<T> block) {
            return new Seq<>(NIL, (ignored, next) -> block.execute(next));
        }

        private final Executable<T> prev;
        private final Continue<T, U> step;

        private Seq(Executable<T> prev, Continue<T, U> step) {
            this.prev = prev;
            this.step = step;
        }

        public <V> Seq<U, V> andThen(Continue<U, V> next) {
            return new Seq<>(this, next);
        }

        @Override
        public void execute(Just<U> next) {
            prev.execute(t -> step.then(t, next));
        }

        public void execute() {
            execute(NOOP);
        }

    }

}
