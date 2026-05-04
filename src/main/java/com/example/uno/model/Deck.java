package com.example.uno.model;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Stack;

public class Deck {

    private final Stack<Card> drawPile = new Stack<>();
    private final List<Card> discardPile = new ArrayList<>();
    private final List<Card> cardPool = new ArrayList<>();
    private final Random random = new Random();

    public Deck(Collection<Card.PartyType> enabledPartyCards) {
        buildCardPool(enabledPartyCards);
        buildInitialDeck();
        shuffle();
    }

    private void buildCardPool(Collection<Card.PartyType> partyCards) {
        Card.Color[] colors = {
                Card.Color.RED,
                Card.Color.BLUE,
                Card.Color.GREEN,
                Card.Color.YELLOW
        };

        for (Card.Color color : colors) {
            for (int number = 0; number <= 9; number++) {
                for (int w = 0; w < 8; w++) {
                    cardPool.add(new Card(color, number));
                }
            }
        }

        for (Card.Color color : colors) {
            for (int w = 0; w < 3; w++) {
                cardPool.add(new Card(color, Card.Type.SKIP));
                cardPool.add(new Card(color, Card.Type.REVERSE));
                cardPool.add(new Card(color, Card.Type.DRAW_TWO));
            }
        }

        for (int w = 0; w < 4; w++) {
            cardPool.add(new Card(Card.Color.WILD, Card.Type.WILD));
            cardPool.add(new Card(Card.Color.WILD, Card.Type.WILD_DRAW_FOUR));
        }

        if (partyCards != null) {
            for (Card.PartyType partyType : partyCards) {
                for (int w = 0; w < 100; w++) {
                    cardPool.add(new Card(partyType));
                }
            }
        }
    }

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
            return generateFromPool();
        }

        return drawPile.pop();
    }

    public void addToDiscard(Card card) {
        if (card != null) {
            discardPile.add(card);
        }
    }

    public Card getTopDiscard() {
        if (discardPile.isEmpty()) {
            return null;
        }

        return discardPile.get(discardPile.size() - 1);
    }

    public int cardsRemainingBeforeInfiniteGeneration() {
        return drawPile.size();
    }

    private Card generateFromPool() {
        Card template = cardPool.get(random.nextInt(cardPool.size()));
        return cloneCard(template);
    }

    private Card cloneCard(Card c) {
        if (c.getType() == Card.Type.NUMBER) {
            return new Card(c.getColor(), c.getNumber());
        }

        if (c.getType() == Card.Type.PARTY) {
            return new Card(c.getPartyType());
        }

        return new Card(c.getColor(), c.getType());
    }
}
