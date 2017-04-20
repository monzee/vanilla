package ph.codeia.query;

import java.util.Collection;

/**
 * This file is a part of the vanilla project.
 */

public interface Queryable {

    <T> Results<T> run(Params in, Row.Mapper<T> out);

    interface Runner<T> {
        /**
         * Runs the query and returns an iterable result wrapper.
         *
         * @param source The data source to query.
         * @return an iterable containing the rows satisfying the query. If the
         * query class does not implement {@link Template}, the original query
         * object will be reused for each row. You should implement {@link
         * Template} if you want to copy the rows directly into a collection.
         */
        Results<T> query(Queryable source);

        /**
         * Runs the query and copies the results into a collection.
         * <p>
         * The query class must implement {@link Template} or else the
         * collection will contain n copies of the last row.</p>
         *
         * @param source The data source to query.
         * @param sink The destination collection.
         * @return false if the query failed. An empty result set doesn't imply
         * failure.
         */
        boolean drain(Queryable source, Collection<? super T> sink);
    }
}
