package ph.codeia.sacdemo.blackjack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ph.codeia.arch.sm.Machine;
import ph.codeia.arch.sm.RootState;
import ph.codeia.arch.sm.Sm;
import ph.codeia.sacdemo.deck.Card;
import ph.codeia.sacdemo.deck.Shoe;

/**
 * This file is a part of the vanilla project.
 */

public interface Table {

    interface Io {
        void prompt(Play hit, Play stand, Play doubleDown);
        void prompt(Play hit, Play stand, Play doubleDown, Play surrender);
        void prompt(Play hit, Play stand, Play doubleDown, Play split, Play surrender);
        void split(Play confirm);
        void offerInsurance(Play accept);
        void holeDrawn();
        void holeRevealed(Card hole);
    }

    interface Player {}

    interface Play extends Sm.Action<Game, Play, Io> {}

    class Dealer implements Phase.Case {
        private static final Play DRAW = (game, io) -> game.dealerDraws();
        private final Machine.Fixed<Game, Play, Io> machine;

        public Dealer(Machine.Fixed<Game, Play, Io> machine) {
            this.machine = machine;
        }

        @Override
        public void starting(Shoe shoe, List<Player> players) {
            machine.apply(DRAW);
            machine.apply(DRAW);
            machine.apply((game, io) -> {
                io.holeDrawn();
                return game.next(p -> p.playing(shoe, players));
            });
        }

        @Override
        public void playing(Shoe shoe, List<Player> players) {

        }

        @Override
        public void resolving(Shoe shoe) {

        }

        @Override
        public void over() {

        }
    }

    class Hands {
        private final List<Card> dealerHand = new ArrayList<>();
        private final Map<Player, List<Card>> playerHands = new HashMap<>();

        Hands addToPlayer(Player p, Card c) {
            if (!playerHands.containsKey(p)) {
                playerHands.put(p, new ArrayList<>());
            }
            playerHands.get(p).add(c);
            return this;
        }

        Hands addToDealer(Card c) {
            dealerHand.add(c);
            return this;
        }
    }

    interface Phase {
        void match(Case p);

        interface Case {
            void starting(Shoe shoe, List<Player> players);
            void playing(Shoe shoe, List<Player> players);
            void resolving(Shoe shoe);
            void over();
        }
    }

    class Game extends RootState<Game, Play> {
        Hands hands = new Hands();
        Phase phase;

        public Game(Shoe shoe, List<Player> players) {
            phase = p -> p.starting(shoe, players);
        }

        public Game next(Phase phase) {
            this.phase = phase;
            return this;
        }

        public Game playerDraws(Player p) {
            phase.match(new Phase.Case() {
                @Override
                public void starting(Shoe shoe, List<Player> players) {
                    if (shoe.spent()) {
                        throw new IllegalStateException("Shoe needs to be shuffled");
                    }
                    hands = hands.addToPlayer(p, shoe.draw());
                }

                @Override
                public void playing(Shoe shoe, List<Player> players) {
                    if (players.contains(p)) {
                        hands = hands.addToPlayer(p, shoe.draw());
                    } else {
                        throw new IllegalStateException("Player has folded and can't draw anymore");
                    }
                }

                @Override
                public void resolving(Shoe shoe) {
                    throw new IllegalStateException("Only the dealer can draw at this phase");
                }

                @Override
                public void over() {
                    throw new IllegalStateException("The round is over");
                }
            });
            return this;
        }

        public Game dealerDraws() {
            phase.match(new Phase.Case() {
                @Override
                public void starting(Shoe shoe, List<Player> players) {
                    hands = hands.addToDealer(shoe.draw());
                }

                @Override
                public void playing(Shoe shoe, List<Player> players) {
                    throw new IllegalStateException("Dealer can't draw until all players have stood");
                }

                @Override
                public void resolving(Shoe shoe) {
                    hands = hands.addToDealer(shoe.draw());
                }

                @Override
                public void over() {
                    throw new IllegalStateException("The round is over");
                }
            });
            return this;
        }
    }

}
