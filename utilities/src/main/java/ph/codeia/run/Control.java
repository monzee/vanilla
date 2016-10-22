package ph.codeia.run;

import ph.codeia.meta.Experimental;

/**
 * This file is a part of the vanilla project.
 */

@Experimental
public enum Control {

    JUMP, BREAK, CONTINUE;

    private static final ThreadLocal<Control> FLAG = new ThreadLocal<>();
    private static final ThreadLocal<String> LABEL = new ThreadLocal<>();

    public static Control status() {
        return FLAG.get();
    }

    public static void clear() {
        FLAG.remove();
        LABEL.remove();
    }

    public static String target() {
        return LABEL.get();
    }

    public void signal() {
        FLAG.set(this);
    }

    public void signal(String target) {
        FLAG.set(this);
        LABEL.set(target);
    }

    public boolean signalled() {
        return this == FLAG.get();
    }

}
