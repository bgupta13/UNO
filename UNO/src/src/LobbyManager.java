package src;

import java.util.Collection;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class LobbyManager {

    private final Map<String, Lobby> lobbies = new ConcurrentHashMap<>();
    private final Random random = new Random();

    public Lobby createLobby(Player host) {
        String code;

        do {
            code = generateCode();
        } while (lobbies.containsKey(code));

        Lobby lobby = new Lobby(code, host);
        lobbies.put(code, lobby);

        return lobby;
    }

    public Lobby getLobby(String code) {
        if (code == null) {
            return null;
        }

        return lobbies.get(code.toUpperCase());
    }

    public Collection<Lobby> getPublicLobbies() {
        return lobbies.values();
    }

    public GameState startGame(String roomCode) {
        Lobby lobby = getLobby(roomCode);

        if (lobby == null) {
            return null;
        }

        synchronized (lobby) {
            if (lobby.getGameState() != null) {
                return lobby.getGameState();
            }

            Deck deck = new Deck(lobby.getRules().getEnabledPartyCards());

            GameState game = new GameState(
                    lobby.getPlayers(),
                    deck,
                    lobby.getRules()
            );

            lobby.setGameState(game);
            game.runAITurnsIfNeeded();

            return game;
        }
    }

    public GameState getGame(String roomCode) {
        Lobby lobby = getLobby(roomCode);
        return lobby == null ? null : lobby.getGameState();
    }

    public boolean addAIPlayer(String roomCode) {
        Lobby lobby = getLobby(roomCode);
        return lobby != null && lobby.addAIPlayer();
    }

    private String generateCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        StringBuilder code = new StringBuilder();

        for (int i = 0; i < 5; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }

        return code.toString();
    }
}
