package com.example.uno.service;

import com.example.uno.model.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;



@Service
public class LobbyService {

    private Map<String, Lobby> lobbies = new ConcurrentHashMap<>();
    private Random random = new Random();

    private final List<LobbyListener> listeners = new java.util.concurrent.CopyOnWriteArrayList<>();


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

    public Collection<Lobby> getAllLobbies() {
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

    public void endLobby(String code) {
        lobbies.remove(code);
    }

    public void addListener(LobbyListener listener) {
        listeners.add(listener);
    }

    public void removeListener(LobbyListener listener) {
        listeners.remove(listener);
    }

    public void notifyLobbyUpdated(Lobby lobby) {
        for (LobbyListener l : listeners) {
            l.onLobbyUpdated(lobby);
        }
    }

    public void notifyGameStarted(String code) {
        for (LobbyListener l : listeners) {
            l.onGameStarted(code);
        }
    }
}