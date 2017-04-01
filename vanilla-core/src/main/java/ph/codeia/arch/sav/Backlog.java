package ph.codeia.arch.sav;

/**
 * This file is a part of the vanilla project.
 */

public class Backlog {
    private final Object lock = new Object();
    private volatile int inFlight = 0;

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
