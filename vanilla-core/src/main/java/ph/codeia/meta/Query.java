package ph.codeia.meta;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This file is a part of the vanilla project.
 */

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Inherited
@Experimental
public @interface Query {

    /**
     * @return path to append to Uri
     */
    String value() default "";

    /**
     * maps a row column to a query field
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.SOURCE)
    @Inherited
    @interface Select {
        String value();
    }

    enum Position { BEFORE, AFTER }

    /**
     * literal row filter, no substitutions
     */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.SOURCE)
    @Inherited
    @interface Where {
        /**
         * @return clauses to be concatenated with AND
         */
        String[] value();

        /**
         * @return where this clause should be placed if other predicates are
         * defined on fields
         */
        Position pos() default Position.BEFORE;

        /**
         * generates {@code '(AND) COL = ?'} for every value
         */
        @Target(ElementType.FIELD)
        @Retention(RetentionPolicy.SOURCE)
        @Inherited
        @interface Eq {
            /**
             * @return column to test for equality
             */
            String value();
            String conj() default "AND";
        }

        /**
         * generates {@code '(AND) COL < ?'} for every value
         */
        @Target(ElementType.FIELD)
        @Retention(RetentionPolicy.SOURCE)
        @Inherited
        @interface Lt {
            /**
             * @return column to test for equality
             */
            String value();
            String conj() default "AND";
        }

        /**
         * generates {@code '(AND) COL > ?'} for every value
         */
        @Target(ElementType.FIELD)
        @Retention(RetentionPolicy.SOURCE)
        @Inherited
        @interface Gt {
            /**
             * @return column to test for equality
             */
            String value();
            String conj() default "AND";
        }

        /**
         * generates {@code '(AND) COL <= ?'} for every value
         */
        @Target(ElementType.FIELD)
        @Retention(RetentionPolicy.SOURCE)
        @Inherited
        @interface Lte {
            /**
             * @return column to test for equality
             */
            String value();
            String conj() default "AND";
        }

        /**
         * generates {@code '(AND) COL >= ?'} for every value
         */
        @Target(ElementType.FIELD)
        @Retention(RetentionPolicy.SOURCE)
        @Inherited
        @interface Gte {
            /**
             * @return column to test for equality
             */
            String value();
            String conj() default "AND";
        }

        /**
         * generates {@code '(AND) COL <> ?'} for every value
         */
        @Target(ElementType.FIELD)
        @Retention(RetentionPolicy.SOURCE)
        @Inherited
        @interface NotEq {
            String value();
            String conj() default "AND";
        }

        /**
         * generates {@code '(AND) COL in (?, ?, ?)'} for every value
         */
        @Target(ElementType.FIELD)
        @Retention(RetentionPolicy.SOURCE)
        @Inherited
        @interface In {
            String value();
            String conj() default "AND";
        }

        /**
         * generates {@code '(AND) COL not in (?, ?, ?)'} for every value
         */
        @Target(ElementType.FIELD)
        @Retention(RetentionPolicy.SOURCE)
        @Inherited
        @interface NotIn {
            String value();
            String conj() default "AND";
        }

        /**
         * generates {@code '(AND) COL like ?'} for every value
         */
        @Target(ElementType.FIELD)
        @Retention(RetentionPolicy.SOURCE)
        @Inherited
        @interface Like {
            String value();
            String conj() default "AND";
        }

        /**
         * generates {@code '(AND) COL not like ?'} for every value
         */
        @Target(ElementType.FIELD)
        @Retention(RetentionPolicy.SOURCE)
        @Inherited
        @interface NotLike {
            String value();
            String conj() default "AND";
        }

        /**
         * generates {@code '(AND) COL is NULL'} for every value
         */
        @Target(ElementType.FIELD)
        @Retention(RetentionPolicy.SOURCE)
        @Inherited
        @interface Null {
            String conj() default "AND";
        }

        /**
         * generates {@code '(AND) COL is not NULL'} for every value
         */
        @Target(ElementType.FIELD)
        @Retention(RetentionPolicy.SOURCE)
        @Inherited
        @interface NotNull {
            String conj() default "AND";
        }
    }

    /**
     * row sorting fields
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.SOURCE)
    @Inherited
    @interface Order {
        /**
         * @return sort priority; higher comes first
         */
        int value() default -1;

        /**
         * generates {@code 'COL DESC'} for every field
         */
        @Target(ElementType.FIELD)
        @Retention(RetentionPolicy.SOURCE)
        @Inherited
        @interface Descending {
            /**
             * @return sort priority; higher comes first
             */
            int value() default -1;
        }
    }
}
