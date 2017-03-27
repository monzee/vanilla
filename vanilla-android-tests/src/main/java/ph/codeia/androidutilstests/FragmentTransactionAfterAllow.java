package ph.codeia.androidutilstests;

import android.Manifest;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.io.IOException;

import ph.codeia.androidutils.AndroidPermit;
import ph.codeia.meta.StrawMan;
import ph.codeia.security.Permission;

@StrawMan
public class FragmentTransactionAfterAllow extends AppCompatActivity {

    public static class Priviledged extends Fragment {
        @Nullable
        @Override
        public View onCreateView(
                LayoutInflater inflater,
                @Nullable ViewGroup container,
                @Nullable Bundle savedInstanceState) {
            TextView tv = (TextView) inflater
                    .inflate(android.R.layout.simple_list_item_1, container, false);
            tv.setText("hey there");
            tv.setLayoutParams(new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER));
            return tv;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment_transaction_after_allow);
        findViewById(R.id.do_ask).setOnClickListener(_v -> AndroidPermit.of(this)
                .ask(Manifest.permission.READ_CONTACTS)
                .before(Permission.Appeal::submit)
                .granted(() -> getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.root, new Priviledged())
                        .addToBackStack("foo")
                        .commit())
                .submit());
        findViewById(R.id.do_reset).setOnClickListener(_v -> {
            try {
                Runtime.getRuntime().exec("pm clear ph.codeia.androidutilstests");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
