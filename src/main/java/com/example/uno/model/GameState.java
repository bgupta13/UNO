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
    private int pendingDrawAmount = 0;
    private Card.Type pendingDrawType = null;

    private Card topCard;
    private Card.Color activeColor;

    private Card previousTopCardBeforeKarlMarx = null;

    private Player unoPendingPlayer = null;
    private boolean unoCalled = false;
    private GameService game;

    private volatile Player winner = null;

    private final ScheduledExecutorService aiExecutor = Executors.newSingleThreadScheduledExecutor();

    private final ReentrantLock aiLock = new ReentrantLock();

    private final List<GameListener> listeners = new java.util.concurrent.CopyOnWriteArrayList<>();

    private boolean aiTurnScheduled = false;

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
        return card != null &&
                (card.getType() == Card.Type.WILD ||
                 card.getType() == Card.Type.WILD_DRAW_FOUR);
    }

    public boolean needsTarget(Card card) {
        return card != null &&
               card.getType() == Card.Type.PARTY &&
               card.getPartyType() == Card.PartyType.SWAPPER;
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
            return false;
        }

        resolveUnoIfExpiredForNextAction(player);

        if (!player.equals(getCurrentPlayer())) {
            return false;
        }

        if (card == null || !isValidMove(card, topCard, activeColor)) {
            return false;
        }

        Hand hand = hands.get(player);

        if (hand == null || !hand.getCards().contains(card)) {
            return false;
        }

        if (!canPlayDuringStack(card)) {
            return false;
        }

        if (requiresChosenColor(card)) {
            if (chosenColor == null) {
                chosenColor = Card.Color.RED;
            }

            if (!isPlayableColor(chosenColor)) {
                return false;
            }
        }

        hand.removeCard(card);

        previousTopCardBeforeKarlMarx = topCard;

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
            unoCalled = false;
        }

        nextTurn(skip);
        autoResolveDrawStackIfCurrentPlayerCannotStack();
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

        // If there is a Draw Two stack, this player must take the full penalty
        // unless they have already played a Draw Two through playCard().
        if (pendingDrawAmount > 0) {
            for (int i = 0; i < pendingDrawAmount; i++) {
                hand.addCard(deck.drawCard());
            }

            clearPendingDraw();
            nextTurn(0);
            runAITurnsIfNeeded();
            return;
        }

        Card drawn = deck.drawCard();
        hand.addCard(drawn);

        if (isValidMove(drawn, topCard, activeColor)) {
            Card.Color chosenColor = getDrawnCardChosenColor(player, drawn, hand);
            playCard(player, drawn, chosenColor);
            return;
        }

        if (rules.isDrawUntilValidEnabled()) {
            while (true) {
                drawn = deck.drawCard();
                hand.addCard(drawn);

                if (isValidMove(drawn, topCard, activeColor)) {
                    Card.Color chosenColor = getDrawnCardChosenColor(player, drawn, hand);
                    playCard(player, drawn, chosenColor);
                    return;
                }
            }
        }

        nextTurn(0);
        runAITurnsIfNeeded();
        notifyUpdate();
    }

    private Card.Color getDrawnCardChosenColor(Player player, Card drawn, Hand hand) {
        if (!requiresChosenColor(drawn)) {
            return null;
        }

        if (player instanceof AIPlayer) {
            return ((AIPlayer) player).chooseWildColor(hand);
        }

        return Card.Color.RED;
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
                clearPendingDraw();
                return 1;

            case REVERSE:
                clearPendingDraw();
                direction *= -1;
                return players.size() == 2 ? 1 : 0;

            case DRAW_TWO:
                if (rules.isStackingEnabled()) {
                    pendingDrawAmount += 2;
                    pendingDrawType = Card.Type.DRAW_TWO;
                    return 0;
                } else {
                    applyDrawToNextPlayer(2);
                    clearPendingDraw();
                    return 1;
                }

            case WILD_DRAW_FOUR:
                // Per requested rule: only Draw Two can stack on Draw Two.
                // Wild Draw Four does not participate in stacking.
                applyDrawToNextPlayer(4);
                clearPendingDraw();
                return 1;

            default:
                clearPendingDraw();
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

    private boolean canPlayDuringStack(Card card) {
        if (pendingDrawAmount <= 0) {
            return true;
        }

        if (!rules.isStackingEnabled()) {
            return false;
        }

        // Only DRAW_TWO can continue a DRAW_TWO stack.
        return card != null &&
               pendingDrawType == Card.Type.DRAW_TWO &&
               card.getType() == Card.Type.DRAW_TWO;
    }

        private boolean currentPlayerHasStackableDrawCard() {
        if (pendingDrawAmount <= 0) {
            return false;
        }

        Hand hand = hands.get(getCurrentPlayer());

        if (hand == null) {
            return false;
        }

        for (Card card : hand.getCards()) {
            if (canPlayDuringStack(card)) {
                return true;
            }
        }

        return false;
    }

    /**
     * If a DRAW_TWO stack reaches a player who cannot continue the stack,
     * that player automatically draws the full pending amount and the turn advances.
     */
    private void autoResolveDrawStackIfCurrentPlayerCannotStack() {
        if (winner != null || pendingDrawAmount <= 0) {
            return;
        }

        if (currentPlayerHasStackableDrawCard()) {
            return;
        }

        Player penalizedPlayer = getCurrentPlayer();
        Hand hand = hands.get(penalizedPlayer);

        if (hand == null) {
            clearPendingDraw();
            nextTurn(0);
            return;
        }

        for (int i = 0; i < pendingDrawAmount; i++) {
            hand.addCard(deck.drawCard());
        }

        clearPendingDraw();
        nextTurn(0);
    }

    private void clearPendingDraw() {
        pendingDrawAmount = 0;
        pendingDrawType = null;
    }

    private void applyPartyCardEffect(Card card, Player player, Player targetPlayer) {
        if (card.getPartyType() == null) {
            return;
        }

        switch (card.getPartyType()) {
            case KARL_MARX:
                applyKarlMarx();
                scheduleKarlMarxTopCardRemoval();
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

    private void scheduleKarlMarxTopCardRemoval() {
        final Card oldTop = previousTopCardBeforeKarlMarx;

        new Thread(() -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            synchronized (GameState.this) {
                if (topCard != null &&
                    topCard.getType() == Card.Type.PARTY &&
                    topCard.getPartyType() == Card.PartyType.KARL_MARX &&
                    oldTop != null) {

                    topCard = oldTop;

                    if (topCard.getColor() == Card.Color.WILD) {
                        activeColor = Card.Color.RED;
                    } else {
                        activeColor = topCard.getColor();
                    }
                }
            }
        }).start();
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

        aiTurnScheduled = true;
        performAITurn();
    }

    public void performAITurn() {
        Player current = getCurrentPlayer();

        if (!(current instanceof AIPlayer)) {
            aiTurnScheduled = false;
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

                    Card chosen = ai.chooseCardToPlay(hand, topCard, activeColor, rules.isStackingEnabled());

                    aiTurnScheduled = false;
                    if (pendingDrawAmount > 0 && !canPlayDuringStack(chosen)) {
                        draw(ai);
                        return;
                    }

                    if (chosen == null) {
                        draw(ai);
                        return;
                    }

                    Card.Color chosenColor = null;

                    if (requiresChosenColor(chosen)) {
                        chosenColor = ai.chooseWildColor(hand);
                    }

                    Player target = null;

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
