package com.example.uno.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import com.example.uno.service.GameListener;
import com.example.uno.service.GameService;

public class GameState {

    private final List<Player> players;
    private final Map<Player, Hand> hands = new java.util.concurrent.ConcurrentHashMap<>();
    private final Deck deck;
    private final LobbyRules rules;

    private int currentPlayerIndex = 0;
    private int direction = 1;

    private Card topCard;
    private Card.Color activeColor;

    private Player unoPendingPlayer = null;
    private boolean unoCalled = false;
    private GameService game;

    private volatile Player winner = null;

    private final ScheduledExecutorService aiExecutor = Executors.newSingleThreadScheduledExecutor();

    private final ReentrantLock aiLock = new ReentrantLock();

    private final List<GameListener> listeners = new java.util.concurrent.CopyOnWriteArrayList<>();

    public GameState(List<Player> players, Deck deck, LobbyRules rules) {
        if (players == null || players.isEmpty()) {
            throw new IllegalArgumentException("Game must have at least one player.");
        }

        this.players = new ArrayList<>(players);
        this.deck = deck;
        this.rules = rules != null ? rules : new LobbyRules();


        for (Player p : this.players) {
            Hand hand = new Hand();

            for (int i = 0; i < 7; i++) {
                hand.addCard(deck.drawCard());
            }

            hands.put(p, hand);
        }


        do {
            topCard = deck.drawCard();
        } while (topCard.getType() == Card.Type.PARTY);

        
        deck.addToDiscard(topCard); 

        if (topCard.getColor() == Card.Color.WILD) {
            activeColor = Card.Color.RED;
        } else {
            activeColor = topCard.getColor();
        }
    }

    public Deck getDeck() {
        return deck;
    }

    public boolean isWild(Card card) {
        if (card.getType() == Card.Type.WILD ||
            card.getType() == Card.Type.WILD_DRAW_FOUR) {
            return true;
        }
        return false;
    }
    public boolean needsTarget(Card card) {
        if (card.getPartyType() == Card.PartyType.SWAPPER) {
            return true;
        }
        return false;
    }

    public Player getCurrentPlayer() {
        return players.get(currentPlayerIndex);
    }

    public void nextTurn(int skipCount) {
        currentPlayerIndex =
                (currentPlayerIndex + direction * (1 + skipCount) + players.size())
                        % players.size();
    }

    public boolean playCard(Player player, Card card) {
        return playCard(player, card, null, null);
    }

    public boolean playCard(Player player, Card card, Card.Color chosenColor) {
        return playCard(player, card, chosenColor, null);
    }

    public boolean playCard(Player player, Card card, Player targetPlayer) {
        return playCard(player, card, null, targetPlayer);
    }

    public boolean playCard(Player player, Card card, Card.Color chosenColor, Player targetPlayer) {
        if (winner != null) {
            System.out.println("i failed on line 101");
            return false;
        }

        resolveUnoIfExpiredForNextAction(player);

        if (!player.equals(getCurrentPlayer())) {
            System.out.println("i failed on line 108");
            return false;
        }

        if (card == null || !isValidMove(card, topCard, activeColor)) {
            System.out.println("i failed on line 113");
            return false;
        }

        if (requiresChosenColor(card)) {
            if (chosenColor == null) {
                chosenColor = Card.Color.RED;
            }

            if (!isPlayableColor(chosenColor)) {
                System.out.println("i failed on line 123");
                return false;
            }
        }

        Hand hand = hands.get(player);

        if (hand == null || !hand.getCards().contains(card)) {
            
            System.out.println("i failed on line 130");
            if(hand == null) { System.out.println("my hand is null");}
            else {System.out.println("my hand does not have this card");}
            return false;
        }

        hand.removeCard(card);

        deck.addToDiscard(card);
        topCard = card;

        if (requiresChosenColor(card)) {
            activeColor = chosenColor;
        } else {
            activeColor = card.getColor();
        }

        if (card.getType() == Card.Type.PARTY) {
            applyPartyCardEffect(card, player, targetPlayer);
        }

        int skip = applyCardEffect(card);

        Hand updatedHand = hands.get(player);

        if (updatedHand.isEmpty()) {
            unoPendingPlayer = null;
            unoCalled = false;
            winner = player;
            notifyGameEnd();
            return true;
        }

        if (updatedHand.size() == 1) {
            unoPendingPlayer = player;
            System.out.println("i failed on line 162");
            unoCalled = false;
        }

        nextTurn(skip);
        runAITurnsIfNeeded();

        notifyUpdate();
        return true;
    }

