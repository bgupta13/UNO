package com.example.uno.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.example.uno.model.Card.PartyType;

public class Lobby {

    private final String roomCode;
    private final List<Player> players = new ArrayList<>();
    private final int MAX_PLAYERS = 4;

    private Player host;

    private final LobbyRules rules = new LobbyRules();
    private GameState gameState;

    public Lobby(String roomCode, Player host) {
        this.roomCode = roomCode;
        this.host = host;
        players.add(host);
    }

    public synchronized boolean addPlayer(Player p) {
        if (p == null || players.size() >= MAX_PLAYERS || gameState != null) {
            return false;
        }

        if (players.contains(p)) {
            return false;
        }

        players.add(p);
        return true;
    }
    
    public Player getHost() {
        return host;
    }

    public synchronized boolean addAIPlayer() {
        if (players.size() >= MAX_PLAYERS || gameState != null) {
            return false;
        }

        int aiNumber = 1;
        AIPlayer ai;

        do {
            ai = new AIPlayer("AI " + aiNumber);
            aiNumber++;
        } while (players.contains(ai));

        players.add(ai);
        return true;
    }

    public synchronized boolean removePlayer(Player p) {
        if (!p.equals(host)) {
            players.remove(p);
            return true;
        }
        return false;
    }

    public synchronized List<Player> getPlayers() {
        return new ArrayList<>(players);
    }

    public synchronized List<Player> getPlayersReadOnly() {
        return Collections.unmodifiableList(new ArrayList<>(players));
    }

    public String getRoomCode() {
        return roomCode;
    }

    public synchronized boolean isFull() {
        return players.size() >= MAX_PLAYERS;
    }

    public synchronized int getPlayerCount() {
        return players.size();
    }

    public int getMaxPlayers() {
        return MAX_PLAYERS;
    }

    public LobbyRules getRules() {
        return rules;
    }

    public void setLobbyRules(
            boolean stacking,
            boolean drawUntilValid,
            boolean karlMarx,
            boolean rotator,
            boolean swapper
    ) {
        rules.setStackingEnabled(stacking);
        System.out.println(stacking);
        rules.setDrawUntilValidEnabled(drawUntilValid);
        System.out.println(drawUntilValid);
        rules.setPartyCardEnabled(PartyType.KARL_MARX, karlMarx);
        System.out.println(karlMarx);
        rules.setPartyCardEnabled(PartyType.ROTATER, rotator);
        System.out.println(rotator);
        rules.setPartyCardEnabled(PartyType.SWAPPER, swapper);
        System.out.println(swapper);
    }

    public synchronized GameState getGameState() {
        return gameState;
    }

    public synchronized void setGameState(GameState gameState) {
        this.gameState = gameState;
    }

    public synchronized boolean hasStarted() {
        return gameState != null;
    }

    @Override
    public String toString() {
        return roomCode + " (" + getPlayerCount() + "/" + MAX_PLAYERS + ")";
    }
}
