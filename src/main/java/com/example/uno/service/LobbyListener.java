package com.example.uno.service;

import com.example.uno.model.Lobby;

public interface LobbyListener {

    void onLobbyUpdated(Lobby lobby);

    void onGameStarted(String code);
}