package ph.codeia.run;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import ph.codeia.values.Do;

import static org.junit.Assert.*;

/**
 * This file is a part of the vanilla project.
 */

public class JumpTest {

    @Test
    public void straw_man() {
        LabelledRunner r = new Jump();
        final AtomicInteger count = new AtomicInteger(0);
        final AtomicInteger resetMe = new AtomicInteger(0);
        AtomicBoolean called = new AtomicBoolean(false);
        Do.Execute<Void> start = next -> {
            resetMe.set(0);
            next.got(null);
        };
        Do.Continue<Void, Void> inc = (x, next) -> {
            count.incrementAndGet();
            resetMe.incrementAndGet();
            next.got(null);
        };

        Seq.of(start).pipe(inc).pipe(inc).pipe(inc).begin(r.run(value -> {
            assertEquals(3, count.get());
            assertEquals(3, resetMe.get());
        }));

        count.set(0);
        resetMe.set(0);

        Seq.of(r.label("start", start))
                .pipe(inc).pipe(inc).pipe(inc)
                .<Void> pipe((x, next) -> {
                    if (!called.get()) {
                        Control.JUMP.signal("start");
                        called.set(true);
                    }
                    next.got(null);
                }).begin(r.run(value -> {
                    assertEquals(6, count.get());
                    assertEquals(3, resetMe.get());
                }));
    }

}
