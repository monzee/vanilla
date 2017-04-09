package ph.codeia.sacdemo.blackjack;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ph.codeia.arch.sm.RootState;
import ph.codeia.arch.sm.Sm;
import ph.codeia.sacdemo.deck.Card;
import ph.codeia.sacdemo.deck.Shoe;

/**
 * This file is a part of the vanilla project.
 */

public interface BlackJack {

    Play start(int wager);
    Play resume();

    /**
     * TODO split? insurance?
     */
    interface Player {

        /**
         * At the beginning if player did not bust and neither got a blackjack.
         *
         * @param hit Draw another card
         * @param stand Stop drawing; dealer draws until hand scores at least
         *              hard 17/soft 18
         * @param doubleDown Double the wager, draw one more card, then stand
         * @param surrender Recover half of wager, round ends.
         */
        void offerSurrender(Play hit, Play stand, Play doubleDown, Play surrender);

        /**
         * After hitting once, the player can't surrender anymore.
         *
         * @param hit Draw another card
         * @param stand Stop drawing; dealer draws until hand scores at least
         *              hard 17/soft 18
         * @param doubleDown Double the wager, draw one more card, then stand
         */
        void prompt(Play hit, Play stand, Play doubleDown);

        /**
         * After player has surrendered, busted or stood and the dealer has
         * resolved their hand.
         *
         * @param outcome The result of the round
         * @param payout may be negative if player surrendered or lost
         */
        void roundOver(Outcome outcome, int payout);

        /**
         * After doubling down.
         *
         * @param newWager twice the original wager
         */
        void anteUp(int newWager);

        /**
         * Every time a player draws a card.
         *
         * @param card The card drawn.
         */
        void playerDrew(Card card);

        /**
         * Every time the dealer draws a card.
         *
         * @param card The card drawn.
         */
        void dealerDrew(Card card);

        /**
         * At the beginning after the dealer has drawn the second card.
         */
        void holeDrawn();

        /**
         * After standing before the dealer draws the required cards.
         *
         * @param card The hole card.
         */
        void holeRevealed(Card card);
    }

    enum Outcome {
        PUSH, WIN, LOSS, BLACKJACK, SURRENDERED,
        BUST, DEALER_BUST, DEALER_BLACKJACK
    }

    interface Play extends Sm.Action<Round, Play, Player> {}

    class Round extends RootState<Round, Play> implements Serializable {

        public static Round start(int wager, Shoe shoe) {
            List<Card> empty = Collections.emptyList();
            return new Round(shoe, wager, true, true, false, empty, empty);
        }

        public final int pot;
        public final boolean canHit;
        public final boolean canSurrender;
        public final boolean surrendered;
        public final Shoe shoe;
        public final List<Card> player;
        public final List<Card> dealer;

        private Round(
                Shoe shoe,
                int pot,
                boolean canHit,
                boolean canSurrender,
                boolean surrendered,
                List<Card> player,
                List<Card> dealer) {
            this.pot = pot;
            this.canHit = canHit;
            this.canSurrender = canSurrender;
            this.shoe = shoe;
            this.surrendered = surrendered;
            this.player = player;
            this.dealer = dealer;
        }

        public Round withPot(int pot) {
            if (this.pot == pot) {
                return this;
            }
            return join(new Round(shoe, pot, canHit, canSurrender, surrendered, player, dealer));
        }

        public Round withCanHit(boolean canHit) {
            if (this.canHit == canHit) {
                return this;
            }
            return join(new Round(shoe, pot, canHit, canSurrender, surrendered, player, dealer));
        }

        public Round withCanSurrender(boolean canSurrender) {
            if (this.canSurrender == canSurrender) {
                return this;
            }
            return join(new Round(shoe, pot, canHit, canSurrender, surrendered, player, dealer));
        }

        public Round withSurrendered(boolean surrendered) {
            if (this.surrendered == surrendered) {
                return this;
            }
            return join(new Round(shoe, pot, canHit, canSurrender, surrendered, player, dealer));
        }

        public Round withPlayerCard(Card card) {
            if (card == null) {
                throw new NullPointerException("New card can't be null");
            }
            List<Card> hand = new ArrayList<>(player);
            hand.add(card);
            return join(new Round(shoe, pot, canHit, canSurrender, surrendered, hand, dealer));
        }

        public Round withDealerCard(Card card) {
            if (card == null) {
                throw new NullPointerException("New card can't be null");
            }
            List<Card> hand = new ArrayList<>(dealer);
            hand.add(card);
            return join(new Round(shoe, pot, canHit, canSurrender, surrendered, player, hand));
        }
    }
}
