package ph.codeia.androidutilstests;

import android.Manifest;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import ph.codeia.androidutils.AndroidPermit;
import ph.codeia.security.Permit;
import ph.codeia.security.Sensitive;
import ph.codeia.signal.Channel;
import ph.codeia.signal.Links;
import ph.codeia.signal.SimpleChannel;

public class PermissionsActivity extends TestActivity {

    static final Channel<String> COARSE = new SimpleChannel<>();
    static final Channel<String> FINE = new SimpleChannel<>();
    static final Channel<String> CONTACTS = new SimpleChannel<>();
    static final Channel<String> EDGE_CASE = new SimpleChannel<>();

    final List<Sensitive> requests = new ArrayList<>();
    final AtomicInteger counter = new AtomicInteger(1);
    private Channel.Link links;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        status = (TextView) findViewById(R.id.the_status);
        Collections.addAll(requests,
                ask(Manifest.permission.ACCESS_COARSE_LOCATION)
                        .granted(() -> COARSE.send("granted")),
                ask(Manifest.permission.ACCESS_FINE_LOCATION)
                        .granted(() -> FINE.send("granted")),
                ask(Manifest.permission.READ_CONTACTS)
                        .granted(() -> CONTACTS.send("granted")),
                ask(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.READ_CONTACTS)
                        .granted(() -> {
                            COARSE.send("granted");
                            CONTACTS.send("granted");
                        }));
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
                requests.get(0).submit();
                return true;
            case R.id.request_fine_location:
                requests.get(1).submit();
                return true;
            case R.id.request_read_contacts:
                requests.get(2).submit();
                return true;
            case R.id.request_combo:
                requests.get(3).submit();
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

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        for (Sensitive s : requests) {
            if (s.check(requestCode, permissions, grantResults)) {
                s.dispatch();
                return;
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    Permit ask(String... permissions) {
        int code = counter.getAndIncrement();
        return new AndroidPermit(this, code).ask(permissions).denied(appeal -> {
            onDeny(appeal);
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
    }

    static void onDeny(Sensitive appeal) {
        for (String s : appeal) switch (s) {
            case Manifest.permission.ACCESS_COARSE_LOCATION:
                COARSE.send("appeal");
                break;
            case Manifest.permission.ACCESS_FINE_LOCATION:
                FINE.send("appeal");
                break;
            case Manifest.permission.READ_CONTACTS:
                CONTACTS.send("appeal");
                break;
            default:
                EDGE_CASE.send("appeal " + s);
                break;
        }
        for (String s : appeal.banned()) switch (s) {
            case Manifest.permission.ACCESS_COARSE_LOCATION:
                COARSE.send("denied");
                break;
            case Manifest.permission.ACCESS_FINE_LOCATION:
                FINE.send("denied");
                break;
            case Manifest.permission.READ_CONTACTS:
                CONTACTS.send("denied");
                break;
            default:
                EDGE_CASE.send("denied " + s);
                break;
        }
        if (appeal.isEmpty() && appeal.banned().isEmpty()) {
            EDGE_CASE.send("this should never happen");
        }
    }
}
