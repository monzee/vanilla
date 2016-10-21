package ph.codeia.values;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * This file is a part of the vanilla project.
 */

public class SimpleStoreTest {

    @Test
    public void straw_man() {
        Store s = new SimpleStore();
        s.put("a", "AAA");
        s.put("b", 123);
        s.put("c", true);
        assertEquals("AAA", s.get("a", "not AAA"));
        assertEquals(123, s.get("b", 0).intValue());
        assertTrue(s.get("c", false));
    }

    @Test
    public void put_overwrites() {
        Store s = new SimpleStore();
        s.put("a", "orig");
        assertEquals("orig", s.get("a", null));
        s.put("a", "changed");
        assertEquals("changed", s.get("a", null));
    }

    @Test
    public void softPut_does_not_overwrite() {
        Store s = new SimpleStore();
        s.softPut("a", () -> "orig");
        assertEquals("orig", s.get("a", null));
        s.softPut("a", () -> "changed");
        assertNotEquals("changed", s.get("a", null));
    }

    @Test
    public void get_default_value() {
        Store s = new SimpleStore();
        assertEquals("abc", s.get("key", "abc"));
        assertEquals("def", s.get("key", "def"));
    }

    @Test
    public void hardGet_makes_future_gets_return_the_same_value() {
        Store s = new SimpleStore();
        assertEquals("abc", s.hardGet("k", () -> "abc"));
        assertEquals("abc", s.get("k", "def"));
    }

}