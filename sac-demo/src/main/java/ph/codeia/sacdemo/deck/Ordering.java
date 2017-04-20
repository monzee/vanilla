package ph.codeia.sacdemo.deck;

import java.util.Comparator;

/**
 * This file is a part of the vanilla project.
 */

public enum Ordering implements Comparator<Card> {

    HIGH_ACE(true), LOW_ACE(false);

    public final Comparator<Card> suitFirst = (first, second) -> {
        int order = first.suit.compareTo(second.suit);
        if (order == 0) {
            order = compare(first, second);
        }
        return order;
    };

    public final Comparator<Card> rankFirst = (first, second) -> {
        int order = compare(first, second);
        if (order == 0) {
            order = first.suit.compareTo(second.suit);
        }
        return order;
    };

    private final boolean highAce;

    Ordering(boolean highAce) {
        this.highAce = highAce;
    }

    @Override
    public int compare(Card first, Card second) {
        return new Card.Value.Case() {
            int order;

            {
                first.value.match(this);
            }

            @Override
            public void number(int value) {
                second.value.match(new Card.Value.Case() {
                    @Override
                    public void number(int other) {
                        order = value - other;
                    }

                    @Override
                    public void face(Face value) {
                        order = -1;
                    }

                    @Override
                    public void ace() {
                        order = highAce ? -1 : 1;
                    }
                });
            }

            @Override
            public void face(Face value) {
                second.value.match(new Card.Value.Case() {
                    @Override
                    public void number(int value) {
                        order = 1;
                    }

                    @Override
                    public void face(Face other) {
                        order = value.compareTo(other);
                    }

                    @Override
                    public void ace() {
                        order = highAce ? -1 : 1;
                    }
                });
            }

            @Override
            public void ace() {
                second.value.match(new Card.Value.Case() {
                    @Override
                    public void number(int value) {
                        order = highAce ? 1 : -1;
                    }

                    @Override
                    public void face(Face value) {
                        order = highAce ? 1 : -1;
                    }

                    @Override
                    public void ace() {
                        order = 0;
                    }
                });
            }
        }.order;
    }
}