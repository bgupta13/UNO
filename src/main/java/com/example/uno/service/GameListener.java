package com.example.uno.service;

import com.example.uno.model.GameState;


public interface GameListener {
    void onGameUpdated(GameState game);
    void onGameEnded(GameState game);
    
} 