/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uno;
import java.io.*;
import java.net.*;

public class ClientHandler extends Thread {

    private Socket socket;
    private LobbyManager lobbyManager;

    private BufferedReader in;
    private PrintWriter out;

    private Player player;

    public ClientHandler(Socket socket, LobbyManager lobbyManager) {
        this.socket = socket;
        this.lobbyManager = lobbyManager;
    }

    public void run() {

        try {

            in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));

            out = new PrintWriter(
                    socket.getOutputStream(), true);

            out.println("Enter username:");
            String name = in.readLine();

            player = new Player(name);

            out.println("Commands: HOST | JOIN <code>");

            String command;

            while ((command = in.readLine()) != null) {

                if (command.equalsIgnoreCase("HOST")) {

                    Lobby lobby = lobbyManager.createLobby(player);

                    out.println("Lobby created.");
                    out.println("Room Code: " + lobby.getRoomCode());

                }

                else if (command.startsWith("JOIN")) {

                    String[] parts = command.split(" ");
                    String code = parts[1];

                    Lobby lobby = lobbyManager.getLobby(code);

                    if (lobby == null) {
                        out.println("Lobby not found.");
                        continue;
                    }

                    if (lobby.isFull()) {
                        out.println("Lobby full.");
                        continue;
                    }

                    lobby.addPlayer(player);

                    out.println("Joined lobby " + code);

                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}