package ph.codeia.sacdemo;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import ph.codeia.sacdemo.blackjack.Score;
import ph.codeia.sacdemo.deck.Card;
import ph.codeia.sacdemo.deck.Face;
import ph.codeia.sacdemo.deck.Suit;

import static org.junit.Assert.*;

/**
 * This file is a part of the vanilla project.
 */

public class ScoreTest {

    @Test
    public void empty_hand_is_a_hard_zero() {
        AtomicBoolean hit = new AtomicBoolean(false);
        Score.Of.hand(Collections.emptyList()).match(new Score.Case() {
            @Override
            public void bust(int total) {
                fail("expected hard 0, got bust " + total);
            }

            @Override
            public void soft(int total) {
                fail("expected hard 0, got soft " + total);
            }

            @Override
            public void hard(int total) {
                hit.set(true);
                assertEquals(0, total);
            }

            @Override
            public void blackJack() {
                fail("expected hard 0, got blackjack");
            }
        });
        assertTrue(hit.get());
    }

    @Test
    public void blackjack() {
        List<List<Card>> hands = Arrays.asList(
                Arrays.asList(Card.of(10, Suit.C), Card.ace(Suit.D)),
                Arrays.asList(Card.of(Face.J, Suit.S), Card.ace(Suit.H)),
                Arrays.asList(Card.of(Face.Q, Suit.H), Card.ace(Suit.S)),
                Arrays.asList(Card.of(Face.K, Suit.D), Card.ace(Suit.C)),
                Arrays.asList(Card.ace(Suit.C), Card.of(10, Suit.D)),
                Arrays.asList(Card.ace(Suit.S), Card.of(Face.J, Suit.H)),
                Arrays.asList(Card.ace(Suit.H), Card.of(Face.Q, Suit.S)),
                Arrays.asList(Card.ace(Suit.D), Card.of(Face.K, Suit.C))
        );
        AtomicBoolean hit = new AtomicBoolean();
        for (List<Card> hand : hands) {
            hit.set(false);
            Score.Of.hand(hand).match(new Score.Case() {
                @Override
                public void bust(int total) {
                    fail("expected blackjack, got bust " + total);
                }

                @Override
                public void soft(int total) {
                    fail("expected blackjack, got soft " + total);
                }

                @Override
                public void hard(int total) {
                    fail("expected blackjack, got hard " + total);
                }

                @Override
                public void blackJack() {
                    hit.set(true);
                }
            });
            assertTrue(hit.get());
        }
    }

    @Test
    public void non_blackjack_soft_21() {
        List<Card> hand = Arrays.asList(
                Card.of(2, Suit.C),
                Card.of(8, Suit.H),
                Card.ace(Suit.D));
        AtomicBoolean hit = new AtomicBoolean(false);
        Score.Of.hand(hand).match(new Score.Case() {
            @Override
            public void bust(int total) {
                fail("expected soft 21, got bust " + total);
            }

            @Override
            public void soft(int total) {
                hit.set(true);
                assertEquals(21, total);
            }

            @Override
            public void hard(int total) {
                fail("expected soft 21, got hard " + total);
            }

            @Override
            public void blackJack() {
                fail("expected soft 21, got blackjack");
            }
        });
        assertTrue(hit.get());
    }

    @Test
    public void hard_21() {
        List<Card> hand = Arrays.asList(
                Card.of(4, Suit.C),
                Card.of(7, Suit.H),
                Card.of(9, Suit.D),
                Card.ace(Suit.S));
        AtomicBoolean hit = new AtomicBoolean(false);
        Score.Of.hand(hand).match(new Score.Case() {
            @Override
            public void bust(int total) {
                fail("expected hard 21, got bust " + total);
            }

            @Override
            public void soft(int total) {
                fail("expected hard 21, got soft " + total);
            }

            @Override
            public void hard(int total) {
                hit.set(true);
                assertEquals(21, total);
            }

            @Override
            public void blackJack() {
                fail("expected hard 21, got blackjack");
            }
        });
        assertTrue(hit.get());
    }

    @Test
    public void bust() {
        List<Card> hand = Arrays.asList(
                Card.of(3, Suit.C),
                Card.of(9, Suit.H),
                Card.of(Face.J, Suit.D));
        AtomicBoolean hit = new AtomicBoolean(false);
        Score.Of.hand(hand).match(new Score.Case() {
            @Override
            public void bust(int total) {
                hit.set(true);
                assertEquals(22, total);
            }

            @Override
            public void soft(int total) {
                fail("expected bust, got soft " + total);
            }

            @Override
            public void hard(int total) {
                fail("expected bust, got hard " + total);
            }

            @Override
            public void blackJack() {
                fail("expected bust, got blackjack");
            }
        });
        assertTrue(hit.get());
    }
}
