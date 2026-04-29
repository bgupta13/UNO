package src;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler extends Thread {

    private final Socket socket;
    private final LobbyManager lobbyManager;

    private BufferedReader in;
    private PrintWriter out;

    private Player player;
    private Lobby currentLobby;

    public ClientHandler(Socket socket, LobbyManager lobbyManager) {
        this.socket = socket;
        this.lobbyManager = lobbyManager;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));

            out = new PrintWriter(
                    socket.getOutputStream(), true);

            out.println("Enter username:");
            String name = in.readLine();

            if (name == null || name.trim().isEmpty()) {
                return;
            }

            player = new Player(name);

            printHelp();

            String command;

            while ((command = in.readLine()) != null) {
                command = command.trim();

                if (command.equalsIgnoreCase("QUIT")) {
                    out.println("Goodbye.");
                    break;
                }

                handleCommand(command);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            cleanup();
        }
    }

    private void printHelp() {
        out.println("Commands:");
        out.println("HOST");
        out.println("JOIN <code>");
        out.println("ADD_AI");
        out.println("RULE STACKING ON|OFF");
        out.println("RULE DRAW_UNTIL_VALID ON|OFF");
        out.println("PARTY KARL_MARX ON|OFF");
        out.println("PARTY SWAPPER ON|OFF");
        out.println("PARTY ROTATER ON|OFF");
        out.println("START");
        out.println("STATUS");
        out.println("QUIT");
    }

    private void handleCommand(String command) {
        if (command.equalsIgnoreCase("HOST")) {
            currentLobby = lobbyManager.createLobby(player);
            out.println("Lobby created.");
            out.println("Room Code: " + currentLobby.getRoomCode());
            return;
        }

        if (command.toUpperCase().startsWith("JOIN ")) {
            String[] parts = command.split("\\s+");

            if (parts.length < 2) {
                out.println("Usage: JOIN <code>");
                return;
            }

            Lobby lobby = lobbyManager.getLobby(parts[1]);

            if (lobby == null) {
                out.println("Lobby not found.");
                return;
            }

            if (lobby.isFull()) {
                out.println("Lobby full.");
                return;
            }

            if (lobby.hasStarted()) {
                out.println("Game already started.");
                return;
            }

            if (!lobby.addPlayer(player)) {
                out.println("Could not join lobby.");
                return;
            }

            currentLobby = lobby;
            out.println("Joined lobby " + lobby.getRoomCode());
            return;
        }

        if (command.equalsIgnoreCase("ADD_AI")) {
            if (!requireLobby()) return;

            boolean added = currentLobby.addAIPlayer();
            out.println(added ? "AI player added." : "Could not add AI player.");
            return;
        }

        if (command.toUpperCase().startsWith("RULE ")) {
            handleRuleCommand(command);
            return;
        }

        if (command.toUpperCase().startsWith("PARTY ")) {
            handlePartyCommand(command);
            return;
        }

        if (command.equalsIgnoreCase("START")) {
            if (!requireLobby()) return;

            GameState game = lobbyManager.startGame(currentLobby.getRoomCode());

            if (game == null) {
                out.println("Unable to start game.");
                return;
            }

            out.println("Game started.");
            out.println("Top Card: " + game.getTopCard());
            out.println("Active Color: " + game.getActiveColor());
            out.println("Current Turn: " + game.getCurrentPlayer().getName());
            out.println("Your Hand: " + game.getHand(player).getCards());
            return;
        }

        if (command.equalsIgnoreCase("STATUS")) {
            if (!requireLobby()) return;

            out.println("Lobby: " + currentLobby);
            out.println("Rules: " + currentLobby.getRules());
            out.println("Players: " + currentLobby.getPlayers());

            GameState game = currentLobby.getGameState();

            if (game != null) {
                out.println("Top Card: " + game.getTopCard());
                out.println("Active Color: " + game.getActiveColor());
                out.println("Current Turn: " + game.getCurrentPlayer().getName());
                out.println("Winner: " + game.getWinner());
                Hand hand = game.getHand(player);
                out.println("Your Hand: " + (hand == null ? "N/A" : hand.getCards()));
            }

            return;
        }

        out.println("Unknown command.");
    }

    private void handleRuleCommand(String command) {
        if (!requireLobby()) return;

        String[] parts = command.split("\\s+");

        if (parts.length != 3) {
            out.println("Usage: RULE STACKING ON|OFF or RULE DRAW_UNTIL_VALID ON|OFF");
            return;
        }

        boolean enabled = parseOnOff(parts[2]);

        if (parts[1].equalsIgnoreCase("STACKING")) {
            currentLobby.getRules().setStackingEnabled(enabled);
            out.println("Stacking set to " + enabled);
        } else if (parts[1].equalsIgnoreCase("DRAW_UNTIL_VALID")) {
            currentLobby.getRules().setDrawUntilValidEnabled(enabled);
            out.println("Draw until valid set to " + enabled);
        } else {
            out.println("Unknown rule.");
        }
    }

    private void handlePartyCommand(String command) {
        if (!requireLobby()) return;

        String[] parts = command.split("\\s+");

        if (parts.length != 3) {
            out.println("Usage: PARTY KARL_MARX|SWAPPER|ROTATER ON|OFF");
            return;
        }

        try {
            Card.PartyType partyType = Card.PartyType.valueOf(parts[1].toUpperCase());
            boolean enabled = parseOnOff(parts[2]);

            currentLobby.getRules().setPartyCardEnabled(partyType, enabled);
            out.println(partyType + " set to " + enabled);

        } catch (IllegalArgumentException e) {
            out.println("Unknown party card. Options: KARL_MARX, SWAPPER, ROTATER");
        }
    }

    private boolean requireLobby() {
        if (currentLobby == null) {
            out.println("You are not in a lobby.");
            return false;
        }

        return true;
    }

    private boolean parseOnOff(String value) {
        return value.equalsIgnoreCase("ON") ||
               value.equalsIgnoreCase("TRUE") ||
               value.equalsIgnoreCase("YES");
    }

    private void cleanup() {
        try {
            if (currentLobby != null && player != null && !currentLobby.hasStarted()) {
                currentLobby.removePlayer(player);
            }

            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }

        } catch (IOException ignored) {
        }
    }
}
