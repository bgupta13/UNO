package com.example.uno.view;

import com.example.uno.model.*;
import com.example.uno.service.GameService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;

@Route("game/:code")
public class GameView extends VerticalLayout implements BeforeEnterObserver {

    private GameService gameService;
    private GameState currentGame;

    public GameView(GameService gameService) {
        this.gameService = gameService;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        String code = event.getRouteParameters().get("code").orElse("");
        currentGame = gameService.getGame(code);

        if (currentGame == null) {
            add("Game not found");
            return;
        }

        renderGame();
    }

    private void renderGame() {
        removeAll();

        Player player = UI.getCurrent().getSession().getAttribute(Player.class);
        Hand hand = currentGame.getHands().get(player);

        System.out.println("Hands map: " + currentGame.getHands());

        if (player == null) {
            add("No player found.");
            return;
        }

        //TODO: add card playing functionality
        //add("Top Card: " + currentGame.getTopCard());



        add("Current Turn: " + currentGame.getCurrentPlayer().getName());

        

        if (hand == null) {
            add("No hand found.");
            return;
        }

        for (Card card : hand.getCards()) {
            Button cardButton = new Button(card.toString(), e -> {



                renderGame();
            });

            add(cardButton);
        }
    }
}