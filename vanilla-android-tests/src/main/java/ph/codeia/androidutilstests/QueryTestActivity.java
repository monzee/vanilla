package ph.codeia.androidutilstests;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import ph.codeia.meta.Query;
import ph.codeia.query.Expr;

public class QueryTestActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Query("POSTS")
    static class PostById {
        @Query.Where.Eq("_ID")
        final int id;

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
    }

    static class MetaPostById {
        PostById container;
        final String source = "POSTS";
        final Expr projection = of -> of.enumeration(
                a -> a.identifier("body"),
                b -> b.identifier("title"),
                c -> c.identifier("banner"),
                d -> d.identifier("created_on"));
        final Expr filter = of -> of.enumeration(
                a -> a.conjunction(" = ", b -> b.identifier("_ID"), c -> c.value(container.id))
        );
        final Expr order = of -> of.enumeration(
                a -> a.association("desc", b -> b.identifier("created_on"))
        );
    }
}
