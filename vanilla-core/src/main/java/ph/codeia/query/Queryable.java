package ph.codeia.query;

/**
 * This file is a part of the vanilla project.
 */

public interface Queryable<T> {
    Results<T> query(Params params);
}
