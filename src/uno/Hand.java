package uno;

import java.util.ArrayList;
import java.util.List;

public class Hand {
    private List<Card> cards = new ArrayList<>();

    public void addCard(Card card) {
        cards.add(card);
    }

    public void removeCard(Card card) {
        cards.remove(card);
    }

    public List<Card> getCards() {
        return cards;
    }

    public boolean hasValidMove(Card topCard, Card.Color activeColor) {
        for (Card card : cards) {
            if (GameState.isValidMove(card, topCard, activeColor)) {
                return true;
            }
        }
        return false;
    }
}