    public void draw(Player player) {
        if (winner != null) {
            return;
        }

        resolveUnoIfExpiredForNextAction(player);

        if (!player.equals(getCurrentPlayer())) {
            return;
        }

        Hand hand = hands.get(player);

        if (rules.isDrawUntilValidEnabled()) {
            while (true) {
                Card drawn = deck.drawCard();
                hand.addCard(drawn);

                if (isValidMove(drawn, topCard, activeColor)) {
                    break;
                }
            }
        } else {
            hand.addCard(deck.drawCard());
        }

        nextTurn(0);
        runAITurnsIfNeeded();
        notifyUpdate();
    }

    public boolean callUno(Player caller) {
        if (unoPendingPlayer == null || unoCalled) {
            return false;
        }

        unoCalled = true;

        if (!caller.equals(unoPendingPlayer)) {
            Hand penalizedHand = hands.get(unoPendingPlayer);
            penalizedHand.addCard(deck.drawCard());
            penalizedHand.addCard(deck.drawCard());

            unoPendingPlayer = null;
            unoCalled = false;
        }
        notifyUpdate();

        return true;
    }

    private void resolveUnoIfExpiredForNextAction(Player actingPlayer) {
        if (unoPendingPlayer == null || unoCalled) {
            return;
        }

        if (actingPlayer.equals(unoPendingPlayer)) {
            return;
        }

        Hand penalizedHand = hands.get(unoPendingPlayer);
        penalizedHand.addCard(deck.drawCard());
        penalizedHand.addCard(deck.drawCard());

        unoPendingPlayer = null;
        unoCalled = false;
    }

    private int applyCardEffect(Card card) {
        switch (card.getType()) {
            case SKIP:
                return 1;

            case REVERSE:
                direction *= -1;
                return players.size() == 2 ? 1 : 0;

            case DRAW_TWO:
                applyDrawToNextPlayer(2);
                return 1;

            case WILD_DRAW_FOUR:
                applyDrawToNextPlayer(4);
                return 1;

            default:
                return 0;
        }
    }

    private void applyDrawToNextPlayer(int amount) {
        int nextIndex =
                (currentPlayerIndex + direction + players.size()) % players.size();

        Player next = players.get(nextIndex);
        Hand nextHand = hands.get(next);

        for (int i = 0; i < amount; i++) {
            nextHand.addCard(deck.drawCard());
        }
    }

    private void applyPartyCardEffect(Card card, Player player, Player targetPlayer) {
        if (card.getPartyType() == null) {
            return;
        }

        switch (card.getPartyType()) {
            case KARL_MARX:
                applyKarlMarx();
                break;

            case SWAPPER:
                applySwapper(player, targetPlayer);
                break;

            case ROTATER:
                applyRotater();
                break;
        }
    }

    private void applyKarlMarx() {
        List<Card> allCards = new ArrayList<>();

        for (Player p : players) {
            Hand hand = hands.get(p);
            allCards.addAll(hand.getCards());
            hand.getCards().clear();
        }

        Collections.shuffle(allCards);

        int playerIndex = 0;

        for (Card card : allCards) {
            Player current = players.get(playerIndex);
            hands.get(current).addCard(card);
            playerIndex = (playerIndex + 1) % players.size();
        }
    }

    private void applySwapper(Player player, Player targetPlayer) {
        if (targetPlayer == null || player.equals(targetPlayer)) {
            return;
        }

        if (!players.contains(targetPlayer)) {
            return;
        }

        Hand playerHand = hands.get(player);
        Hand targetHand = hands.get(targetPlayer);

        hands.put(player, targetHand);
        hands.put(targetPlayer, playerHand);
    }

