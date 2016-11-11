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

import ph.codeia.androidutils.AndroidPermit;
import ph.codeia.security.Permit;
import ph.codeia.security.Sensitive;
import ph.codeia.signal.Channel;
import ph.codeia.signal.SimpleChannel;

public class PermissionsActivity extends TestActivity {

    public static final Channel<String> GRANTED = new SimpleChannel<>();
    public static final Channel<String> APPEAL = new SimpleChannel<>();
    public static final Channel<String> DENIED = new SimpleChannel<>();

    private Sensitive askCoarseLocation;
    private Sensitive askFineLocation;
    private Sensitive askReadContacts;
    private Sensitive askAsk;
    private Sensitive askPhoneCall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        status = (TextView) findViewById(R.id.the_status);
        askCoarseLocation = ask(Manifest.permission.ACCESS_COARSE_LOCATION)
                .granted(() -> {
                    tell("can get coarse");
                    GRANTED.send(Manifest.permission.ACCESS_COARSE_LOCATION);
                });
        askFineLocation = ask(Manifest.permission.ACCESS_FINE_LOCATION)
                .granted(() -> {
                    tell("can get fine");
                    GRANTED.send(Manifest.permission.ACCESS_FINE_LOCATION);
                });
        askReadContacts = ask(Manifest.permission.READ_CONTACTS)
                .granted(() -> {
                    tell("can read");
                    GRANTED.send(Manifest.permission.READ_CONTACTS);
                });
        askAsk = ask(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.READ_CONTACTS)
                .granted(() -> {
                    tell("can locate and read");
                    GRANTED.send(Manifest.permission.ACCESS_COARSE_LOCATION);
                    GRANTED.send(Manifest.permission.READ_CONTACTS);
                });
        askPhoneCall = ask(Manifest.permission.CALL_PHONE)
                .granted(() -> {
                    tell("can phone");
                    GRANTED.send(Manifest.permission.CALL_PHONE);
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
                askCoarseLocation.submit();
                return true;
            case R.id.request_fine_location:
                askFineLocation.submit();
                return true;
            case R.id.request_read_contacts:
                askReadContacts.submit();
                return true;
            case R.id.reset_permissions:
                try {
                    Runtime.getRuntime().exec("pm clear ph.codeia.androidutilstests");
                } catch (IOException e) {
                    tell(e.toString());
                }
                return true;
            case R.id.request_combo:
                askAsk.submit();
                return true;
            case R.id.do_something:
                askPhoneCall.submit();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        for (Sensitive s : new Sensitive[] {
                askCoarseLocation,
                askFineLocation,
                askReadContacts,
                askAsk,
        }) {
            if (s.apply(requestCode, permissions, grantResults)) {
                return;
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private Permit ask(String... permissions) {
        return new AndroidPermit(this).ask(permissions).denied(appeal -> {
            for (String denied : appeal) {
                APPEAL.send(denied);
            }
            for (String banned : appeal.banned()) {
                DENIED.send(banned);
            }
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
}
