/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.uno.model;

import java.util.*;

public class Lobby {

    private String roomCode;
    private List<Player> players = new ArrayList<>();
    private final int MAX_PLAYERS = 4;

    private Player host;

    public Lobby(String roomCode, Player host) {
        this.roomCode = roomCode;
        this.host = host;
        players.add(host);
    }

    public Player getHost() {
        return host;
    }

    public synchronized boolean addPlayer(Player p) {
        if (players.size() >= MAX_PLAYERS) {
            return false;
        }
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
        if (players.size() >= MAX_PLAYERS) {
            return true;
        }
        return false;
    }
}