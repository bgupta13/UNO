package com.example.uno.model;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public class LobbyRules {

    private boolean stackingEnabled = true;
    private boolean drawUntilValidEnabled = true;
    private final EnumSet<Card.PartyType> enabledPartyCards = EnumSet.noneOf(Card.PartyType.class);

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
        if (partyType != null) {
            enabledPartyCards.remove(partyType);
        }
    }

    public boolean isPartyCardEnabled(Card.PartyType partyType) {
        return partyType != null && enabledPartyCards.contains(partyType);
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
