package ph.codeia.sacdemo.deck;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * This file is a part of the vanilla project.
 */

public class Rigged implements Shoe {

    public static Rigged of(Card... cards) {
        Rigged shoe = new Rigged();
        for (Card c : cards) {
            shoe.add(c);
        }
        return shoe;
    }

    private final List<Card> cards = new ArrayList<>();
    private int i = 0;

    public void add(Card card) {
        cards.add(card);
    }

    public void reset() {
        i = 0;
    }

    @Override
    public void shuffle(Random rng) {
        reset();
    }

    @Override
    public synchronized Card draw() {
        if (cards.size() == 0) {
            throw new IllegalStateException("Shoe is empty; add something!");
        }
        Card card = cards.get(i);
        i = (i + 1) % cards.size();
        return card;
    }

    @Override
    public boolean spent() {
        return true;
    }
}
