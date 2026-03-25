/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package src;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class LobbyManager {

    private Map<String, Lobby> lobbies = new ConcurrentHashMap<>();
    private Random random = new Random();

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
        return lobbies.get(code);
    }

    public Collection<Lobby> getPublicLobbies() {
        return lobbies.values();
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
