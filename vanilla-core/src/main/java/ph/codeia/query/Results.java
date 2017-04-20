package ph.codeia.query;

/**
 * This file is a part of the vanilla project.
 */

public interface Results<T> extends Iterable<T> {

    boolean ok();
    int count();
    void dispose();

}
