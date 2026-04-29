package src;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Lobby {

    private final String roomCode;
    private final List<Player> players = new ArrayList<>();
    private final int MAX_PLAYERS = 4;

    private final LobbyRules rules = new LobbyRules();
    private GameState gameState;

    public Lobby(String roomCode, Player host) {
        this.roomCode = roomCode;
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

    public synchronized void removePlayer(Player p) {
        players.remove(p);
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
