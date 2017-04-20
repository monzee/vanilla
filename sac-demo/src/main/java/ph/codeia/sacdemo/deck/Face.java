package ph.codeia.sacdemo.deck;

/**
 * This file is a part of the vanilla project.
 */
public enum Face {
    J, Q, K;

    public Card of(Suit suit) {
        return Card.of(this, suit);
    }
}
