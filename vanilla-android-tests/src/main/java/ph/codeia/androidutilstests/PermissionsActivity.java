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
import ph.codeia.meta.StrawMan;
import ph.codeia.security.Permit;
import ph.codeia.signal.Channel;
import ph.codeia.signal.Links;
import ph.codeia.signal.SimpleChannel;

@StrawMan
public class PermissionsActivity extends TestActivity {

    static final Channel<String> COARSE = new SimpleChannel<>();
    static final Channel<String> FINE = new SimpleChannel<>();
    static final Channel<String> CONTACTS = new SimpleChannel<>();
    static final Channel<String> EDGE_CASE = new SimpleChannel<>();

    private static final int ASK_COARSE = 1;
    private static final int ASK_FINE = 2;
    private static final int ASK_CONTACTS = 3;
    private static final int ASK_ALL = 4;

    private Channel.Link links;
    private Permit coarse, fine, contacts, combo;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        status = (TextView) findViewById(R.id.the_status);
        AndroidPermit.Helper permits = AndroidPermit.of(getSupportFragmentManager());
        permits.setBefore(appeal -> {
            publish("appeal", appeal.permissions());
            new AlertDialog.Builder(this)
                    .setTitle("Please?")
                    .setMessage(""
                            + "I need these permissions to continue:\n-  "
                            + TextUtils.join("\n-  ", appeal.permissions()))
                    .setPositiveButton("Ask me again", (dialog, id) -> appeal.submit())
                    .create()
                    .show();
        });
        permits.setAfter(response -> {
            publish("denied", response.forbidden());
            if (!response.appeal()) {
                tell("permanently denied:%n-  %s", TextUtils.join("\n-  ", response.forbidden()));
            }
        });
        coarse = permits
                .ask(ASK_COARSE, Manifest.permission.ACCESS_COARSE_LOCATION)
                .granted(() -> COARSE.send("granted"));
        fine = permits
                .ask(Manifest.permission.ACCESS_FINE_LOCATION)
                .granted(() -> FINE.send("granted"));
        contacts = permits
                .ask(ASK_CONTACTS, Manifest.permission.READ_CONTACTS)
                .granted(() -> CONTACTS.send("granted"));
    }

    @Override
    protected void onStart() {
        super.onStart();
        links = Links.of(
                connect(COARSE, "coarse location"),
                connect(FINE, "fine location"),
                connect(CONTACTS, "read contacts"));
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
                AndroidPermit.of(this)
                        .ask(Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.READ_CONTACTS)
                        .granted(() -> {
                            COARSE.send("granted");
                            CONTACTS.send("granted");
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

    private static void publish(String message, Iterable<String> permissions) {
        for (String p : permissions) switch (p) {
            case Manifest.permission.ACCESS_COARSE_LOCATION:
                COARSE.send(message);
                break;
            case Manifest.permission.ACCESS_FINE_LOCATION:
                FINE.send(message);
                break;
            case Manifest.permission.READ_CONTACTS:
                CONTACTS.send(message);
                break;
            default:
                EDGE_CASE.send(message + " " + p);
                break;
        }
    }
}
