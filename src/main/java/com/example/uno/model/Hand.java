package com.example.uno.model;

import java.util.*;

public class Hand {

    private List<Card> cards = new ArrayList<>();

    public void addCard(Card c) {
        cards.add(c);
    }

    public void removeCard(Card c) {
        cards.remove(c);
    }

    public List<Card> getCards() {
        return cards;
    }

    public boolean hasValidMove(Card topCard) {
        for (Card c : cards) {
            if (GameState.isValidMove(c, topCard)) {
                return true;
            }
        }
        return false;
    }
}
