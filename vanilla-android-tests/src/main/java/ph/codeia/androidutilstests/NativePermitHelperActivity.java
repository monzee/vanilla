package ph.codeia.androidutilstests;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;

import ph.codeia.androidutils.AndroidPermit;
import ph.codeia.security.Permission;
import ph.codeia.values.Do;

public class NativePermitHelperActivity extends Activity {

    TextView status;
    Button askReadContacts;
    Button askCoarseLocation;
    Button askFineLocation;
    Button askCombo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_native_perms);
        status = (TextView) findViewById(R.id.the_status);
        askReadContacts = (Button) findViewById(R.id.ask_read_contact);
        askCoarseLocation = (Button) findViewById(R.id.ask_coarse_location);
        askFineLocation = (Button) findViewById(R.id.ask_fine_location);
        askCombo = (Button) findViewById(R.id.ask_combination);

        AndroidPermit.Helper permits = AndroidPermit.of(this);

        askReadContacts.setOnClickListener(_v -> permits
                .ask(Manifest.permission.READ_CONTACTS)
                .before(appeal("read contacts"))
                .after(denied("read contacts"))
                .granted(granted("read contacts"))
                .submit());

        askCoarseLocation.setOnClickListener(_v -> permits
                .ask(Manifest.permission.ACCESS_COARSE_LOCATION)
                .before(appeal("coarse location"))
                .after(denied("coarse location"))
                .granted(granted("coarse location"))
                .submit());

        askFineLocation.setOnClickListener(_v -> permits
                .ask(Manifest.permission.ACCESS_FINE_LOCATION)
                .before(appeal("fine location"))
                .after(denied("fine location"))
                .granted(granted("fine location"))
                .submit());

        askCombo.setOnClickListener(_v -> permits
                .ask(Manifest.permission.READ_CONTACTS,
                        Manifest.permission.ACCESS_COARSE_LOCATION)
                .before(appeal("combo"))
                .after(denied("combo"))
                .granted(granted("combo"))
                .submit());

        findViewById(R.id.do_reset).setOnClickListener(_v -> {
            try {
                Runtime.getRuntime().exec("pm clear ph.codeia.androidutilstests");
            } catch (IOException ignored) {}
        });
    }

    Runnable granted(String message) {
        return () -> status.setText(message + " granted.");
    }

    Do.Just<Permission.Appeal> appeal(String permission) {
        return appeal -> {
            status.setText("appealing " + permission);
            new AlertDialog.Builder(this)
                    .setTitle("Appeal")
                    .setMessage("let me\n- " + TextUtils.join("\n- ", appeal.permissions()))
                    .setPositiveButton("Ask me again", (_d, _i) -> appeal.submit())
                    .show();
        };
    }

    Do.Just<Permission.Denial> denied(String permission) {
        return denial -> {
            denial.appeal();
            status.setText("asked " + permission
                    + "\ndenied:\n- " + TextUtils.join("\n- ", denial.denied())
                    + "\nforbidden:\n- " + TextUtils.join("\n- ", denial.forbidden()));
        };
    }

}