    private void applyRotater() {
        if (players.size() < 2) {
            return;
        }

        Hand lastHand = hands.get(players.get(players.size() - 1));

        for (int i = players.size() - 1; i > 0; i--) {
            Player current = players.get(i);
            Player previous = players.get(i - 1);

            hands.put(current, hands.get(previous));
        }

        hands.put(players.get(0), lastHand);
    }

    public static boolean isValidMove(Card played, Card top, Card.Color activeColor) {
        if (played == null || top == null) {
            return false;
        }

        if (played.getType() == Card.Type.WILD ||
            played.getType() == Card.Type.WILD_DRAW_FOUR ||
            played.getType() == Card.Type.PARTY) {
            return true;
        }

        if (played.getColor() == activeColor) {
            return true;
        }

        if (played.getType() == Card.Type.NUMBER &&
            top.getType() == Card.Type.NUMBER &&
            played.getNumber() == top.getNumber()) {
            return true;
        }

        return played.getType() == top.getType() && played.getType() != Card.Type.NUMBER;
    }

    public static boolean isValidMove(Card played, Card top) {
        return isValidMove(played, top, top.getColor());
    }

    public boolean requiresChosenColor(Card card) {
        return card.getType() == Card.Type.WILD ||
               card.getType() == Card.Type.WILD_DRAW_FOUR ||
               card.getType() == Card.Type.PARTY;
    }

    private boolean isPlayableColor(Card.Color color) {
        return color == Card.Color.RED ||
               color == Card.Color.BLUE ||
               color == Card.Color.GREEN ||
               color == Card.Color.YELLOW;
    }

    public void runAITurnsIfNeeded() {
        if (winner != null) return;

        Player current = getCurrentPlayer();

        if (current instanceof AIPlayer) {
            performAITurn();
        }
    }

    public void performAITurn() {
        Player current = getCurrentPlayer();

        if (!(current instanceof AIPlayer)) {
            return;
        }

        

        aiExecutor.schedule(() -> {
            try {
                AIPlayer ai = (AIPlayer) current;

                Hand hand = hands.get(ai);

                Card chosen = ai.chooseCardToPlay(
                        hand,
                        topCard,
                        activeColor,
                        rules.isStackingEnabled()
                );

                if (chosen == null) {
                    draw(ai);
                    return;
                }

                Card.Color chosenColor = null;

                if (requiresChosenColor(chosen)) {
                    chosenColor = ai.chooseWildColor(hand);
                }

                Player target = null;

                if (chosen.getType() == Card.Type.PARTY &&
                    chosen.getPartyType() == Card.PartyType.SWAPPER) {
                    target = chooseRandomTarget(ai);
                }

                playCard(ai, chosen, chosenColor, target);

            } finally {
                aiLock.unlock();
            }
        }, 1200, TimeUnit.MILLISECONDS);

        
    }

    private Player chooseRandomTarget(Player current) {
        List<Player> possibleTargets = new ArrayList<>();

        for (Player p : players) {
            if (!p.equals(current)) {
                possibleTargets.add(p);
            }
        }

        if (possibleTargets.isEmpty()) {
            return null;
        }

        return possibleTargets.get(new Random().nextInt(possibleTargets.size()));
    }

    public Map<Player, Hand> getHands() {
        return hands;
    }

    public Hand getHand(Player player) {
        return hands.get(player);
    }

    public Card getTopCard() {
        return topCard;
    }

    public Card.Color getActiveColor() {
        return activeColor;
    }

    public LobbyRules getRules() {
        return rules;
    }

    public List<Player> getPlayers() {
        return Collections.unmodifiableList(players);
    }

    public Player getUnoPendingPlayer() {
        return unoPendingPlayer;
    }

    public boolean isUnoCalled() {
        return unoCalled;
    }

    public Player getWinner() {
        return winner;
    }

    public boolean isGameOver() {
        return winner != null;
    }

    // Listeners

    public void addListener(GameListener listener) {
    listeners.add(listener);
}

    public void removeListener(GameListener listener) {
        listeners.remove(listener);
    }

    private void notifyUpdate() {
    for (GameListener l : listeners) {
        l.onGameUpdated(this);
    }
}

    private void notifyGameEnd() {
        for (GameListener l : listeners) {
            l.onGameEnded(this);
        }
    }

}
