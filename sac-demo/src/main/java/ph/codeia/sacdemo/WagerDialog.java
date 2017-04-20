package ph.codeia.sacdemo;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.NumberPicker;

import ph.codeia.androidutils.AndroidMachine;
import ph.codeia.arch.sm.Machine;
import ph.codeia.arch.sm.RootState;
import ph.codeia.arch.sm.Sm;

/**
 * This file is a part of the vanilla project.
 */

public class WagerDialog extends DialogFragment {

    public interface Return {
        void wagered(int wager);
    }

    private static class State extends RootState<State, Action> {
        int max;
        int initialValue;
        Return onReturn;
    }

    private interface Action extends Sm.Action<State, Action, WagerDialog> {}

    private NumberPicker picker;

    private Machine.Bound<State, Action, WagerDialog> machine;

    void inject(int max, int lastWager, Return onReturn) {
        machine = new AndroidMachine.Builder<>(new State()).build(this);
        machine.start(init(max, lastWager, onReturn));
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return onShow(savedInstanceState, new AlertDialog.Builder(getActivity())
                .setView(R.layout.fragment_wager)
                .setTitle("Wager")
                .setPositiveButton("Play", (_d, _i) -> {
                    if (machine != null) {
                        machine.apply(ok(picker.getValue()));
                    }
                })
                .create());
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("last-value", picker.getValue());
    }

    @Override
    public void onDetach() {
        super.onDetach();
        machine.stop();
        machine = null;
    }

    @SuppressWarnings("ConstantConditions")
    Dialog onShow(Bundle savedState, Dialog dialog) {
        dialog.setOnShowListener(_d -> {
            picker = (NumberPicker) dialog.findViewById(R.id.picker);
            if (machine != null) {
                int value = savedState == null ? -1 : savedState.getInt("last-value", -1);
                machine.apply(restore(value));
            }
            dialog.findViewById(R.id.do_plus_10).setOnClickListener(moveBy(10));
            dialog.findViewById(R.id.do_plus_100).setOnClickListener(moveBy(100));
            dialog.findViewById(R.id.do_plus_1000).setOnClickListener(moveBy(1000));
            dialog.findViewById(R.id.do_minus_10).setOnClickListener(moveBy(-10));
            dialog.findViewById(R.id.do_minus_100).setOnClickListener(moveBy(-100));
            dialog.findViewById(R.id.do_minus_1000).setOnClickListener(moveBy(-1000));
        });
        return dialog;
    }

    private View.OnClickListener moveBy(int delta) {
        return _v -> picker.setValue(picker.getValue() + delta);
    }

    static Action init(int max, int lastWager, WagerDialog.Return onReturn) {
        return (state, dialog) -> {
            state.max = max;
            state.initialValue = lastWager;
            state.onReturn = onReturn;
            return state;
        };
    }

    static Action restore(int pickerValue) {
        return (state, dialog) -> {
            dialog.picker.setMinValue(0);
            dialog.picker.setMaxValue(state.max);
            dialog.picker.setWrapSelectorWheel(true);
            dialog.picker.setValue(pickerValue == -1 ? state.initialValue : pickerValue);
            return state;
        };
    }

    static Action ok(int wager) {
        return (state, dialog) -> {
            if (state.onReturn != null) {
                state.onReturn.wagered(wager);
            }
            state.onReturn = null;
            return state;
        };
    }
}
