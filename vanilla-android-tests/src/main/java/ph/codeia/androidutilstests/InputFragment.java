package ph.codeia.androidutilstests;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import ph.codeia.androidutils.AndroidLoaderStore;
import ph.codeia.signal.Channel;
import ph.codeia.signal.SimpleChannel;


/**
 * A simple {@link Fragment} subclass.
 */
public class InputFragment extends Fragment {

    private Channel<Integer> keys;

    public InputFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_keypad, container, false);
        for (int id : new int[] {
                R.id.num_0, R.id.num_1, R.id.num_2,
                R.id.num_3, R.id.num_4, R.id.num_5,
                R.id.num_6, R.id.num_7, R.id.num_8,
                R.id.num_9, R.id.dec, R.id.neg,
        }) {
            root.findViewById(id).setOnClickListener(view -> keys.send(id));
        }
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        keys = new AndroidLoaderStore(getActivity())
                .hardGet(DisplayFragment.KEY_PRESS, SimpleChannel::new);
    }

}
