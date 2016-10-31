package ph.codeia.values;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/**
 * This file is a part of the vanilla project.
 */

public class LazyTest {

    @Test
    public void straw_man() {
        assertEquals("foo", Lazy.of(() -> "foo").get());
    }

    @Test
    public void factory_is_only_ever_called_once() {
        AtomicInteger counter = new AtomicInteger();
        Lazy<Integer> lazy = Lazy.of(counter::incrementAndGet);
        assertEquals(1, lazy.get().intValue());
        assertEquals(1, lazy.get().intValue());
        assertEquals(1, lazy.get().intValue());
        assertEquals(1, lazy.get().intValue());
        assertEquals(1, lazy.get().intValue());
    }

}