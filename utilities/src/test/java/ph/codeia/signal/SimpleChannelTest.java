package ph.codeia.signal;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

/**
 * This file is a part of the vanilla project.
 */

public class SimpleChannelTest {

    @Test
    public void straw_man() {
        AtomicReference<String> msg = new AtomicReference<>();
        Channel<String> ch = new SimpleChannel<>();
        ch.link(msg::set);
        ch.send("foo");
        assertEquals("foo", msg.get());
    }

    @Test
    public void stops_receiving_messages_when_unlinked() {
        AtomicInteger msg1 = new AtomicInteger();
        AtomicInteger msg2 = new AtomicInteger();
        Channel<Integer> ch = new SimpleChannel<>();
        Channel.Link l = ch.link(msg1::set);
        ch.link(msg2::set);

        ch.send(100);
        l.unlink();
        ch.send(1024);

        assertEquals(100, msg1.get());
        assertEquals(1024, msg2.get());
    }

    @Test
    public void unlink_all() {
        int N = 10;
        Channel<Integer> ch = new SimpleChannel<>();
        AtomicInteger[] clients = new AtomicInteger[N];
        for (int i = 0; i < N; i++) {
            clients[i] = new AtomicInteger();
            ch.link(clients[i]::set);
        }

        ch.send(123);
        for (AtomicInteger client : clients) {
            assertEquals(123, client.get());
        }

        ch.unlinkAll();
        ch.send(456);
        for (AtomicInteger client : clients) {
            assertNotEquals(456, client.get());
        }
    }

}
