package ph.codeia.sacdemo;

import org.junit.Test;

import ph.codeia.arch.LogLevel;
import ph.codeia.arch.Logger;
import ph.codeia.arch.sac.Stepper;
import ph.codeia.sacdemo.blackjack.BlackJack;
import ph.codeia.sacdemo.blackjack.HeadToHead;
import ph.codeia.sacdemo.deck.Card;
import ph.codeia.sacdemo.deck.Face;
import ph.codeia.sacdemo.deck.Rigged;
import ph.codeia.sacdemo.deck.Suit;

import static org.junit.Assert.*;

/**
 * This file is a part of the vanilla project.
 */

public class HeadToHeadTest {
    Stepper<BlackJack.Round, BlackJack.Play, BlackJack.Player> sim = new Stepper<>(null);
    TestPlayer player = new TestPlayer(100);

    void start(BlackJack game) {
        sim.apply(player, game.start(100));
        sim.drain(player);
    }

    @Test
    public void player_blackjack() {
        Rigged shoe = Rigged.of(
                Card.ace(Suit.S), Card.of(10, Suit.C),
                Card.of(2, Suit.H), Card.of(3, Suit.D));
        start(new HeadToHead(shoe, null));
        player.expect(BlackJack.Outcome.BLACKJACK);
    }

    @Test
    public void blackjack_push() {
        Rigged shoe = Rigged.of(
                Card.ace(Suit.S), Card.of(10, Suit.C),
                Card.of(Face.J, Suit.H), Card.ace(Suit.D));
        start(new HeadToHead(shoe, null));
        player.expect(BlackJack.Outcome.PUSH);
    }

    @Test
    public void dealer_blackjack() {
        Rigged shoe = Rigged.of(
                Card.ace(Suit.S), Card.of(5, Suit.C),
                Card.of(Face.J, Suit.H), Card.ace(Suit.D));
        start(new HeadToHead(shoe, null));
        player.expect(BlackJack.Outcome.DEALER_BLACKJACK);
    }

    @Test
    public void player_bust() {
        Rigged shoe = Rigged.of(
                Card.of(10, Suit.S), Card.of(5, Suit.C),
                Card.of(Face.J, Suit.H), Card.of(7, Suit.D),
                Card.of(Face.K, Suit.C));
        start(new HeadToHead(shoe, null));
        player.hit(sim);
        player.expect(BlackJack.Outcome.BUST);
    }

    @Test
    public void dealer_bust() {
        Rigged shoe = Rigged.of(
                Card.of(5, Suit.S), Card.of(5, Suit.C),
                Card.of(Face.J, Suit.H), Card.of(2, Suit.D),
                Card.of(Face.K, Suit.C));
        start(new HeadToHead(shoe, null));
        player.stand(sim);
        player.expect(BlackJack.Outcome.DEALER_BUST);
    }

    @Test
    public void push() {
        Rigged shoe = Rigged.of(
                Card.of(5, Suit.S), Card.of(5, Suit.C),
                Card.of(Face.J, Suit.H), Card.of(2, Suit.D),
                Card.of(8, Suit.D),
                Card.of(6, Suit.C));
        start(new HeadToHead(shoe, null));
        player.hit(sim);
        player.stand(sim);
        player.expect(BlackJack.Outcome.PUSH);
    }

    @Test
    public void dealer_hits_at_soft_17() {
        Rigged shoe = Rigged.of(
                Card.of(10, Suit.S), Card.of(10, Suit.C),
                Card.of(6, Suit.H), Card.ace(Suit.D),
                Card.of(9, Suit.D),
                Card.of(6, Suit.C));
        start(new HeadToHead(shoe, null));
        player.stand(sim);
        player.expect(BlackJack.Outcome.DEALER_BUST);
    }

    @Test
    public void dealer_stays_at_soft_18() {
        Rigged shoe = Rigged.of(
                Card.of(10, Suit.S), Card.of(9, Suit.C),
                Card.of(4, Suit.H), Card.of(3, Suit.D),
                Card.ace(Suit.S),
                Card.of(2, Suit.H));
        start(new HeadToHead(shoe, null));
        player.stand(sim);
        player.expect(BlackJack.Outcome.WIN);
    }

    @Test
    public void dealer_stays_at_hard_17() {
        Rigged shoe = Rigged.of(
                Card.of(10, Suit.S), Card.of(9, Suit.C),
                Card.of(Face.Q, Suit.H), Card.of(7, Suit.D),
                Card.of(3, Suit.H));
        start(new HeadToHead(shoe, null));
        player.stand(sim);
        player.expect(BlackJack.Outcome.WIN);
    }

