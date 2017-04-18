package ph.codeia.androidutilstests;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import ph.codeia.meta.Query;
import ph.codeia.query.Template;

public class QueryTestActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Query("POSTS")
    static class PostById implements Template<PostById> {
        @Query.Where.Eq("_ID")
        final int id;

        @Query.Where.Eq("category")
        final String category = "uncategorized";

        @Query.Select("body")
        String body;

        @Query.Select("title")
        String title;

        @Query.Select("banner")
        byte[] banner;

        @Query.Select("created_on")
        @Query.Order.Descending
        long timestamp;

        PostById(int id) {
            this.id = id;
        }

        @Override
        public PostById copy() {
            return new PostById(id);
        }
    }
}
