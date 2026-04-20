package com.example.uno.view;

import com.example.uno.model.*;
import com.example.uno.service.GameService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;

@Route("game/:code")
public class GameView extends VerticalLayout implements BeforeEnterObserver {

    private final GameService gameService;
    private GameState currentGame;

    public GameView(GameService gameService) {
        this.gameService = gameService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Player player = UI.getCurrent().getSession().getAttribute(Player.class);

        String code = event.getRouteParameters()
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
            //TODO: draw card logic
            Card drawn = currentGame.getDeck().drawCard();
            hand.addCard(drawn);
            formatPage();
        });


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
        handLayout.setSpacing(true);
        handLayout.setJustifyContentMode(JustifyContentMode.CENTER);
        handLayout.setAlignItems(Alignment.END);

        for (Card card : hand.getCards()) {

            Image cardImage = new Image(
                    "frontend/images/cards/" + card.toString() + ".PNG",
                    card.toString()
            );

            cardImage.setWidth("80px");
            cardImage.setHeight("120px");

            Button cardButton = new Button(cardImage, e -> {
                // play card logic here
                if (currentGame.compareToDiscard(card) && player.equals(currentGame.getCurrentPlayer())) {
                    currentGame.getDeck().addToDiscard(card);
                    hand.getCards().remove(card);
                    currentGame.nextTurn(0);
                }
                formatPage();
            });

            handLayout.add(cardButton);
        }

        add(handLayout);
    }
}