package com.example.uno.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class GameState {

    private final List<Player> players;
    private final Map<Player, Hand> hands = new HashMap<>();
    private final Deck deck;
    private final LobbyRules rules;

    private int currentPlayerIndex = 0;
    private int direction = 1;

    private Card topCard;
    private Card.Color activeColor;
    private Card previousTopCardBeforeKarlMarx;

    private Player unoPendingPlayer = null;
    private boolean unoCalled = false;

    private Player winner = null;

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

        if (topCard.getColor() == Card.Color.WILD) {
            activeColor = Card.Color.RED;
        } else {
            activeColor = topCard.getColor();
        }
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
            return true;
        }

        if (updatedHand.size() == 1) {
            unoPendingPlayer = player;
            unoCalled = false;
        }

        nextTurn(skip);
        runAITurnsIfNeeded();
        return true;
    }

    public void draw(Player player) {
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
                    Card.Color chosenColor = getDrawnCardChosenColor(player, drawn, hand);
                    playCard(player, drawn, chosenColor);

                    runAITurnsIfNeeded();   // ← ADD THIS
                    return;
                }
            }
        } else {
            Card drawn = deck.drawCard();
            hand.addCard(drawn);

            if (isValidMove(drawn, topCard, activeColor)) {
                Card.Color chosenColor = getDrawnCardChosenColor(player, drawn, hand);
                playCard(player, drawn, chosenColor);

                runAITurnsIfNeeded();   // ← ADD THIS
                return;
            }

            nextTurn(0);
            runAITurnsIfNeeded();       // ← ADD THIS
        }
    }
    
    private Card.Color getDrawnCardChosenColor(Player player, Card drawn, Hand hand) {
        if (drawn.getType() != Card.Type.WILD &&
            drawn.getType() != Card.Type.WILD_DRAW_FOUR &&
            drawn.getType() != Card.Type.PARTY) {
            return null;
        }

        if (player instanceof AIPlayer) {
            return ((AIPlayer) player).chooseWildColor(hand);
        }

        // Temporary default for human players.
        // Replace with UI color selection later.
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

        return played.getType() == top.getType();
    }

    public static boolean isValidMove(Card played, Card top) {
        return isValidMove(played, top, top.getColor());
    }

    private boolean requiresChosenColor(Card card) {
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
        int safety = 0;

        while (winner == null && getCurrentPlayer() instanceof AIPlayer && safety < players.size() * 5) {
            performAITurn();
            safety++;
        }
    }

    public void performAITurn() {
        Player current = getCurrentPlayer();

        if (!(current instanceof AIPlayer)) {
            return;
        }

        AIPlayer ai = (AIPlayer) current;
        Hand hand = hands.get(ai);

        Card chosen = ai.chooseCardToPlay(hand, topCard, activeColor, rules.isStackingEnabled());

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
        new Thread(() -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            synchronized (this) {
                if (topCard != null &&
                    topCard.getType() == Card.Type.PARTY &&
                    topCard.getPartyType() == Card.PartyType.KARL_MARX) {

                    deck.removeTopDiscard();

                    if (previousTopCardBeforeKarlMarx != null) {
                        topCard = previousTopCardBeforeKarlMarx;
                        activeColor = topCard.getColor();
                    }
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
