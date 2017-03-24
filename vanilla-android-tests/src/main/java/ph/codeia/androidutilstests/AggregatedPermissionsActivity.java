package ph.codeia.androidutilstests;

import android.Manifest;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import java.io.IOException;

import ph.codeia.androidutils.AndroidPermit;
import ph.codeia.security.Sensitive;
import ph.codeia.signal.Channel;
import ph.codeia.signal.Links;

public class AggregatedPermissionsActivity extends TestActivity {

    private static final int ASK_COARSE = 1;
    private static final int ASK_FINE = 2;
    private static final int ASK_CONTACTS = 3;
    private static final int ASK_ALL = 4;

    private Channel.Link links;
    private Sensitive coarse, fine, contacts, combo;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        status = (TextView) findViewById(R.id.the_status);
        AndroidPermit.Host permits = AndroidPermit.host(getSupportFragmentManager());
        permits.setDeniedCallback(appeal -> {
            PermissionsActivity.onDeny(appeal);
            if (!appeal.isEmpty()) {
                new AlertDialog.Builder(this)
                        .setTitle("Please?")
                        .setMessage(""
                                + "I need these permissions to continue:\n-  "
                                + TextUtils.join("\n-  ", appeal))
                        .setPositiveButton("Ask me again", (dialog, id) -> appeal.submit())
                        .create()
                        .show();
            } else {
                tell("permanently denied:\n-  %s", TextUtils.join("\n-  ", appeal.banned()));
            }
        });
        coarse = permits
                .ask(ASK_COARSE, Manifest.permission.ACCESS_COARSE_LOCATION)
                .granted(() -> PermissionsActivity.COARSE.send("granted"));
        fine = permits
                .ask(Manifest.permission.ACCESS_FINE_LOCATION)
                .granted(() -> PermissionsActivity.FINE.send("granted"));
        contacts = permits
                .ask(ASK_CONTACTS, Manifest.permission.READ_CONTACTS)
                .granted(() -> PermissionsActivity.CONTACTS.send("granted"));
    }

    @Override
    protected void onStart() {
        super.onStart();
        links = Links.of(
                connect(PermissionsActivity.COARSE, "coarse location"),
                connect(PermissionsActivity.FINE, "fine location"),
                connect(PermissionsActivity.CONTACTS, "read contacts"));
    }

    @Override
    protected void onStop() {
        super.onStop();
        links.unlink();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.permissions, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        tell("CHECK: %s", item.getTitle());
        switch (item.getItemId()) {
            case R.id.request_coarse_location:
                coarse.submit();
                return true;
            case R.id.request_fine_location:
                fine.submit();
                return true;
            case R.id.request_read_contacts:
                contacts.submit();
                return true;
            case R.id.request_combo:
                AndroidPermit.host(this)
                        .ask(Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.READ_CONTACTS)
                        .granted(() -> {
                            PermissionsActivity.COARSE.send("granted");
                            PermissionsActivity.CONTACTS.send("granted");
                        })
                        .submit();
                return true;
            case R.id.reset_permissions:
                try {
                    Runtime.getRuntime().exec("pm clear ph.codeia.androidutilstests");
                } catch (IOException e) {
                    tell(e.toString());
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private Channel.Link connect(Channel<String> channel, String desc) {
        return channel.link(s -> {
            switch (s) {
                case "granted":
                    tell("can access %s", desc);
                    break;
                case "appeal":
                    tell("appealing %s", desc);
                    break;
                case "denied":
                default:
                    break;
            }
        });
    }
}
