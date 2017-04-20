package ph.codeia.arch.sm;

import ph.codeia.meta.Untested;

/**
 * This file is a part of the vanilla project.
 */

@Untested
public class Backlog {
    private final Object lock = new Object();
    private int inFlight = 0;

    public void started() {
        synchronized (lock) {
            inFlight++;
        }
    }

    public void done() {
        if (inFlight == 0) {
            throw new IllegalStateException("No pending work.");
        }
        synchronized (lock) {
            if (--inFlight == 0) {
                lock.notifyAll();
            }
        }
    }

    public void await() throws InterruptedException {
        synchronized (lock) {
            while (inFlight > 0) {
                lock.wait();
            }
        }
    }
}
