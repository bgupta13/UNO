/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uno;

import java.net.*;
import java.io.*;

public class LobbyServer {

    private static final int PORT = 5000;
    private static LobbyManager lobbyManager = new LobbyManager();

    public static void main(String[] args) throws Exception {

        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("UNO Lobby Server running...");

        while (true) {
            Socket socket = serverSocket.accept();
            new ClientHandler(socket, lobbyManager).start();
        }
    }
}