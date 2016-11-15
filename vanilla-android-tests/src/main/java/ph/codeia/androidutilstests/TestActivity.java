package ph.codeia.androidutilstests;

import android.support.annotation.StringRes;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;
import android.widget.Toast;

import ph.codeia.signal.Channel;
import ph.codeia.signal.SimpleChannel;

/**
 * This file is a part of the vanilla project.
 *
 * @author mon
 */
public class TestActivity extends AppCompatActivity {
    static final Channel<Boolean> READY = new SimpleChannel<>();

    protected TextView status;

    protected void tell(@StringRes int tpl, Object... fmtArgs) {
        tell(getString(tpl, fmtArgs));
    }

    protected void tell(String tpl, Object... fmtArgs) {
        status.setText(String.format(tpl, fmtArgs));
    }

    protected void toast(@StringRes int tpl, Object... fmtArgs) {
        toast(getString(tpl, fmtArgs));
    }

    protected void toast(String tpl, Object... fmtArgs) {
        Toast.makeText(this, String.format(tpl, fmtArgs), Toast.LENGTH_SHORT).show();
    }
}
