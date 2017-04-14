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

    enum Position { FIRST, LAST }

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
        Position pos() default Position.FIRST;

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
        }

        /**
         * generates {@code '(AND) COL <> ?'} for every value
         */
        @Target(ElementType.FIELD)
        @Retention(RetentionPolicy.SOURCE)
        @Inherited
        @interface NotEq {
            String value();
        }

        /**
         * generates {@code '(AND) COL in (?, ?, ?)'} for every value
         */
        @Target(ElementType.FIELD)
        @Retention(RetentionPolicy.SOURCE)
        @Inherited
        @interface In {
            String value();
        }

        /**
         * generates {@code '(AND) COL not in (?, ?, ?)'} for every value
         */
        @Target(ElementType.FIELD)
        @Retention(RetentionPolicy.SOURCE)
        @Inherited
        @interface NotIn {
            String value();
        }

        /**
         * generates {@code '(AND) COL like ?'} for every value
         */
        @Target(ElementType.FIELD)
        @Retention(RetentionPolicy.SOURCE)
        @Inherited
        @interface Like {
            String value();
        }

        /**
         * generates {@code '(AND) COL not like ?'} for every value
         */
        @Target(ElementType.FIELD)
        @Retention(RetentionPolicy.SOURCE)
        @Inherited
        @interface NotLike {
            String value();
        }

        /**
         * generates {@code '(AND) COL is NULL'} for every value
         */
        @Target(ElementType.TYPE)
        @Retention(RetentionPolicy.SOURCE)
        @Inherited
        @interface Null {}

        /**
         * generates {@code '(AND) COL is not NULL'} for every value
         */
        @Target(ElementType.TYPE)
        @Retention(RetentionPolicy.SOURCE)
        @Inherited
        @interface NotNull {}
    }

    /**
     * row sorting fields
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.SOURCE)
    @Inherited
    @interface Order {
        /**
         * @return sort priority
         */
        int value() default -1;

        /**
         * generates {@code 'COL DESC'} for every field
         */
        @Target(ElementType.FIELD)
        @Retention(RetentionPolicy.SOURCE)
        @interface Descending {
            /**
             * @return sort priority
             */
            int value() default -1;
        }
    }

    interface Template<T extends Template<T>> {
        T copy();
    }

    interface Result<T> extends Iterable<T> {
        boolean isEmpty();
    }

    interface Row {
        int getInt(int column);
        String getString(int column);
        byte[] getBlob(int column);
        float getFloat(int column);
        long getLong(int column);
        short getShort(int column);
    }

    @Query("posts")
    class BlogPostByAuthor implements Template<BlogPostByAuthor> {
        @Where.Eq("AuthorId") final int authorId;

        @Select("_ID") int id;
        @Select("Title") String title;
        @Select("Body") String body;
        @Select("Date") @Order.Descending long timeStamp;

        public BlogPostByAuthor(int authorId) {
            this.authorId = authorId;
        }

        @Override
        public BlogPostByAuthor copy() {
            return new BlogPostByAuthor(authorId);
        }
    }

    // Query.Result<BlogPostByAuthor> results = GenerateQuery
    //      .from(new BlogPostByAuthor(123))
    //      .query(new AndroidContent(getContentResolver(), contentUri));

    class BlogPostByAuthor_GeneratedQuery {
        private final BlogPostByAuthor receiver;
        private static final int ARGC = 1;

        public BlogPostByAuthor_GeneratedQuery(BlogPostByAuthor receiver) {
            this.receiver = receiver;
        }

        public String dataset() {
            return "posts";
        }

        public String[] projection() {
            return new String[] {"_ID", "Title", "Body", "Date"};
        }

        public String selection() {
            StringBuilder selection = new StringBuilder();
            selection.append(" AND AuthorId = ?");
            return selection.substring(5);
        }

        public String[] selectionArgs() {
            String[] stringArgs = new String[ARGC];
            if (receiver != null) {
                stringArgs[0] = String.valueOf(receiver.authorId);
            }
            return stringArgs;
        }

        public String sortOrder() {
            StringBuilder order = new StringBuilder();
            order.append(", Date DESC");
            return order.substring(2);
        }

        public BlogPostByAuthor map(Row cursor) {
            BlogPostByAuthor row = receiver.copy();
            row.id = cursor.getInt(0);
            row.title = cursor.getString(1);
            row.body = cursor.getString(2);
            row.timeStamp = cursor.getLong(3);
            return row;
        }
    }
}
