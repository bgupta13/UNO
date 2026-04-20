package com.example.uno.model;

import java.util.*;

import com.example.uno.model.Card.Type;
import com.vaadin.flow.component.charts.model.style.Color;

public class GameState {

    private List<Player> players;
    private Map<Player, Hand> hands = new HashMap<>();
    private Deck deck;

    private int currentPlayerIndex = 0;
    private int direction = 1; // 1 = clockwise, -1 = reverse

    private Card topCard;

    // Rule toggles (not implemented)
    private boolean stackingEnabled = true;
    private boolean drawUntilValid = true;

    public GameState(List<Player> players, Deck deck) {
        this.players = players;
        this.deck = deck;

        for (Player p : players) {
    System.out.println("Player object: " + p + " | name: " + p.getName());
}

        // Initialize hands
        for (Player p : players) {
            Hand hand = new Hand();
            for (int i = 0; i < 7; i++) {
                hand.addCard(deck.drawCard());
            }
            hands.put(p, hand);
        }

        topCard = deck.drawCard();
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

    // --- PLAY CARD ---
    public boolean playCard(Player player, Card card) {

        if (player != getCurrentPlayer()) return false;

        if (!isValidMove(card, topCard)) return false;

        Hand hand = hands.get(player);
        hand.removeCard(card);

        deck.addToDiscard(card);
        topCard = card;

        int skip = applyCardEffect(card);

        // UNO penalty check (simplified)
        if (hand.getCards().size() == 1) {
            // assume player forgot to call UNO
            hand.addCard(deck.drawCard());
            hand.addCard(deck.drawCard());
        }

        nextTurn(skip);
        return true;
    }

    // --- DRAW LOGIC ---
    public void draw(Player player) {

        if (!player.getName().equals(getCurrentPlayer().getName())) return; // Modified for Spring Boot

        Hand hand = hands.get(player);

        if (drawUntilValid) {
            while (true) {
                Card drawn = deck.drawCard();
                hand.addCard(drawn);

                if (isValidMove(drawn, topCard)) break;
            }
        } else {
            hand.addCard(deck.drawCard());
        }

        nextTurn(0);
    }

    // --- CARD EFFECTS ---
    private int applyCardEffect(Card card) {

        switch (card.getType()) {

            case SKIP:
                return 1;

            case REVERSE:
                direction *= -1;
                return 0;

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

    // --- VALIDATION ---
    public static boolean isValidMove(Card played, Card top) {

        // Wild always valid
        if (played.getType() == Card.Type.WILD ||
            played.getType() == Card.Type.WILD_DRAW_FOUR) {
            return true;
        }

        // Match color
        if (played.getColor() == top.getColor()) {
            return true;
        }

        // Match number
        if (played.getType() == Card.Type.NUMBER &&
            top.getType() == Card.Type.NUMBER &&
            played.getNumber() == top.getNumber()) {
            return true;
        }

        // Match type (skip, reverse, etc.)
        return played.getType() == top.getType();
    }

    // Added for the GameView.java
    public Map<Player, Hand> getHands() {
        return hands;
    }

    public Card getTopCard() {
        return topCard;
    }

    public boolean compareToDiscard(Card playerCard) {
        Card.Color discardColor = deck.getTopDiscard().getColor();
        Card.Color playerColor = playerCard.getColor();
        boolean colorEqual = discardColor.equals(playerColor) || playerColor.equals(Card.Color.WILD);

        int discardNumber = deck.getTopDiscard().getNumber();
        int playerNumber = playerCard.getNumber();
        boolean numberEqual = discardNumber == playerNumber;

        Card.Type discardType = deck.getTopDiscard().getType();
        Card.Type playerType = playerCard.getType();
        boolean typeEqual = discardType.equals(playerType) && !discardType.equals(Card.Type.NUMBER);
        
        if(colorEqual || numberEqual || typeEqual) {
            return true;
        }
        return false;
    }
}
