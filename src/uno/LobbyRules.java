package uno;

import java.util.HashSet;
import java.util.Set;

public class LobbyRules {
    private boolean stackingEnabled = true;
    private boolean drawUntilValidEnabled = true;
    private Set<Card.PartyType> enabledPartyCards = new HashSet<>();

    public boolean isStackingEnabled() {
        return stackingEnabled;
    }

    public void setStackingEnabled(boolean stackingEnabled) {
        this.stackingEnabled = stackingEnabled;
    }

    public boolean isDrawUntilValidEnabled() {
        return drawUntilValidEnabled;
    }

    public void setDrawUntilValidEnabled(boolean drawUntilValidEnabled) {
        this.drawUntilValidEnabled = drawUntilValidEnabled;
    }

    public Set<Card.PartyType> getEnabledPartyCards() {
        return enabledPartyCards;
    }

    public void addPartyCard(Card.PartyType partyType) {
        enabledPartyCards.add(partyType);
    }

    public void removePartyCard(Card.PartyType partyType) {
        enabledPartyCards.remove(partyType);
    }

    public boolean isPartyCardEnabled(Card.PartyType partyType) {
        return enabledPartyCards.contains(partyType);
    }
}
