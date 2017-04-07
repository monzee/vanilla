package ph.codeia.sacdemo.blackjack;

import java.util.Random;

import ph.codeia.sacdemo.deck.Card;
import ph.codeia.sacdemo.deck.Shoe;

/**
 * This file is a part of the vanilla project.
 */

public class HeadToHead implements BlackJack {

    private static final Play PLAYER_DRAW = (round, player) -> {
        Card card = round.shoe.draw();
        player.playerDrew(card);
        return round.withPlayerCard(card);
    };

    private static final Play DEALER_DRAW = (round, player) -> {
        Card card = round.shoe.draw();
        player.dealerDrew(card);
        return round.withDealerCard(card);
    };

    private static final Play HOLE_CARD = (round, player) -> {
        Card card = round.shoe.draw();
        player.holeDrawn();
        return round.withDealerCard(card);
    };

    private static final int DECK_COUNT = 4;
    private static final int LAST_QUARTER = DECK_COUNT * 52 * 3 / 4;
    private final Shoe shoe;
    private final Random rng;

    public HeadToHead() {
        this(new Shoe.Legit(DECK_COUNT, LAST_QUARTER), new Random());
    }

    public HeadToHead(Shoe shoe, Random rng) {
        this.shoe = shoe;
        this.rng = rng;
    }

    @Override
    public Play start(int wager) {
        return (round, player) -> {
            if (!shoe.spent()) {
                shoe.shuffle(rng);
            }
            return Round.start(wager, shoe)
                    .plus(PLAYER_DRAW)
                    .plus(PLAYER_DRAW)
                    .plus(DEALER_DRAW)
                    .plus(HOLE_CARD)
                    .plus(prelude());
        };
    }

    @Override
    public Play resume() {
        return refrain();
    }

    private static Round start(Round round) {
        return round.withCanHit(true).withCanSurrender(true);
    }

    private static Round resume(Round round) {
        return round.withCanHit(true).withCanSurrender(false);
    }

    private static Round gameOver(Round round) {
        return round.withCanHit(false).withCanSurrender(false);
    }

    private static Play prelude() {
        return (round, player) -> new Score.Case() {
            Round next = round;

            /**
             * - cannot hit or surrender if player or dealer got a blackjack
             * - push if both have blackjacks
             * - else offer surrender
             *
             * @return new round state
             */
            Round next() {
                Score.Of.hand(round.dealer).match(this);
                return next.plus(refrain());
            }

            void checkPlayerHand() {
                Score.Of.hand(round.player).match(new Score.Case() {
                    @Override
                    public void bust(int total) {
                        throw new IllegalStateException("Bust on opening hand");
                    }

                    @Override
                    public void soft(int total) {
                        next = start(round);
                    }

                    @Override
                    public void hard(int total) {
                        next = start(round);
                    }

                    @Override
                    public void blackJack() {
                        next = gameOver(round);
                    }
                });
            }

            @Override
            public void bust(int total) {
                throw new IllegalStateException("Bust on opening hand");
            }

            @Override
            public void soft(int total) {
                checkPlayerHand();
            }

            @Override
            public void hard(int total) {
                checkPlayerHand();
            }

            @Override
            public void blackJack() {
                next = gameOver(round);
            }
        }.next();
    }

    private static Play refrain() {
        return (round, player) -> {
            if (round.canSurrender) {
                player.offerSurrender(hit(), stand(), doubleDown(), surrender());
                return round;
            } else if (round.canHit) {
                player.prompt(hit(), stand(), doubleDown());
                return round;
            } else {
                return round.plus(stand());
            }
        };
    }

    private static Play hit() {
        return (round, player) -> resume(round)
                .plus(PLAYER_DRAW)
                .plus((futureRound, futurePlayer) -> new Score.Case() {
                    Round next = futureRound;

                    Round next() {
                        Score.Of.hand(futureRound.player).match(this);
                        return next.plus(refrain());
                    }

                    @Override
                    public void bust(int total) {
                        next = gameOver(futureRound);
                    }

                    @Override
                    public void soft(int total) {
                        if (total == 21) {
                            next = gameOver(futureRound);
                        }
                    }

                    @Override
                    public void hard(int total) {
                        soft(total);
                    }

                    @Override
                    public void blackJack() {
                        throw new IllegalStateException("Blackjack after a hit");
                    }
                }.next());
    }

    private static Play stand() {
        return (round, player) -> {
            player.holeRevealed(round.dealer.get(1));
            return gameOver(round).plus(coda());
        };
    }

    private static Play doubleDown() {
        return (round, player) -> {
            int newWager = round.pot * 2;
            player.anteUp(newWager);
            return round.withPot(newWager).plus(PLAYER_DRAW).plus(stand());
        };
    }

    private static Play surrender() {
        return (round, player) -> round.withSurrendered(true).plus(stand());
    }

    private static Play coda() {
        return (round, player) -> new Score.Case() {
            Round next = round;

            /**
             * - if surrendered: take half of wager, game over.
             * - else get player score
             *   - if bust: take wager, game over.
             *   - if blackjack, get dealer score
             *     - if blackjack: push, game over.
             *     - else pay out 1.5x wager, game over.
             *   - else get dealer score
             *     - if bust: pay out wager, game over.
             *     - if blackjack: take wager, game over.
             *     - if lt 17 or soft 17: dealer draw, loop
             *     - if gt 17 or hard 17, compare player to dealer
             *       - if ==: push, game over.
             *       - if gt: pay out wager, game over.
             *       - else take wager, game over.
             *
             * @return new state or the same instance if game is over
             */
            Round next() {
                if (round.surrendered) {
                    player.roundOver(Outcome.SURRENDERED, -round.pot / 2);
                } else {
                    Score.Of.hand(round.player).match(this);
                }
                return next;
            }

            @Override
            public void bust(int total) {
                player.roundOver(Outcome.BUST, -round.pot);
            }

            @Override
            public void soft(int playerScore) {
                Score.Of.hand(round.dealer).match(new Score.Case() {
                    void payout(int dealerScore) {
                        if (playerScore == dealerScore) {
                            player.roundOver(Outcome.PUSH, 0);
                        } else if (playerScore < dealerScore) {
                            player.roundOver(Outcome.LOSS, -round.pot);
                        } else {
                            player.roundOver(Outcome.WIN, round.pot);
                        }
                    }

                    @Override
                    public void bust(int total) {
                        player.roundOver(Outcome.DEALER_BUST, round.pot);
                    }

                    @Override
                    public void soft(int total) {
                        if (total <= 17) {
                            next = round.plus(DEALER_DRAW).plus(coda());
                        } else {
                            payout(total);
                        }
                    }

                    @Override
                    public void hard(int total) {
                        if (total < 17) {
                            next = round.plus(DEALER_DRAW).plus(coda());
                        } else {
                            payout(total);
                        }
                    }

                    @Override
                    public void blackJack() {
                        player.roundOver(Outcome.DEALER_BLACKJACK, -round.pot);
                    }
                });
            }

            @Override
            public void hard(int total) {
                soft(total);
            }

            @Override
            public void blackJack() {
                Score.Of.hand(round.dealer).match(new Score.Case() {
                    void payout() {
                        player.roundOver(Outcome.BLACKJACK, round.pot * 3 / 2);
                    }

                    @Override
                    public void bust(int total) {
                        payout();
                    }

                    @Override
                    public void soft(int total) {
                        payout();
                    }

                    @Override
                    public void hard(int total) {
                        payout();
                    }

                    @Override
                    public void blackJack() {
                        player.roundOver(Outcome.PUSH, 0);
                    }
                });
            }
        }.next();
    }
}
