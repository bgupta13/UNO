package com.example.uno.service;

import com.example.uno.model.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class GameService {

    private final LobbyService lobbyService;
    private Map<String, GameState> games = new HashMap<>();


    public GameService(LobbyService lobbyService) {
        this.lobbyService = lobbyService;
    }

    public GameState startGame(Lobby lobby) {
        Deck deck = new Deck(lobby.getRules().getEnabledPartyCards());
        GameState game = new GameState(lobby.getPlayers(), deck, lobby.getRules());

        games.put(lobby.getRoomCode(), game);
        
        lobbyService.notifyGameStarted(lobby.getRoomCode());
        
        return game;
    }

    public GameState getGame(String code) {
        return games.get(code);
    }

    public void endSession(String code) {
        games.remove(code);
        lobbyService.endLobby(code);
    }
}