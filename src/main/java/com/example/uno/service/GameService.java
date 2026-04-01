package com.example.uno.service;

import com.example.uno.model.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class GameService {

    private Map<String, GameState> games = new HashMap<>();

    public GameState startGame(Lobby lobby) {
        Deck deck = new Deck(null);
        GameState game = new GameState(lobby.getPlayers(), deck);

        games.put(lobby.getRoomCode(), game);
        return game;
    }

    public GameState getGame(String code) {
        return games.get(code);
    }
}