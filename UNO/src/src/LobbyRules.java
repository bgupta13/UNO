package src;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public class LobbyRules {

    private boolean stackingEnabled;
    private boolean drawUntilValidEnabled;
    private final EnumSet<Card.PartyType> enabledPartyCards;

    public LobbyRules() {
        this.stackingEnabled = true;
        this.drawUntilValidEnabled = true;
        this.enabledPartyCards = EnumSet.noneOf(Card.PartyType.class);
    }

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
        return Collections.unmodifiableSet(enabledPartyCards);
    }

    public void addPartyCard(Card.PartyType partyType) {
        if (partyType != null) {
            enabledPartyCards.add(partyType);
        }
    }

    public void removePartyCard(Card.PartyType partyType) {
        enabledPartyCards.remove(partyType);
    }

    public boolean isPartyCardEnabled(Card.PartyType partyType) {
        return enabledPartyCards.contains(partyType);
    }

    public void setPartyCardEnabled(Card.PartyType partyType, boolean enabled) {
        if (enabled) {
            addPartyCard(partyType);
        } else {
            removePartyCard(partyType);
        }
    }

    @Override
    public String toString() {
        return "stacking=" + stackingEnabled +
               ", drawUntilValid=" + drawUntilValidEnabled +
               ", partyCards=" + enabledPartyCards;
    }
}
