package ph.codeia.sacdemo.deck;

/**
 * This file is a part of the vanilla project.
 */

public class Card {

    public static Card of(Face value, Suit suit) {
        return new Card(e -> e.face(value), suit);
    }

    public static Card of(int value, Suit suit) {
        if (value < 2 || value > 10) {
            throw new IllegalArgumentException("Number card must be in range [2, 10]");
        }
        return new Card(e -> e.number(value), suit);
    }

    public static Card ace(Suit suit) {
        return new Card(Value.Case::ace, suit);
    }

    public interface Value {
        void match(Case sum);
        interface Case {
            void number(int value);
            void face(Face value);
            void ace();
        }
    }

    public final Value value;
    public final Suit suit;

    private Card(Value value, Suit suit) {
        this.value = value;
        this.suit = suit;
    }

    @Override
    public String toString() {
        return new Card.Value.Case() {
            String repr;

            String repr() {
                value.match(this);
                switch (suit) {
                    case C:
                        return repr + '♣';
                    case S:
                        return repr + '♠';
                    case H:
                        return repr + '♥';
                    case D:
                        return repr + '♦';
                    default:
                        return repr + suit;
                }
            }

            @Override
            public void number(int value) {
                repr = Integer.toString(value);
            }

            @Override
            public void face(Face value) {
                repr = value.toString();
            }

            @Override
            public void ace() {
                repr = "A";
            }
        }.repr();
    }
}
