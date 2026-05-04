package com.example.uno.view;

import com.example.uno.model.*;
import com.example.uno.model.Card.Color;
import com.example.uno.service.GameService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
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
        setAlignItems(FlexComponent.Alignment.STRETCH);

        callUnoButton = new Button("UNO!", event -> {
            Player player = UI.getCurrent().getSession().getAttribute(Player.class);
            currentGame.callUno(player);
            timeInterval();
            formatPage();
        });

        styleUnoButton();
    }

    public void timeInterval() {
        UI.getCurrent().setPollInterval(2000);
        UI.getCurrent().addPollListener(e -> {
            if (currentGame == null || code == null) {
                return;
            }

            if (currentGame.isGameOver()) {
                UI.getCurrent().navigate("winner/" + code);
            } else {
                formatPage();
            }
        });
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        code = event.getRouteParameters().get("code").orElse("");
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

        Hand hand = currentGame.getHand(player);

        if (hand == null) {
            add("No hand found.");
            return;
        }

        add(buildHeader());
        add(buildPlayerInfoPanel(player));
        add(buildCenterPileArea(player));

        Div spacer = new Div();
        spacer.setWidthFull();
        spacer.getStyle().set("flex-grow", "1");
        add(spacer);
        expand(spacer);

        add(buildBottomArea(player, hand));
    }

    private HorizontalLayout buildHeader() {
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        String currentTurn = currentGame.getCurrentPlayer().getName();
        String activeColor = currentGame.getActiveColor() != null
                ? currentGame.getActiveColor().toString()
                : "None";

        Span turnText = new Span("Current Turn: " + currentTurn + " | Active Color: " + activeColor);
        turnText.getStyle()
                .set("font-size", "20px")
                .set("font-weight", "bold");

        header.add(turnText);
        return header;
    }

    private HorizontalLayout buildPlayerInfoPanel(Player currentPlayer) {
        HorizontalLayout playerInfo = new HorizontalLayout();
        playerInfo.setWidthFull();
        playerInfo.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        playerInfo.setAlignItems(FlexComponent.Alignment.CENTER);
        playerInfo.setSpacing(true);

        for (Player p : currentGame.getPlayers()) {
            Hand playerHand = currentGame.getHand(p);
            int cardCount = playerHand == null ? 0 : playerHand.size();

            VerticalLayout card = new VerticalLayout();
            card.setPadding(true);
            card.setSpacing(false);
            card.setAlignItems(FlexComponent.Alignment.CENTER);
            card.setWidth("160px");
            card.getStyle()
                    .set("border", p.equals(currentGame.getCurrentPlayer()) ? "3px solid #ffcc00" : "1px solid #cccccc")
                    .set("border-radius", "10px")
                    .set("background", p.equals(currentPlayer) ? "#eef6ff" : "#f8f8f8");

            Span name = new Span(p.getName());
            name.getStyle().set("font-weight", "bold");

            Span count = new Span(cardCount + " card" + (cardCount == 1 ? "" : "s"));

            card.add(name, count);
            playerInfo.add(card);
        }

        return playerInfo;
    }

    private HorizontalLayout buildCenterPileArea(Player player) {
        HorizontalLayout centerArea = new HorizontalLayout();
        centerArea.setWidthFull();
        centerArea.setHeight("260px");
        centerArea.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        centerArea.setAlignItems(FlexComponent.Alignment.CENTER);
        centerArea.setSpacing(true);
        centerArea.getStyle().set("margin-top", "20px");

        VerticalLayout drawPile = new VerticalLayout();
        drawPile.setAlignItems(FlexComponent.Alignment.CENTER);
        drawPile.setSpacing(false);

        Image drawDeckImage = new Image("frontend/images/cards/BACK.PNG", "Draw Deck");
        drawDeckImage.setWidth("100px");
        drawDeckImage.setHeight("150px");

        Button drawDeckButton = new Button(drawDeckImage, e -> {
            currentGame.draw(player);
            formatPage();
        });
        drawDeckButton.setEnabled(player.equals(currentGame.getCurrentPlayer()) && !currentGame.isGameOver());

        drawPile.add(new Span("Draw"), drawDeckButton);

        VerticalLayout discardPile = new VerticalLayout();
        discardPile.setAlignItems(FlexComponent.Alignment.CENTER);
        discardPile.setSpacing(false);

        Card displayCard = currentGame.getTopCard();

        Image discardImage = new Image(
                "frontend/images/cards/" + displayCard.toString() + ".PNG",
                displayCard.toString()
        );
        discardImage.setWidth("100px");
        discardImage.setHeight("150px");

        discardPile.add(new Span("Center Pile"), discardImage);

        centerArea.add(drawPile, discardPile);
        return centerArea;
    }

    private HorizontalLayout buildBottomArea(Player player, Hand hand) {
        HorizontalLayout bottomArea = new HorizontalLayout();
        bottomArea.setWidthFull();
        bottomArea.setAlignItems(FlexComponent.Alignment.END);
        bottomArea.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        bottomArea.setSpacing(true);

        HorizontalLayout handLayout = buildHandLayout(player, hand);

        boolean shouldShowUno = currentGame.getUnoPendingPlayer() != null && !currentGame.isUnoCalled();
        callUnoButton.setEnabled(shouldShowUno);
        callUnoButton.setVisible(shouldShowUno);

        bottomArea.add(handLayout, callUnoButton);
        bottomArea.expand(handLayout);

        return bottomArea;
    }

    private HorizontalLayout buildHandLayout(Player player, Hand hand) {
        HorizontalLayout handLayout = new HorizontalLayout();
        handLayout.setWidthFull();
        handLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
        handLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        handLayout.setSpacing(true);
        handLayout.getStyle().set("overflow-x", "auto");

        for (Card card : hand.getCards()) {
            Image cardImage = new Image(
                    "frontend/images/cards/" + card.toString() + ".PNG",
                    card.toString()
            );

            cardImage.setWidth("80px");
            cardImage.setHeight("120px");

            Button cardButton = new Button(cardImage, e -> {
                if (currentGame.isWild(card)) {
                    chooseColor(player, card);
                } else if (currentGame.needsTarget(card)) {
                    choosePlayer(player, card);
                } else {
                    currentGame.playCard(player, card);
                    formatPage();
                }
            });

            cardButton.setEnabled(player.equals(currentGame.getCurrentPlayer()) && !currentGame.isGameOver());
            handLayout.add(cardButton);
        }

        return handLayout;
    }

    private void chooseColor(Player player, Card card) {
        Dialog colorDialog = new Dialog();

        Button blue = new Button("Blue", event -> {
            currentGame.playCard(player, card, Color.BLUE);
            colorDialog.close();
            formatPage();
        });

        Button green = new Button("Green", event -> {
            currentGame.playCard(player, card, Color.GREEN);
            colorDialog.close();
            formatPage();
        });

        Button red = new Button("Red", event -> {
            currentGame.playCard(player, card, Color.RED);
            colorDialog.close();
            formatPage();
        });

        Button yellow = new Button("Yellow", event -> {
            currentGame.playCard(player, card, Color.YELLOW);
            colorDialog.close();
            formatPage();
        });

        colorDialog.add(new H3("Choose a color"), new HorizontalLayout(blue, green, red, yellow));
        colorDialog.open();
    }

    private void choosePlayer(Player player, Card card) {
        Dialog playerDialog = new Dialog();
        VerticalLayout content = new VerticalLayout();
        content.add(new H3("Choose a player"));

        for (Player targetPlayer : currentGame.getPlayers()) {
            if (!player.equals(targetPlayer)) {
                Button playerButton = new Button(targetPlayer.getName(), event -> {
                    currentGame.playCard(player, card, targetPlayer);
                    playerDialog.close();
                    formatPage();
                });
                content.add(playerButton);
            }
        }

        playerDialog.add(content);
        playerDialog.open();
    }

    private void styleUnoButton() {
        callUnoButton.setText("UNO!");
        callUnoButton.setWidth("130px");
        callUnoButton.setHeight("70px");
        callUnoButton.getStyle()
                .set("font-size", "24px")
                .set("font-weight", "bold")
                .set("background-color", "red")
                .set("color", "white")
                .set("border-radius", "12px")
                .set("margin-right", "20px")
                .set("margin-bottom", "10px");
    }
}
