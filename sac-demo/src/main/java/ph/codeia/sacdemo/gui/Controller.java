package ph.codeia.sacdemo.gui;

import android.widget.Button;

import ph.codeia.arch.sm.Machine;
import ph.codeia.sacdemo.blackjack.BlackJack;
import ph.codeia.sacdemo.deck.Card;

import static android.view.View.VISIBLE;
import static android.view.View.GONE;

/**
 * This file is a part of the vanilla project.
 */

public class Controller implements BlackJack.Player {

    public interface View {

    }

    private final Machine.Bound<BlackJack.Round, BlackJack.Play, BlackJack.Player> model;
    private final Button doHit;
    private final Button doStand;
    private final Button doDoubleDown;
    private final Button doSurrender;

    public Controller(
            Machine.Bound<BlackJack.Round, BlackJack.Play, BlackJack.Player> model,
            Button doHit,
            Button doStand,
            Button doDoubleDown,
            Button doSurrender) {
        this.model = model;
        this.doHit = doHit;
        this.doStand = doStand;
        this.doDoubleDown = doDoubleDown;
        this.doSurrender = doSurrender;
    }

    void reset() {
        reset(doHit);
        reset(doStand);
        reset(doDoubleDown);
        reset(doSurrender);
    }

    void reset(Button button) {
        button.setOnClickListener(null);
        button.setEnabled(false);
    }

    void link(Button button, BlackJack.Play action) {
        button.setEnabled(true);
        button.setOnClickListener(_v -> {
            model.apply(action);
            reset();
        });
    }

    @Override
    public void offerSurrender(
            BlackJack.Play hit,
            BlackJack.Play stand,
            BlackJack.Play doubleDown,
            BlackJack.Play surrender) {
        doSurrender.setVisibility(VISIBLE);
        link(doHit, hit);
        link(doStand, stand);
        link(doDoubleDown, doubleDown);
        link(doSurrender, surrender);
    }

    @Override
    public void prompt(
            BlackJack.Play hit,
            BlackJack.Play stand,
            BlackJack.Play doubleDown) {
        doSurrender.setVisibility(GONE);
        link(doHit, hit);
        link(doStand, stand);
        link(doDoubleDown, doubleDown);
    }

    @Override
    public void roundOver(BlackJack.Outcome outcome, int payout) {

    }

    @Override
    public void anteUp(int newWager) {

    }

    @Override
    public void playerDrew(Card card) {

    }

    @Override
    public void dealerDrew(Card card) {

    }

    @Override
    public void holeDrawn() {

    }

    @Override
    public void holeRevealed(Card card) {

    }
}
