package ph.codeia.sacdemo;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.widget.NumberPicker;

import ph.codeia.androidutils.AndroidMachine;
import ph.codeia.arch.sm.BaseState;
import ph.codeia.arch.sm.Machine;
import ph.codeia.arch.sm.Sm;

/**
 * This file is a part of the vanilla project.
 */

public class WagerDialog extends DialogFragment {

    public interface Return {
        void wagered(int wager);
    }

    private NumberPicker picker;

    private Machine.Fixed<WagerState, WagerAction, WagerDialog> wager;

    void inject(int max, int lastWager, Return onReturn) {
        wager = new AndroidMachine.Builder<>(new WagerState()).build(this);
        wager.start(init(max, lastWager, onReturn));
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return onShow(savedInstanceState, new AlertDialog.Builder(getActivity())
                .setView(R.layout.fragment_wager)
                .setTitle("Wager")
                .setPositiveButton("Play", (_d, _i) -> {
                    if (wager != null) {
                        wager.apply(ok(picker.getValue()));
                    }
                })
                .create());
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("last-value", picker.getValue());
    }

    @SuppressWarnings("ConstantConditions")
    Dialog onShow(Bundle savedState, Dialog dialog) {
        dialog.setOnShowListener(_d -> {
            picker = (NumberPicker) dialog.findViewById(R.id.picker);
            if (wager != null) {
                int value = savedState == null ? -1 : savedState.getInt("last-value", -1);
                wager.apply(restore(value));
            }
            dialog.findViewById(R.id.do_plus_10)
                    .setOnClickListener(_v -> picker
                            .setValue(10 + picker.getValue()));
            dialog.findViewById(R.id.do_plus_100)
                    .setOnClickListener(_v -> picker
                            .setValue(100 + picker.getValue()));
            dialog.findViewById(R.id.do_plus_1000)
                    .setOnClickListener(_v -> picker
                            .setValue(1000 + picker.getValue()));
            dialog.findViewById(R.id.do_minus_10)
                    .setOnClickListener(_v -> picker
                            .setValue(-10 + picker.getValue()));
            dialog.findViewById(R.id.do_minus_100)
                    .setOnClickListener(_v -> picker
                            .setValue(-100 + picker.getValue()));
            dialog.findViewById(R.id.do_minus_1000)
                    .setOnClickListener(_v -> picker
                            .setValue(-1000 + picker.getValue()));
        });
        return dialog;
    }

    static WagerAction init(int max, int lastWager, WagerDialog.Return onReturn) {
        return (state, dialog) -> {
            state.max = max;
            state.initialValue = lastWager;
            state.onReturn = onReturn;
            return state;
        };
    }

    static WagerAction restore(int pickerValue) {
        return (state, dialog) -> {
            dialog.picker.setMinValue(0);
            dialog.picker.setMaxValue(state.max);
            dialog.picker.setWrapSelectorWheel(true);
            dialog.picker.setValue(pickerValue == -1 ? state.initialValue : pickerValue);
            return state;
        };
    }

    static WagerAction ok(int wager) {
        return (state, dialog) -> {
            if (state.onReturn != null) {
                state.onReturn.wagered(wager);
            }
            state.onReturn = null;
            return state;
        };
    }
}

class WagerState extends BaseState<WagerState, WagerAction> {
    int max;
    int initialValue;
    WagerDialog.Return onReturn;
}

interface WagerAction extends Sm.Action<WagerState, WagerAction, WagerDialog> {}

