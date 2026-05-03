package com.example.uno.view;

import com.example.uno.model.*;
import com.example.uno.model.Card.Color;
import com.example.uno.model.Card.Type;
import com.example.uno.service.GameService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;

@Route("game/:code")
public class GameView extends VerticalLayout implements BeforeEnterObserver {

    private final GameService gameService;
    private GameState currentGame;
    private Button callUnoButton;
    private String code;

    

    public GameView(GameService gameService) {
        this.gameService = gameService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        

        callUnoButton = new Button("UNO!", event -> {
            Player player = UI.getCurrent().getSession().getAttribute(Player.class);
            currentGame.callUno(player);
            timeInterval();
            formatPage(); 
        });

        
    }

    public void timeInterval() {
        UI.getCurrent().setPollInterval(2000);
        UI.getCurrent().addPollListener(e -> {
            if (currentGame == null || code == null) {
                return;
            }
            if (currentGame.isGameOver()) {
                UI.getCurrent().navigate("winner/" + code);
            }
            else {
                formatPage();
            }
        });
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Player player = UI.getCurrent().getSession().getAttribute(Player.class);

        code = event.getRouteParameters()
                .get("code")
                .orElse("");

        currentGame = gameService.getGame(code);

        if (currentGame == null) {
            add(new Span("Game not found."));
            return;
        }

        formatPage();
    }

    public void formatPage() {
        removeAll();

        Player player = UI.getCurrent().getSession().getAttribute(Player.class);

        if (player == null) {
            add("No player found.");
            return;
        }

        Hand hand = currentGame.getHands().get(player);

        add(new Span("Current Turn: " + currentGame.getCurrentPlayer().getName()));

        if (hand == null) {
            add("No hand found.");
            return;
        }

        //Format draw & discard
        HorizontalLayout centerArea = new HorizontalLayout();
        centerArea.setWidthFull();
        centerArea.setJustifyContentMode(JustifyContentMode.CENTER);
        centerArea.setAlignItems(Alignment.CENTER);
        centerArea.setSpacing(true);

        // DRAW DECK
        Image drawDeckImage = new Image("frontend/images/cards/BACK.PNG", "Draw Deck");
        drawDeckImage.setWidth("100px");
        drawDeckImage.setHeight("150px");

        Button drawDeckButton = new Button(drawDeckImage, e -> {
           currentGame.draw(player);
           formatPage();
            
        });

        boolean shouldShowUno = currentGame.getUnoPendingPlayer() != null && !currentGame.isUnoCalled();

        displayUnoButton(player, shouldShowUno);


        Card topCard = currentGame.getDeck().getTopDiscard();

        if (topCard == null) {
            currentGame.getDeck().addToDiscard(currentGame.getTopCard());
        }
        else {
            Image discardImage = new Image("frontend/images/cards/" + topCard.toString() + ".PNG", topCard.toString());
            discardImage.setWidth("100px");
            discardImage.setHeight("150px");

            centerArea.add(drawDeckButton, discardImage);

            add(centerArea);
        }
        

        // Format hand
        VerticalLayout spacer = new VerticalLayout();
        spacer.setSizeFull();
        add(spacer);
        expand(spacer);

        HorizontalLayout handLayout = new HorizontalLayout();
        handLayout.setWidthFull();
        
        
        for (Card card : hand.getCards()) {

            Image cardImage = new Image(
                    "frontend/images/cards/" + card.toString() + ".PNG",
                    card.toString()
            );

            cardImage.setWidth("80px");
            cardImage.setHeight("120px");

            Button cardButton = new Button(cardImage, e -> {
                // play card logic here
                
                if (currentGame.isWild(card)) {
                    chooseColor(player, card);
                }
                else if (currentGame.needsTarget(card)) {
                    choosePlayer(player, card);
                }
                else {
                    currentGame.playCard(player, card);
                }
                formatPage();
            });
            

            handLayout.add(cardButton);
        }

        
        add(handLayout);
    }

    private void chooseColor(Player player, Card card) {
        Dialog colorDialog = new Dialog();
        
        Button blue = new Button("Blue", event -> {
            currentGame.playCard(player, card, Color.BLUE);
            colorDialog.close();
        });

        Button green = new Button("Green", event -> {
            currentGame.playCard(player, card, Color.GREEN);
            colorDialog.close();
        });

        Button red = new Button("Red", event -> {
            currentGame.playCard(player, card, Color.RED);
            colorDialog.close();
        });

        Button yellow = new Button("Yellow", event -> {
            currentGame.playCard(player, card, Color.YELLOW);
            colorDialog.close();
        });

        colorDialog.add(blue, green, red, yellow);
        colorDialog.open();
    }

    private void choosePlayer(Player player, Card card) {
        Dialog playerDialog = new Dialog();
        for (Player targetPlayer : currentGame.getPlayers()) {
            if (!player.equals(targetPlayer)) {
                System.out.println(targetPlayer.getName());
                Button playerButton = new Button(targetPlayer.getName(), event -> {
                    System.out.println(currentGame.playCard(player, card, targetPlayer));
                    
                    playerDialog.close();
                });
                playerDialog.add(playerButton);
            }
        }
        playerDialog.open();
    }

    private void displayUnoButton(Player player, boolean pendingUno) {
        remove(callUnoButton); // always clear first

        if (pendingUno) {
            add(callUnoButton);
        }
    }

}