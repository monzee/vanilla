package ph.codeia.sacdemo.blackjack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ph.codeia.arch.sm.BaseState;
import ph.codeia.arch.sm.Sm;
import ph.codeia.sacdemo.deck.Card;
import ph.codeia.sacdemo.deck.Shoe;

/**
 * This file is a part of the vanilla project.
 */

public interface Table {

    interface Player {}

    interface Play extends Sm.Action<Phase, Play, Player> {}

    class Hands {
        private final List<Card> dealerHand = new ArrayList<>();
        private final Map<Player, List<Card>> playerHands = new HashMap<>();

        void playerDraws(Player p, Card c) {
            if (!playerHands.containsKey(p)) {
                playerHands.put(p, new ArrayList<>());
            }
            playerHands.get(p).add(c);
        }

        void dealerDraws(Card c) {
            dealerHand.add(c);
        }
    }

    interface Phase extends Sm.State<Phase, Play> {
        void match(Case c);
        Phase withPlayerCard(Player p, Card c);
        Phase withDealerCard(Card c);

        interface Case {
            void starting(Shoe shoe, Map<Player, Integer> players);
            void playing(Shoe shoe, Map<Player, Integer> players);
            void resolving(Shoe shoe);
            void over(List<Player> players);
        }

        class Starting extends BaseState<Phase, Play> implements Phase {
            private final Shoe shoe;
            private final Map<Player, Integer> players;
            private final Hands hands;

            public Starting(Shoe shoe, Map<Player, Integer> players, Hands hands) {
                this.shoe = shoe;
                this.players = players;
                this.hands = hands;
            }

            @Override
            public void match(Case c) {
                c.starting(shoe, players);
            }

            @Override
            public Phase withPlayerCard(Player p, Card c) {
                hands.playerDraws(p, c);
                return this;
            }

            @Override
            public Phase withDealerCard(Card c) {
                hands.dealerDraws(c);
                return this;
            }
        }

        class Playing extends BaseState<Phase, Play> implements Phase {
            private final Shoe shoe;
            private final Map<Player, Integer> players;
            private final Hands hands;

            public Playing(Shoe shoe, Map<Player, Integer> players, Hands hands) {
                this.shoe = shoe;
                this.players = players;
                this.hands = hands;
            }

            @Override
            public void match(Case c) {
                c.playing(shoe, players);
            }

            @Override
            public Phase withPlayerCard(Player p, Card c) {
                if (!players.containsKey(p)) {
                    throw new IllegalStateException("Player can't draw anymore.");
                }
                hands.playerDraws(p, c);
                return this;
            }

            @Override
            public Phase withDealerCard(Card c) {
                throw new IllegalStateException("Dealer can't draw yet.");
            }
        }

        class Resolving extends BaseState<Phase, Play> implements Phase {
            private final Shoe shoe;
            private final Hands hands;

            public Resolving(Shoe shoe, Hands hands) {
                this.shoe = shoe;
                this.hands = hands;
            }

            @Override
            public void match(Case c) {
                c.resolving(shoe);
            }

            @Override
            public Phase withPlayerCard(Player p, Card c) {
                throw new IllegalStateException("Only the dealer can draw at this phase.");
            }

            @Override
            public Phase withDealerCard(Card c) {
                hands.dealerDraws(c);
                return this;
            }
        }
    }
}
