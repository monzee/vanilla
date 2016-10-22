package ph.codeia.signal;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

/**
 * This file is a part of the vanilla project.
 */

public class ReplayTest {

    @Test
    public void straw_man() {
        AtomicReference<String> client = new AtomicReference<>();
        Channel<String> ch = new Replay<>(new SimpleChannel<>());
        ch.send("foo");
        ch.send("bar");
        ch.send("baz");
        ch.link(client::set);
        assertEquals("baz", client.get());
    }

    @Test
    public void receives_messages_sent_by_inner_channel() {
        Channel<String> inner = new SimpleChannel<>();
        Channel<String> ch = new Replay<>(inner);
        AtomicReference<String> client = new AtomicReference<>();
        ch.link(client::set);
        inner.send("asdf");
        assertEquals("asdf", client.get());
    }

    @Test
    public void inner_clients_receives_messages_from_decorator() {
        Channel<String> inner = new SimpleChannel<>();
        Channel<String> ch = new Replay<>(inner);
        AtomicReference<String> client = new AtomicReference<>();
        inner.link(client::set);
        ch.send("asdf");
        assertEquals("asdf", client.get());
    }

    @Test
    public void does_not_replay_messages_sent_through_the_inner_channel() {
        Channel<String> inner = new SimpleChannel<>();
        Channel<String> ch = new Replay<>(inner);
        AtomicReference<String> client = new AtomicReference<>();
        ch.send("xzcv");
        inner.send("asdf");
        ch.link(client::set);
        assertNotEquals("asdf", client.get());
    }

}
