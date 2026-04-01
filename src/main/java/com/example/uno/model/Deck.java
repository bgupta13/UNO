package com.example.uno.model;

import java.util.*;

public class Deck {

    private Stack<Card> drawPile = new Stack<>();
    private List<Card> discardPile = new ArrayList<>();
    private List<Card> cardPool = new ArrayList<>();

    private Random random = new Random();

    public Deck(List<String> enabledPartyCards) {
        buildCardPool(enabledPartyCards);
        buildInitialDeck();
        shuffle();
    }

    // Build weighted pool (controls probability)
    private void buildCardPool(List<String> partyCards) {

        Card.Color[] colors = {
                Card.Color.RED,
                Card.Color.BLUE,
                Card.Color.GREEN,
                Card.Color.YELLOW
        };

        // NUMBER CARDS (high weight ~67%)
        for (Card.Color color : colors) {
            for (int i = 0; i <= 9; i++) {
                for (int w = 0; w < 8; w++) { // weight multiplier
                    cardPool.add(new Card(color, i));
                }
            }
        }

        // SKIP (~7%)
        for (Card.Color color : colors) {
            for (int w = 0; w < 3; w++) {
                cardPool.add(new Card(color, Card.Type.SKIP));
            }
        }

        // REVERSE (~7%)
        for (Card.Color color : colors) {
            for (int w = 0; w < 3; w++) {
                cardPool.add(new Card(color, Card.Type.REVERSE));
            }
        }

        // DRAW TWO (~7%)
        for (Card.Color color : colors) {
            for (int w = 0; w < 3; w++) {
                cardPool.add(new Card(color, Card.Type.DRAW_TWO));
            }
        }

        // WILD (~3.5%)
        for (int w = 0; w < 4; w++) {
            cardPool.add(new Card(Card.Color.WILD, Card.Type.WILD));
        }

        // WILD DRAW FOUR (~3.5%)
        for (int w = 0; w < 4; w++) {
            cardPool.add(new Card(Card.Color.WILD, Card.Type.WILD_DRAW_FOUR));
        }

        // PARTY CARDS (~up to 5%)
        if (partyCards != null) {
            for (String name : partyCards) {
                for (int w = 0; w < 2; w++) {
                    cardPool.add(new Card(name));
                }
            }
        }
    }

    // Initial finite deck (optional realism)
    private void buildInitialDeck() {
        for (int i = 0; i < 100; i++) {
            drawPile.add(generateFromPool());
        }
    }

    public void shuffle() {
        Collections.shuffle(drawPile);
    }

    public Card drawCard() {
        if (drawPile.isEmpty()) {
            return generateFromPool(); // infinite behavior
        }
        return drawPile.pop();
    }

    public void addToDiscard(Card card) {
        discardPile.add(card);
    }

    public Card getTopDiscard() {
        if (discardPile.isEmpty()) return null;
        return discardPile.get(discardPile.size() - 1);
    }

    // Core generator using weighted pool
    private Card generateFromPool() {
        int index = random.nextInt(cardPool.size());
        Card template = cardPool.get(index);

        return cloneCard(template);
    }

    // Prevent shared references
    private Card cloneCard(Card c) {
        if (c.getType() == Card.Type.NUMBER) {
            return new Card(c.getColor(), c.getNumber());
        } else if (c.getType() == Card.Type.PARTY) {
            return new Card(c.getPartyName());
        } else {
            return new Card(c.getColor(), c.getType());
        }
    }
}