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
import ph.codeia.arch.sm.BaseState;
import ph.codeia.arch.sm.Machine;
import ph.codeia.arch.sm.Sm;
import ph.codeia.sacdemo.blackjack.BlackJack;

public class MainActivity extends AppCompatActivity {

    private static class States {
        MainState main = new MainState();
        BlackJack.Round round;
    }

    private Machine<BlackJack.Round, BlackJack.Play, BlackJack.Player> game;
    private Machine.Fixed<MainState, MainAction, MainActivity> main;
    private States states;

    private TextView bank;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        states = (States) getLastCustomNonConfigurationInstance();
        if (states == null) {
            states = new States();
        }
        main = new AndroidMachine.Builder<>(states.main).build(AsyncTask.SERIAL_EXECUTOR, this);
        super.onCreate(savedInstanceState);
        main.apply(MainAction.LOAD);
        setContentView(R.layout.activity_main);
        findViewById(R.id.do_play).setOnClickListener(_v -> main.apply(MainAction.WAGER));
        bank = (TextView) findViewById(R.id.bank);
    }

    @Override
    protected void onResume() {
        super.onResume();
        main.start(MainAction.NOOP);
    }

    @Override
    protected void onPause() {
        super.onPause();
        main.apply(MainAction.SAVE);
        main.stop();
    }

    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        return states;
    }

    @Override
    public void onAttachFragment(Fragment f) {
        if (f instanceof WagerDialog) {
            main.applyNow(inject((WagerDialog) f));
        }
    }

    void tell(String message, Object... fmtArgs) {
        Toast.makeText(this, String.format(message, fmtArgs), Toast.LENGTH_SHORT).show();
    }

    void show(int funds) {
        bank.setText(NumberFormat.getIntegerInstance().format((long) funds));
    }

    File saveFile() {
        return new File(getCacheDir(), "bank");
    }

    void ping() {
        main.apply(MainAction.NOOP);
    }

    static MainAction inject(WagerDialog f) {
        return (state, main) -> {
            f.inject(state.bank, state.lastWager, wager -> {
                state.plus(wagered(wager));
                main.ping();
            });
            return state;
        };
    }

    static MainAction wagered(int wager) {
        return (state, main) -> {
            state.lastWager = wager;
            main.tell("playing for %d", wager);
            return state;
        };
    }
}

class MainState extends BaseState<MainState, MainAction> implements Serializable {
    boolean stale = true;
    int bank = 20_000;
    int lastWager;
}

interface MainAction extends Sm.Action<MainState, MainAction, MainActivity> {
    MainAction NOOP = (state, _m) -> state;

    MainAction READY = (state, main) -> {
        main.show(state.bank);
        return state;
    };

    MainAction WAGER = (state, main) -> {
        FragmentManager fm = main.getSupportFragmentManager();
        WagerDialog f = (WagerDialog) fm.findFragmentByTag("wager");
        f = f != null ? f : new WagerDialog();
        f.show(fm, "wager");
        return state;
    };

    @SuppressLint("NewApi")
    MainAction LOAD = (state, main) -> {
        if (!state.stale) {
            return state.plus(READY);
        }
        File file = main.saveFile();
        return state.async(() -> {
            if (file.createNewFile()) {
                return (futureState, _m) -> {
                    futureState.stale = false;
                    return futureState.plus(READY);
                };
            }
            try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
                MainState saved = (MainState) in.readObject();
                LOG.e("loaded");
                return (futureState, futureMain) -> {
                    futureState.bank = saved.bank;
                    futureState.lastWager = saved.lastWager;
                    futureState.stale = false;
                    return futureState.plus(READY);
                };
            }
        });
    };

    @SuppressLint("NewApi")
    MainAction SAVE = (state, main) -> {
        if (state.stale) {
            return state;
        }
        File file = main.saveFile();
        return state.async(() -> {
            try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file))) {
                out.writeObject(state);
                LOG.e("saved");
                return NOOP;
            }
        });
    };

    final class LOG {
        static void e(String message, Object... fmtArgs) {
            Log.e("mz", String.format(message, fmtArgs));
        }
    }
}