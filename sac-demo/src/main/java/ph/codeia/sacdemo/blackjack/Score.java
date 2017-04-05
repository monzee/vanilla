package ph.codeia.sacdemo.blackjack;

import java.util.List;

import ph.codeia.sacdemo.deck.Card;
import ph.codeia.sacdemo.deck.Face;

/**
 * This file is a part of the vanilla project.
 */

public interface Score {
    void match(Case sum);

    interface Case {
        void bust(int total);
        void soft(int total);
        void hard(int total);
        void blackJack();
    }

    final class Of implements Card.Value.Case {

        public static Score hand(List<Card> cards) {
            Of accumulator = new Of();
            for (Card c : cards) {
                accumulator.fold(c);
            }
            final int total = accumulator.total;
            if (total == 21 && cards.size() == 2) {
                return Case::blackJack;
            } else if (total > 21) {
                return e -> e.bust(total);
            } else if (accumulator.hasSoftAce) {
                return e -> e.soft(total);
            } else {
                return e -> e.hard(total);
            }
        }

        private int total = 0;
        private boolean hasSoftAce = false;

        private Of() {}

        private void fold(Card card) {
            card.value.match(this);
        }

        @Override
        public void number(int value) {
            total += value;
            if (total > 21 && hasSoftAce) {
                total -= 10;
                hasSoftAce = false;
            }
        }

        @Override
        public void face(Face value) {
            number(10);
        }

        @Override
        public void ace() {
            if (!hasSoftAce) {
                hasSoftAce = true;
                number(11);
            } else {
                number(1);
            }
        }
    }
}
