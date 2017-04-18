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
public @interface Command {
    /**
     * @return table/key to execute command on
     */
    String value() default "";

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.SOURCE)
    @Inherited
    @interface Key {
        /**
         * @return primary key name
         */
        String value();
    }

    /**
     * must be attached to a field of type Delta
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.SOURCE)
    @Inherited
    @interface Value {
        /**
         * @return column name
         */
        String value();
    }


    interface Delta<T> {
        T change();
    }

    @Command("Posts")
    class Post {
        @Key("_ID")
        final int id;

        @Value("title")
        Delta<String> title;

        @Value("body")
        Delta<String> body;

        @Value("created_at")
        Delta<Long> creationTime;

        @Value("updated_at")
        Delta<Long> updateTime;

        public Post(int id) {
            this.id = id;
        }
    }

    // Command.Result result = GenerateCommand
    //      .from(new Post(1231))
    //      .setTitle("new title")
    //      .setBody(null)
    //      .setUpdateTime(DateTime.now())
    //      .update(new AndroidDatabase(db));

    class PostCommand {
        private final Post post;

        public PostCommand(Post post) {
            this.post = post;
        }

        public PostCommand setTitle(final String title) {
            post.title = new Delta<String>() {
                @Override
                public String change() {
                    return title;
                }
            };
            return this;
        }

        public PostCommand setBody(final String body) {
            post.body = new Delta<String>() {
                @Override
                public String change() {
                    return body;
                }
            };
            return this;
        }

        public PostCommand setCreationTime(final Long creationTime) {
            post.creationTime = new Delta<Long>() {
                @Override
                public Long change() {
                    return creationTime;
                }
            };
            return this;
        }

        public PostCommand setUpdateTime(final Long updateTime) {
            post.updateTime = new Delta<Long>() {
                @Override
                public Long change() {
                    return updateTime;
                }
            };
            return this;
        }

        public void create(Object db) {}
        public void update(Object db) {}
        public void delete(Object db) {}
    }
}
