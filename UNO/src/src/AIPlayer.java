package src;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class AIPlayer extends Player {

    private final Random random = new Random();

    public AIPlayer(String name) {
        super(name);
    }

    @Override
    public boolean isAI() {
        return true;
    }

    public Card chooseCardToPlay(
            Hand hand,
            Card topCard,
            Card.Color activeColor,
            boolean stackingEnabled
    ) {
        List<Card> validCards = new ArrayList<>();

        for (Card card : hand.getCards()) {
            if (GameState.isValidMove(card, topCard, activeColor)) {
                validCards.add(card);
            }
        }

        if (validCards.isEmpty()) {
            return null;
        }

        if (stackingEnabled &&
            (topCard.getType() == Card.Type.DRAW_TWO ||
             topCard.getType() == Card.Type.WILD_DRAW_FOUR)) {

            for (Card card : validCards) {
                if (canStack(card, topCard)) {
                    return card;
                }
            }
        }

        for (Card card : validCards) {
            if (matchesPrevious(card, topCard, activeColor)) {
                return card;
            }
        }

        Card.Color bestColor = getMostCommonColor(hand);

        if (bestColor != null) {
            List<Card> bestColorCards = new ArrayList<>();

            for (Card card : validCards) {
                if (card.getColor() == bestColor &&
                    card.getType() != Card.Type.WILD &&
                    card.getType() != Card.Type.WILD_DRAW_FOUR &&
                    card.getType() != Card.Type.PARTY) {
                    bestColorCards.add(card);
                }
            }

            if (!bestColorCards.isEmpty()) {
                return bestColorCards.get(random.nextInt(bestColorCards.size()));
            }
        }

        return validCards.get(random.nextInt(validCards.size()));
    }

    public Card.Color chooseWildColor(Hand hand) {
        Card.Color best = getMostCommonColor(hand);
        return best != null ? best : Card.Color.RED;
    }

    private boolean canStack(Card candidate, Card topCard) {
        return (topCard.getType() == Card.Type.DRAW_TWO &&
                candidate.getType() == Card.Type.DRAW_TWO)
            || (topCard.getType() == Card.Type.WILD_DRAW_FOUR &&
                candidate.getType() == Card.Type.WILD_DRAW_FOUR);
    }

    private boolean matchesPrevious(Card candidate, Card topCard, Card.Color activeColor) {
        if (candidate.getColor() == activeColor) {
            return true;
        }

        if (candidate.getType() == Card.Type.NUMBER &&
            topCard.getType() == Card.Type.NUMBER &&
            candidate.getNumber() == topCard.getNumber()) {
            return true;
        }

        return candidate.getType() == topCard.getType();
    }

    private Card.Color getMostCommonColor(Hand hand) {
        Map<Card.Color, Integer> colorCounts = new EnumMap<>(Card.Color.class);

        colorCounts.put(Card.Color.RED, 0);
        colorCounts.put(Card.Color.BLUE, 0);
        colorCounts.put(Card.Color.GREEN, 0);
        colorCounts.put(Card.Color.YELLOW, 0);

        for (Card card : hand.getCards()) {
            Card.Color color = card.getColor();

            if (color != Card.Color.WILD) {
                colorCounts.put(color, colorCounts.get(color) + 1);
            }
        }

        Card.Color bestColor = null;
        int maxCount = -1;

        for (Map.Entry<Card.Color, Integer> entry : colorCounts.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                bestColor = entry.getKey();
            }
        }

        return maxCount > 0 ? bestColor : null;
    }
}
