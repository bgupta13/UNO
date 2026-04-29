package src;

import java.net.ServerSocket;
import java.net.Socket;

public class LobbyServer {

    private static final int PORT = 5000;
    private static final LobbyManager lobbyManager = new LobbyManager();

    public static void main(String[] args) throws Exception {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("UNO Lobby Server running on port " + PORT + "...");

        while (true) {
            Socket socket = serverSocket.accept();
            new ClientHandler(socket, lobbyManager).start();
        }
    }
}