    @Test
    public void dealer_forced_to_hit_even_if_player_already_lost() {
        Rigged shoe = Rigged.of(
                Card.of(2, Suit.S), Card.of(2, Suit.C),
                Card.of(6, Suit.H), Card.of(Face.Q, Suit.D),
                Card.of(6, Suit.C));
        start(new HeadToHead(shoe, null));
        player.stand(sim);
        player.expect(BlackJack.Outcome.DEALER_BUST);
    }

    @Test
    public void cannot_surrender_after_hitting() {
        Rigged shoe = Rigged.of(
                Card.of(5, Suit.S), Card.of(5, Suit.C),
                Card.of(9, Suit.D), Card.of(Face.K, Suit.C));
        start(new HeadToHead(shoe, null));
        player.surrender(sim);
        player.expect(BlackJack.Outcome.SURRENDERED);

        shoe = Rigged.of(
                Card.of(5, Suit.S), Card.of(5, Suit.C),
                Card.of(9, Suit.D), Card.of(Face.K, Suit.C),
                Card.of(5, Suit.D));
        start(new HeadToHead(shoe, null));
        player.hit(sim);
        assertNull(player.surrender);
    }
}






class TestPlayer implements BlackJack.Player, Logger {
    BlackJack.Play hit;
    BlackJack.Play stand;
    BlackJack.Play doubleDown;
    BlackJack.Play surrender;
    BlackJack.Outcome actual;
    int bank = 0;

    TestPlayer(int bank) {
        this.bank = bank;
    }

    void expect(BlackJack.Outcome expected) {
        assertNotNull("no outcome yet", actual);
        assertSame(expected, actual);
    }

    void clear() {
        hit = null;
        stand = null;
        doubleDown = null;
        surrender = null;
    }

    void hit(Stepper<BlackJack.Round, BlackJack.Play, BlackJack.Player> unit) {
        assertNotNull(hit);
        unit.apply(this, hit);
        unit.drain(this);
    }

    void stand(Stepper<BlackJack.Round, BlackJack.Play, BlackJack.Player> unit) {
        assertNotNull(stand);
        unit.apply(this, stand);
        unit.drain(this);
    }

    void doubleDown(Stepper<BlackJack.Round, BlackJack.Play, BlackJack.Player> unit) {
        assertNotNull(doubleDown);
        unit.apply(this, doubleDown);
        unit.drain(this);
    }

    void surrender(Stepper<BlackJack.Round, BlackJack.Play, BlackJack.Player> unit) {
        assertNotNull(surrender);
        unit.apply(this, surrender);
        unit.drain(this);
    }

    @Override
    public boolean active(LogLevel level) {
        return true;
    }

    @Override
    public void log(LogLevel level, String message) {
        switch (level) {
            case E:
                System.err.println(message);
                break;
            default:
                System.out.println(message);
        }
    }

    @Override
    public void log(LogLevel level, Throwable error) {
        throw new RuntimeException(error);
    }

    @Override
    public void offerSurrender(
            BlackJack.Play hit,
            BlackJack.Play stand,
            BlackJack.Play doubleDown,
            BlackJack.Play surrender) {
        LogLevel.I.to(this, "hit, stand, double, surrender?");
        clear();
        this.hit = hit;
        this.stand = stand;
        this.doubleDown = doubleDown;
        this.surrender = surrender;
    }

    @Override
    public void prompt(
            BlackJack.Play hit,
            BlackJack.Play stand,
            BlackJack.Play doubleDown) {
        LogLevel.I.to(this, "hit, stand, double?");
        clear();
        this.hit = hit;
        this.stand = stand;
        this.doubleDown = doubleDown;
    }

    @Override
    public void roundOver(BlackJack.Outcome outcome, int payout) {
        actual = outcome;
        LogLevel.I.to(this, "result: %s", outcome);
        LogLevel.I.to(this, "you earned %d", payout);
        bank += payout;
    }

    @Override
    public void anteUp(int newWager) {
        LogLevel.I.to(this, "wager doubled to %d", newWager);
    }

    @Override
    public void playerDrew(Card card) {
        LogLevel.I.to(this, "player drew %s", card);
    }

    @Override
    public void dealerDrew(Card card) {
        LogLevel.I.to(this, "dealer drew %s", card);
    }

    @Override
    public void holeDrawn() {
        LogLevel.I.to(this, "dealer drew something");
    }

    @Override
    public void holeRevealed(Card card) {
        LogLevel.I.to(this, "hole card is %s", card);
    }
}
