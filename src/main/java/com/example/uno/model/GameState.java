package com.example.uno.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

import com.example.uno.service.GameListener;

public class GameState {

    private final List<Player> players;
    private final Map<Player, Hand> hands = new HashMap<>();
    private final Deck deck;
    private final LobbyRules rules;
    private final List<GameListener> listeners = new CopyOnWriteArrayList<>();

    private int currentPlayerIndex = 0;
    private int direction = 1;

    private int pendingDrawAmount = 0;
    private Card.Type pendingDrawType = null;

    private Card topCard;
    private Card.Color activeColor;
    private Card previousTopCardBeforeKarlMarx;

    private Player unoPendingPlayer = null;
    private boolean unoCalled = false;

    private Player winner = null;
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

    public void addListener(GameListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeListener(GameListener listener) {
        listeners.remove(listener);
    }

    private void notifyGameUpdated() {
        for (GameListener listener : listeners) {
            listener.onGameUpdated(this);
        }
    }

    private void notifyGameEnded() {
        for (GameListener listener : listeners) {
            listener.onGameEnded(this);
        }
    }

    public Deck getDeck() {
        return deck;
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

    public synchronized boolean playCard(Player player, Card card, Card.Color chosenColor, Player targetPlayer) {
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

        Hand hand = hands.get(player);

        if (hand == null || !hand.getCards().contains(card)) {
            return false;
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
            notifyGameUpdated();
            notifyGameEnded();
            return true;
        }

        if (updatedHand.size() == 1) {
            unoPendingPlayer = player;
            unoCalled = false;
        }

        nextTurn(skip);
        resolvePendingDrawAutomaticallyIfNeeded();
        notifyGameUpdated();
        runAITurnsIfNeeded();
        return true;
    }

    public synchronized void draw(Player player) {
        if (winner != null) {
            return;
        }

        resolveUnoIfExpiredForNextAction(player);

        if (!player.equals(getCurrentPlayer())) {
            return;
        }

        Hand hand = hands.get(player);

        if (pendingDrawAmount > 0) {
            drawPendingCards(hand);
            clearPendingDraw();
            nextTurn(0);
            notifyGameUpdated();
            runAITurnsIfNeeded();
            return;
        }

        Card drawn = deck.drawCard();
        hand.addCard(drawn);

        if (isValidMove(drawn, topCard, activeColor)) {
            Card.Color chosenColor = getDrawnCardChosenColor(player, drawn, hand);
            Player target = getDrawnCardTarget(player, drawn);
            playCard(player, drawn, chosenColor, target);
            return;
        }

        if (rules.isDrawUntilValidEnabled()) {
            while (true) {
                drawn = deck.drawCard();
                hand.addCard(drawn);

                if (isValidMove(drawn, topCard, activeColor)) {
                    Card.Color chosenColor = getDrawnCardChosenColor(player, drawn, hand);
                    Player target = getDrawnCardTarget(player, drawn);
                    playCard(player, drawn, chosenColor, target);
                    return;
                }
            }
        }

        nextTurn(0);
        notifyGameUpdated();
        runAITurnsIfNeeded();
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

    private Player getDrawnCardTarget(Player player, Card drawn) {
        if (drawn != null &&
            drawn.getType() == Card.Type.PARTY &&
            drawn.getPartyType() == Card.PartyType.SWAPPER &&
            player instanceof AIPlayer) {
            return chooseRandomTarget(player);
        }

        return null;
    }

    public synchronized boolean callUno(Player caller) {
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

        notifyGameUpdated();
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
                }

                applyDrawToNextPlayer(2);
                clearPendingDraw();
                return 1;

            case WILD_DRAW_FOUR:
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

        return card != null &&
               pendingDrawType == Card.Type.DRAW_TWO &&
               card.getType() == Card.Type.DRAW_TWO;
    }

    private void resolvePendingDrawAutomaticallyIfNeeded() {
        while (winner == null && pendingDrawAmount > 0) {
            Player current = getCurrentPlayer();
            Hand hand = hands.get(current);

            if (hasStackableDrawTwo(hand)) {
                return;
            }

            drawPendingCards(hand);
            clearPendingDraw();
            nextTurn(0);
        }
    }

    private boolean hasStackableDrawTwo(Hand hand) {
        if (hand == null) {
            return false;
        }

        for (Card card : hand.getCards()) {
            if (card.getType() == Card.Type.DRAW_TWO) {
                return true;
            }
        }

        return false;
    }

    private void drawPendingCards(Hand hand) {
        for (int i = 0; i < pendingDrawAmount; i++) {
            hand.addCard(deck.drawCard());
        }
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
                scheduleKarlMarxRemoval();
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

    public boolean requiresChosenColor(Card card) {
        return card != null &&
               (card.getType() == Card.Type.WILD ||
                card.getType() == Card.Type.WILD_DRAW_FOUR ||
                card.getType() == Card.Type.PARTY);
    }

    private boolean isPlayableColor(Card.Color color) {
        return color == Card.Color.RED ||
               color == Card.Color.BLUE ||
               color == Card.Color.GREEN ||
               color == Card.Color.YELLOW;
    }

    public synchronized void runAITurnsIfNeeded() {
        if (winner != null) {
            return;
        }

        if (!(getCurrentPlayer() instanceof AIPlayer)) {
            aiTurnScheduled = false;
            return;
        }

        if (aiTurnScheduled) {
            return;
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

        new Thread(() -> {
            try {
                Thread.sleep(2000);

                synchronized (GameState.this) {
                    if (winner != null || !current.equals(getCurrentPlayer())) {
                        aiTurnScheduled = false;
                        return;
                    }

                    AIPlayer ai = (AIPlayer) current;
                    Hand hand = hands.get(ai);

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

                    if (chosen.getType() == Card.Type.PARTY &&
                        chosen.getPartyType() == Card.PartyType.SWAPPER) {
                        target = chooseRandomTarget(ai);
                    }

                    playCard(ai, chosen, chosenColor, target);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                aiTurnScheduled = false;
            }
        }).start();
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

    private void scheduleKarlMarxRemoval() {
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

                    deck.removeTopDiscard();
                    topCard = oldTop;

                    if (topCard.getColor() == Card.Color.WILD) {
                        activeColor = Card.Color.RED;
                    } else {
                        activeColor = topCard.getColor();
                    }

                    notifyGameUpdated();
                }
            }
        }).start();
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
}
