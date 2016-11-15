package ph.codeia.androidutilstests;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import ph.codeia.androidutils.AndroidLoaderStore;
import ph.codeia.signal.Channel;
import ph.codeia.signal.SimpleChannel;
import ph.codeia.values.Store;


/**
 * A simple {@link Fragment} subclass.
 */
public class DisplayFragment extends Fragment {

    public static final String KEY_PRESS = "keypress";
    public static final String LOG = "log";
    private static final String STATE = "state";

    private static class State {
        final Channel<String> log;
        final StringBuilder buffer = new StringBuilder();
        boolean positive = true;
        boolean isZero = true;
        int decimalPlace = -1;

        State(Channel<String> log) {
            this.log = log;
        }

        void num(char digit) {
            if (digit != '0' || buffer.length() > 0) {
                buffer.append(digit);
            } else {
                log.send("insignificant zero");
            }
        }

        void negate() {
            isZero = isZero && allZeroes(buffer);
            if (isZero) {
                log.send("negative zero?");
                return;
            }
            if (positive) {
                buffer.insert(0, '-');
            } else {
                buffer.deleteCharAt(0);
            }
            positive = !positive;
        }

        void point() {
            if (decimalPlace == -1) {
                decimalPlace = buffer.length();
                if (decimalPlace == 0) {
                    buffer.append('0');
                    decimalPlace += 1;
                }
                buffer.append('.');
            } else {
                log.send("already has a decimal point");
            }
        }

        private static boolean allZeroes(CharSequence chars) {
            for (int i = 0; i < chars.length(); i++) {
                char c = chars.charAt(i);
                if (c != '0' && c != '.') {
                    return false;
                }
            }
            return true;
        }
    }

    private Channel.Link onKeyPress;
    private TextView display;
    private State state;

    public DisplayFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_display, container, false);
        display = (TextView) root.findViewById(R.id.the_display);
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        Store store = new AndroidLoaderStore(getActivity());
        Channel<String> log = store.hardGet(LOG, Store.Presence.expected());
        state = store.hardGet(STATE, () -> new State(log));
        onKeyPress = store.hardGet(KEY_PRESS, SimpleChannel<Integer>::new).link(this::keyPressed);
        update();
    }

    @Override
    public void onPause() {
        super.onPause();
        onKeyPress.unlink();
    }

    void update() {
        if (state.buffer.length() == 0) {
            display.setText("0");
        } else {
            display.setText(state.buffer);
        }
    }

    void keyPressed(int id) {
        switch (id) {
            case R.id.num_0:
                state.num('0');
                break;
            case R.id.num_1:
                state.num('1');
                break;
            case R.id.num_2:
                state.num('2');
                break;
            case R.id.num_3:
                state.num('3');
                break;
            case R.id.num_4:
                state.num('4');
                break;
            case R.id.num_5:
                state.num('5');
                break;
            case R.id.num_6:
                state.num('6');
                break;
            case R.id.num_7:
                state.num('7');
                break;
            case R.id.num_8:
                state.num('8');
                break;
            case R.id.num_9:
                state.num('9');
                break;
            case R.id.dec:
                state.point();
                break;
            case R.id.neg:
                state.negate();
                break;
            default:
                break;
        }
        update();
    }
}
