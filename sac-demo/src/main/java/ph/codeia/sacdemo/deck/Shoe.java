package ph.codeia.sacdemo.deck;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * This file is a part of the vanilla project.
 */

public interface Shoe {

    void shuffle(Random rng);
    Card draw();
    boolean spent();

    class Legit implements Shoe, Serializable {
        private final List<Card> cards = new ArrayList<>();
        private final int cut;
        private volatile int i = 0;
        private volatile boolean shuffled;

        public Legit(int decks, int cut) {
            if (decks < 1) {
                throw new IllegalArgumentException("Non-positive deck count");
            }
            if (cut < 0 || cut >= 52 * decks) {
                throw new IllegalArgumentException("Invalid cut index");
            }
            this.cut = cut;
            for (; decks --> 0;) {
                for (Suit s : Suit.values()) {
                    cards.add(Card.ace(s));
                    for (int n = 2; n <= 10; n++) {
                        cards.add(Card.of(n, s));
                    }
                    for (Face f : Face.values()) {
                        cards.add(Card.of(f, s));
                    }
                }
            }
        }

        @Override
        public synchronized void shuffle(Random rng) {
            Collections.shuffle(cards, rng);
            i = 0;
            shuffled = true;
        }

        @Override
        public synchronized Card draw() {
            return cards.get(i++);
        }

        @Override
        public boolean spent() {
            return shuffled && i < cut;
        }
    }
}
