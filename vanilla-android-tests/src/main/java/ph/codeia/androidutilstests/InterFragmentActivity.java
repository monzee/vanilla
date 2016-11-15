package ph.codeia.androidutilstests;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import ph.codeia.androidutils.AndroidLoaderStore;
import ph.codeia.signal.Channel;
import ph.codeia.signal.Replay;
import ph.codeia.signal.SimpleChannel;
import ph.codeia.values.Store;

public class InterFragmentActivity extends TestActivity {

    static final Channel<Boolean> READY = new SimpleChannel<>();

    private Channel.Link tell;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_interfragment);
        status = (TextView) findViewById(R.id.the_status);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Store store = new AndroidLoaderStore(this);
        tell = store.hardGet(DisplayFragment.LOG, Replay<String>::new).link(this::tell);
        READY.send(true);
    }

    @Override
    protected void onStop() {
        super.onStop();
        tell.unlink();
    }

}
