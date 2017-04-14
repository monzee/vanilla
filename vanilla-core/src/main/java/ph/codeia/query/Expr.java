package ph.codeia.query;

/**
 * This file is a part of the vanilla project.
 */

public interface Expr {
    void match(Case node);

    interface Case {
        void identifier(String id);
        void phrase(String sym);
        void value(Object value);
        void conjunction(String sym, Expr left, Expr right);
        void enumeration(Expr... components);
        void association(String name, Expr value);
        void blank();
    }
}
