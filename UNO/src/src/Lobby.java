/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package src;

import java.util.*;

public class Lobby {

    private String roomCode;
    private List<Player> players = new ArrayList<>();
    private final int MAX_PLAYERS = 4;

    public Lobby(String roomCode, Player host) {
        this.roomCode = roomCode;
        players.add(host);
    }

    public synchronized boolean addPlayer(Player p) {
        if (players.size() >= MAX_PLAYERS) return false;
        players.add(p);
        return true;
    }

    public synchronized void removePlayer(Player p) {
        players.remove(p);
    }

    public synchronized List<Player> getPlayers() {
        return new ArrayList<>(players);
    }

    public String getRoomCode() {
        return roomCode;
    }

    public boolean isFull() {
        return players.size() >= MAX_PLAYERS;
    }
}