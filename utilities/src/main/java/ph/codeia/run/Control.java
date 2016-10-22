package ph.codeia.run;

/**
 * This file is a part of the vanilla project.
 */

public enum Control {

    BRANCH, BREAK, CONTINUE;

    private static final ThreadLocal<Control> CURRENT = new ThreadLocal<>();

    public static Control status() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }

    public void signal() {
        CURRENT.set(this);
    }

    public boolean signalled() {
        return this == CURRENT.get();
    }

}
