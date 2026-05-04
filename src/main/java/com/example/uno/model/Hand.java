package com.example.uno.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Hand {

    private final List<Card> cards = new ArrayList<>();

    public void addCard(Card c) {
        if (c != null) {
            cards.add(c);
        }
    }

    public boolean removeCard(Card c) {
        return cards.remove(c);
    }

    public List<Card> getCards() {
        return cards;
    }

    public List<Card> getCardsReadOnly() {
        return Collections.unmodifiableList(cards);
    }

    public int size() {
        return cards.size();
    }

    public boolean isEmpty() {
        return cards.isEmpty();
    }
}
