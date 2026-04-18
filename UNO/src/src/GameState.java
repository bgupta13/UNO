package src;

import java.util.*;

public class GameState {

    private List<Player> players;
    private Map<Player, Hand> hands = new HashMap<>();
    private Deck deck;
    private Player unoPendingPlayer = null;
    private boolean unoCalled = false;
    private int currentPlayerIndex = 0;
    private int direction = 1; // 1 = clockwise, -1 = reverse

    private Card topCard;

    // Rule toggles (not implemented)
    private boolean stackingEnabled = true;
    private boolean drawUntilValid = true;

    public GameState(List<Player> players, Deck deck) {
        this.players = players;
        this.deck = deck;

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

    public Player getCurrentPlayer() {
        return players.get(currentPlayerIndex);
    }
    
    public Player getUnoPendingPlayer() {
        return unoPendingPlayer;
    }

    public boolean isUnoCalled() {
        return unoCalled;
    }

    public void nextTurn(int skipCount) {
        currentPlayerIndex =
                (currentPlayerIndex + direction * (1 + skipCount) + players.size())
                        % players.size();
    }

    // --- PLAY CARD ---
    public boolean playCard(Player player, Card card) {
    	
    	resolveUnoPenalty();
        if (!player.equals(getCurrentPlayer())) return false;
        if (!isValidMove(card, topCard)) return false;

        Hand hand = hands.get(player);
        hand.removeCard(card);

        deck.addToDiscard(card);
        topCard = card;

        int skip = applyCardEffect(card);

        if (hand.getCards().size() == 1) {
            unoPendingPlayer = player;
            unoCalled = false;
        } else if (hand.getCards().isEmpty()) {
            unoPendingPlayer = null;
            unoCalled = false;
            return true;
        }

        nextTurn(skip);
        return true;
    }
    
    public boolean callUno(Player caller) {
        if (unoPendingPlayer == null) {
            return false;
        }

        // Only first call counts
        if (unoCalled) {
            return false;
        }

        unoCalled = true;

        // If someone other than the pending player called UNO first,
        // the pending player gets penalized.
        if (!caller.equals(unoPendingPlayer)) {
            Hand penalizedHand = hands.get(unoPendingPlayer);
            penalizedHand.addCard(deck.drawCard());
            penalizedHand.addCard(deck.drawCard());
        }

        return true;
    }
    
    private void resolveUnoPenalty() {
        if (unoPendingPlayer != null && !unoCalled) {
            Hand penalizedHand = hands.get(unoPendingPlayer);
            penalizedHand.addCard(deck.drawCard());
            penalizedHand.addCard(deck.drawCard());
        }

        unoPendingPlayer = null;
        unoCalled = false;
    }
    
    public void performAITurn() {
        Player current = getCurrentPlayer();

        if (!(current instanceof AIPlayer)) {
            return;
        }

        AIPlayer ai = (AIPlayer) current;
        Hand hand = hands.get(ai);

        Card chosen = ai.chooseCardToPlay(hand, topCard, stackingEnabled);

        if (chosen == null) {
            draw(ai);
        } else {
            playCard(ai, chosen);
        }
    }

    // --- DRAW LOGIC ---
    public void draw(Player player) {
    	
    	resolveUnoPenalty();

        if (player != getCurrentPlayer()) return;

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
}
