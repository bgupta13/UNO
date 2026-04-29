package uno;

import java.util.ArrayList;
import java.util.List;

public class Lobby {
    private String roomCode;
    private List<Player> players = new ArrayList<>();
    private final int MAX_PLAYERS = 4;
    private Player host;
    private LobbyRules rules = new LobbyRules();
    private GameState gameState;

    public Lobby(String roomCode, Player host) {
        this.roomCode = roomCode;
        this.host = host;
        players.add(host);
    }

    public synchronized boolean addPlayer(Player player) {
        if (players.size() >= MAX_PLAYERS) {
            return false;
        }
        if (players.contains(player)) {
            return false;
        }
        players.add(player);
        return true;
    }

    public synchronized boolean addAIPlayer() {
        if (players.size() >= MAX_PLAYERS) {
            return false;
        }
        int aiNumber = 1;
        Player ai;
        do {
            ai = new AIPlayer("AI " + aiNumber);
            aiNumber++;
        } while (players.contains(ai));
        players.add(ai);
        return true;
    }

    public synchronized void removePlayer(Player player) {
        players.remove(player);
    }

    public synchronized List<Player> getPlayers() {
        return new ArrayList<>(players);
    }

    public String getRoomCode() {
        return roomCode;
    }

    public Player getHost() {
        return host;
    }

    public boolean isHost(Player player) {
        return host != null && host.equals(player);
    }

    public synchronized boolean isFull() {
        return players.size() >= MAX_PLAYERS;
    }

    public synchronized int getPlayerCount() {
        return players.size();
    }

    public LobbyRules getRules() {
        return rules;
    }

    public void setRules(LobbyRules rules) {
        this.rules = rules;
    }

    public GameState getGameState() {
        return gameState;
    }

    public void setGameState(GameState gameState) {
        this.gameState = gameState;
    }
}
