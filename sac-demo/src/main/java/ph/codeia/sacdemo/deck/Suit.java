package ph.codeia.sacdemo.deck;

/**
 * This file is a part of the vanilla project.
 */
public enum Suit {
    C, S, H, D;

    public Card ace() {
        return Card.ace(this);
    }

    public Card of(Face value) {
        return Card.of(value, this);
    }

    public Card of(int value) {
        return Card.of(value, this);
    }
}
