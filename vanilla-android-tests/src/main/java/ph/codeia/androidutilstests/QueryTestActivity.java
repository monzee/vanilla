package ph.codeia.androidutilstests;

import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;

import ph.codeia.androidutils.AndroidContent;
import ph.codeia.meta.Query;
import ph.codeia.query.GenerateQuery;
import ph.codeia.query.Template;

public class QueryTestActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        for (ProfileEmail profile : GenerateQuery.from(new ProfileEmail())
                .query(new AndroidContent(getContentResolver(), ProfileEmail.URI))) {
            profile.isPrimary++;
        }
    }

    static class PostSchema {
        @Query.Select("Title")
        String title;

        @Query.Select("Body")
        String body;

        @Query.Select("Banner")
        byte[] banner;

        @Query.Select("Created On")
        @Query.Order.Descending
        long timestamp;
    }

    @Query("Posts")
    public static class PostById extends PostSchema implements Template<PostById> {
        @Query.Where.Eq("_ID")
        final int id;

        PostById(int id) {
            this.id = id;
        }

        @Override
        public PostById copy() {
            return new PostById(id);
        }
    }

    @Query(ContactsContract.Contacts.Data.CONTENT_DIRECTORY)
    public static class ProfileEmail implements Template<ProfileEmail> {
        static final Uri URI = ContactsContract.Profile.CONTENT_URI;

        @Query.Where.Eq(ContactsContract.Contacts.Data.MIMETYPE)
        final String type = ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE;

        @Query.Select(ContactsContract.CommonDataKinds.Email.ADDRESS)
        String email;

        @Query.Select(ContactsContract.CommonDataKinds.Email.IS_PRIMARY)
        @Query.Order.Descending
        int isPrimary;

        @Override
        public ProfileEmail copy() {
            return new ProfileEmail();
        }
    }
}
