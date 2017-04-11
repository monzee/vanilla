package ph.codeia.sacdemo;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.text.NumberFormat;

import ph.codeia.androidutils.AndroidMachine;
import ph.codeia.arch.sm.Machine;
import ph.codeia.arch.sm.RootState;
import ph.codeia.arch.sm.Sm;
import ph.codeia.sacdemo.blackjack.BlackJack;

public class MainActivity extends AppCompatActivity {

    private static class States {
        State main = new State();
        BlackJack.Round round;
    }

    private static class State extends RootState<State, Action> implements Serializable {
        boolean stale = true;
        int bank = 20_000;
        int lastWager;
        transient File saveFile;
    }

    private interface Action extends Sm.Action<State, Action, MainActivity> {
        Action NOOP = (state, _m) -> state;

        Action READY = (state, main) -> {
            main.show(state.bank);
            return state;
        };

        Action WAGER = (state, main) -> {
            FragmentManager fm = main.getSupportFragmentManager();
            WagerDialog f = (WagerDialog) fm.findFragmentByTag("wager");
            f = f != null ? f : new WagerDialog();
            f.show(fm, "wager");
            return state;
        };

        @SuppressLint("NewApi")
        Action SAVE = (state, main) -> {
            if (state.stale) {
                return state;
            }
            return state.async(() -> {
                FileOutputStream fileOut = new FileOutputStream(state.saveFile);
                try (ObjectOutputStream out = new ObjectOutputStream(fileOut)) {
                    out.writeObject(state);
                    log("saved");
                    return NOOP;
                }
            });
        };

        @SuppressLint("NewApi")
        Action LOAD = (state, main) -> {
            if (!state.stale) {
                return state.plus(READY);
            }
            return state.async(() -> {
                File file = state.saveFile;
                if (file.createNewFile()) {
                    return (futureState, _m) -> {
                        futureState.stale = false;
                        return futureState.plus(SAVE).plus(READY);
                    };
                }
                try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
                    State saved = (State) in.readObject();
                    log("loaded");
                    return (futureState, futureMain) -> {
                        futureState.bank = saved.bank;
                        futureState.lastWager = saved.lastWager;
                        futureState.stale = false;
                        return futureState.plus(READY);
                    };
                }
            });
        };
    }

    private Machine<BlackJack.Round, BlackJack.Play, BlackJack.Player> game;
    private Machine.Bound<State, Action, MainActivity> machine;
    private States states;

    private TextView bank;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        states = (States) getLastCustomNonConfigurationInstance();
        if (states == null) {
            states = new States();
            states.main.saveFile = new File(getCacheDir(), "bank");
        }
        machine = new AndroidMachine.Builder<>(states.main).build(AsyncTask.SERIAL_EXECUTOR, this);
        super.onCreate(savedInstanceState);
        machine.apply(Action.LOAD);
        setContentView(R.layout.activity_main);
        findViewById(R.id.do_play).setOnClickListener(_v -> machine.apply(Action.WAGER));
        bank = (TextView) findViewById(R.id.bank);
    }

    @Override
    public void onAttachFragment(Fragment f) {
        if (f instanceof WagerDialog) {
            machine.apply(inject((WagerDialog) f));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        machine.start(Action.NOOP);
    }

    @Override
    protected void onPause() {
        super.onPause();
        machine.apply(Action.SAVE);
        machine.stop();
    }

    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        return states;
    }

    void tell(String message, Object... fmtArgs) {
        Toast.makeText(this, String.format(message, fmtArgs), Toast.LENGTH_SHORT).show();
    }

    void show(int funds) {
        bank.setText(NumberFormat.getIntegerInstance().format((long) funds));
    }

    void wagered(int wager) {
        machine.apply((state, main) -> {
            state.lastWager = wager;
            main.tell("playing for %d", wager);
            return state;
        });
    }

    static Action inject(WagerDialog f) {
        return (state, main) -> {
            f.inject(state.bank, state.lastWager, main::wagered);
            return state;
        };
    }

    static void log(String message, Object... fmtArgs) {
        Log.e("mz", String.format(message, fmtArgs));
    }
}
