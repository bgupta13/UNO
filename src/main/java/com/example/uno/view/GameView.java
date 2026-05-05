package com.example.uno.view;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.example.uno.model.*;
import com.example.uno.model.Card.Color;
import com.example.uno.service.GameListener;
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
import com.vaadin.flow.router.*;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.page.Push;

@Route("game/:code")
public class GameView extends VerticalLayout implements BeforeEnterObserver {

    private final GameService gameService;

    private GameState currentGame;
    private String code;

    private Registration listenerRegistration;

    private final Button callUnoButton = new Button("UNO!");

    public GameView(GameService gameService) {
        this.gameService = gameService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        callUnoButton.addClickListener(event -> {
            Player player = getCurrentPlayer();
            if (currentGame != null) {
                currentGame.callUno(player);
            }
        });
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        removeAll();

        code = event.getRouteParameters().get("code").orElse("");
        currentGame = gameService.getGame(code);

        if (currentGame == null) {
            add(new Span("Game not found."));
            return;
        }

        registerGameListener();
        formatPage();
    }

    private void registerGameListener() {
        UI ui = UI.getCurrent();

        GameListener listener = new GameListener() {
            @Override
            public void onGameUpdated(GameState game) {
                ui.access(() -> {
                    if (game.isGameOver()) {
                        ui.navigate("winner/" + code);
                    } else {
                        formatPage();
                    }
                });
            }

            @Override
            public void onGameEnded(GameState game) {
                ui.access(() -> ui.navigate("winner/" + code));
            }
        };

        currentGame.addListener(listener);
        listenerRegistration = () -> currentGame.removeListener(listener);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }
    }

    private Player getCurrentPlayer() {
        return UI.getCurrent().getSession().getAttribute(Player.class);
    }

    public void formatPage() {
        removeAll();

        Player player = getCurrentPlayer();

        if (player == null) {
            add("No player found.");
            return;
        }

        if (currentGame.isGameOver()) {
            UI.getCurrent().navigate("winner/" + code);
            return;
        }

  
        VerticalLayout opponentsContainer = new VerticalLayout();
        opponentsContainer.setWidthFull();
        opponentsContainer.setHeight("40%");
        opponentsContainer.setAlignItems(Alignment.CENTER);
        opponentsContainer.setJustifyContentMode(JustifyContentMode.CENTER);
        opponentsContainer.setSpacing(true);

        HorizontalLayout opponentsRow = new HorizontalLayout();
        opponentsRow.setWidthFull();
        opponentsRow.setHeight("100%");
        opponentsRow.setJustifyContentMode(JustifyContentMode.CENTER);
        opponentsRow.setAlignItems(Alignment.CENTER);
        opponentsRow.setSpacing(true);

        List<Player> allPlayers = currentGame.getPlayers();
        Player currentPlayerObj = currentGame.getCurrentPlayer();


        List<Player> opponents = new ArrayList<>();
        for (Player p : allPlayers) {
            if (!p.equals(player)) {
                opponents.add(p);
            }
        }

        Div leftSlot = createOpponentSlot(opponents, 0);
        Div topSlot = createOpponentSlot(opponents, 1);
        Div rightSlot = createOpponentSlot(opponents, 2);
        
        HorizontalLayout topRow = new HorizontalLayout();
        topRow.setWidthFull();
        topRow.setJustifyContentMode(JustifyContentMode.CENTER);
        topRow.setAlignItems(Alignment.CENTER);
        topRow.setFlexGrow(1, leftSlot, topSlot, rightSlot); // Distribute space


        if (opponents.size() > 0) leftSlot.add(createOpponentCard(opponents.get(0)));
        if (opponents.size() > 1) topSlot.add(createOpponentCard(opponents.get(1)));
        if (opponents.size() > 2) rightSlot.add(createOpponentCard(opponents.get(2)));

        // Layout presets
        
        leftSlot.getStyle().set("width", "25%");
        leftSlot.getStyle().set("height", "100%");
        leftSlot.getStyle().set("display", "flex");
        leftSlot.getStyle().set("justify-content", "center");
        leftSlot.getStyle().set("align-items", "center");

        topSlot.getStyle().set("width", "25%");
        topSlot.getStyle().set("height", "100%");
        topSlot.getStyle().set("display", "flex");
        topSlot.getStyle().set("justify-content", "center");
        topSlot.getStyle().set("align-items", "center");

        rightSlot.getStyle().set("width", "25%");
        rightSlot.getStyle().set("height", "100%");
        rightSlot.getStyle().set("display", "flex");
        rightSlot.getStyle().set("justify-content", "center");
        rightSlot.getStyle().set("align-items", "center");

        topRow.add(leftSlot, topSlot, rightSlot);
        opponentsContainer.add(topRow);

        // Draw and discard
        HorizontalLayout centerArea = new HorizontalLayout();
        centerArea.setWidthFull();
        centerArea.setHeight("30%");
        centerArea.setJustifyContentMode(JustifyContentMode.CENTER);
        centerArea.setAlignItems(Alignment.CENTER);
        centerArea.setSpacing(true);
        centerArea.getStyle().set("margin-top", "20px");

        VerticalLayout drawPile = new VerticalLayout();
        drawPile.setAlignItems(FlexComponent.Alignment.CENTER);
        drawPile.setSpacing(false);

        // Images for draw
        Image drawDeckImage = new Image("frontend/images/cards/BACK.PNG", "Draw Deck");
        drawDeckImage.setWidth("120px");
        drawDeckImage.setHeight("180px");
        
        Button drawDeckButton = new Button(drawDeckImage, e -> {
            currentGame.draw(player);
        });
        drawDeckButton.getStyle().set("border", "none");
        drawDeckButton.getStyle().set("background", "transparent");

        // Image for discard
        Card topCard = currentGame.getDeck().getTopDiscard();
        Image discardImage = null;
        if (topCard != null) {
            Card.Color activeColor = currentGame.getActiveColor();
            
            String imageName = topCard.getImageName(activeColor);
            
            discardImage = new Image(
                    "frontend/images/cards/" + imageName,
                    topCard.toString()
            );
            discardImage.setWidth("120px");
            discardImage.setHeight("180px");
        }

        centerArea.add(drawDeckButton);
        if (discardImage != null) {
            centerArea.add(discardImage);
        }


        // Player hand
        VerticalLayout handContainer = new VerticalLayout();
        handContainer.setWidthFull();
        handContainer.setHeight("30%");
        handContainer.setAlignItems(Alignment.CENTER);
        handContainer.setJustifyContentMode(JustifyContentMode.CENTER);
        handContainer.setSpacing(true);

        Span statusSpan = new Span("Current Turn: " + currentPlayerObj.getName());
        statusSpan.getStyle().set("font-weight", "bold").set("margin-bottom", "10px");
        handContainer.add(statusSpan);

        Hand myHand = currentGame.getHands().get(player);
        if (myHand != null) {
            List<Card> sortedCards = new ArrayList<>(myHand.getCards());
            sortedCards.sort(new CardComparator());

            HorizontalLayout handLayout = new HorizontalLayout();
            handLayout.setSpacing(true);
            handLayout.setPadding(true);

            for (Card card : sortedCards) {
                Image cardImage = new Image("frontend/images/cards/" + card + ".PNG", card.toString());
                cardImage.setWidth("90px");
                cardImage.setHeight("135px");

                Button cardButton = new Button(cardImage, e -> handleCardPlay(player, card));
                cardButton.getStyle().set("border", "none");
                cardButton.getStyle().set("background", "transparent");
                
                
                handLayout.add(cardButton);
            }

            handContainer.add(handLayout);
        }

        // UNO button
        boolean showUno = currentGame.getUnoPendingPlayer() != null && !currentGame.isUnoCalled();
        if (showUno) {
            positionCallUnoButton();
            add(callUnoButton);
        }


        add(opponentsContainer);
        add(centerArea);
        add(handContainer);
    }
    private void positionCallUnoButton() {
        // Keep the button inside safe viewport regions:
        // - top band above the draw/discard area
        // - left/right middle bands beside the draw/discard area
        // This avoids the player's hand at the bottom and the center deck/discard pile.
        double[][] safeRegions = {
                // minLeftVW, maxLeftVW, minTopVH, maxTopVH
                {8, 88, 8, 32},
                {8, 25, 42, 62},
                {75, 88, 42, 62}
        };

        double[] region = safeRegions[(int) (Math.random() * safeRegions.length)];
        int left = randomInRange(region[0], region[1]);
        int top = randomInRange(region[2], region[3]);

        callUnoButton.getStyle().set("position", "fixed");
        callUnoButton.getStyle().remove("bottom");
        callUnoButton.getStyle().set("left", left + "vw");
        callUnoButton.getStyle().set("top", top + "vh");
        callUnoButton.getStyle().set("z-index", "100");
        callUnoButton.getStyle().set("background-color", "rgba(255, 0, 0, 0.8)");
        callUnoButton.getStyle().set("font-weight", "bold");
    }

    private int randomInRange(double min, double max) {
        return (int) Math.round(min + Math.random() * (max - min));
    }


    // Player layouts
    private Div createOpponentSlot(List<Player> opponents, int index) {
        Div div = new Div();
        div.getStyle().set("display", "flex");
        div.getStyle().set("flex-direction", "column");
        div.getStyle().set("align-items", "center");
        div.getStyle().set("justify-content", "center");
        return div;
    }

    private Div createOpponentCard(Player p) {
        Div container = new Div();
        container.getStyle().set("text-align", "center");
        container.getStyle().set("background", "#ffffffff");
        container.getStyle().set("padding", "10px");
        container.getStyle().set("border-radius", "8px");
        container.getStyle().set("box-shadow", "0 2px 4px rgba(0, 0, 0, 0.1)");

        H3 name = new H3(p.getName());
        name.getStyle().set("margin", "0 0 5px 0");
        
        Span count = new Span("Cards: " + currentGame.getHand(p).size());
        count.getStyle().set("font-size", "0.9em");
        count.getStyle().set("color", "#666");

        // Visual indicator if it's their turn
        if (p.equals(currentGame.getCurrentPlayer())) {
            container.getStyle().set("border", "2px solid #4CAF50");
            count.setText(count.getText() + " (Your Turn)");
        }

        container.add(name, count);
        return container;
    }

    private void handleCardPlay(Player player, Card card) {
        if (currentGame.requiresChosenColor(card)) {
            chooseColor(player, card);
        }
        else {
            currentGame.playCard(player, card);
        }
    }

    // Dialogs for choosing color & player

    private void chooseColor(Player player, Card card) {
        Dialog colorDialog = new Dialog();
        colorDialog.setWidth("300px");

        HorizontalLayout colorLayout = new HorizontalLayout();
        colorLayout.setSpacing(true);

        for (Color color : Color.values()) {
            if (color != Color.WILD) {
                Button colorButton = new Button(color.name(), e -> {
                    if (currentGame.needsTarget(card)) {
                        choosePlayer(player, card, color);
                        colorDialog.close();
                    }
                    else {
                        currentGame.playCard(player, card, color);
                        colorDialog.close();
                    }
                });
                
                colorButton.getStyle().set("background-color", getColorHex(color));
                colorButton.getStyle().set("color", "white");
                colorLayout.add(colorButton);
            }
        }

        colorDialog.add(colorLayout);
        colorDialog.open();
    }

    private void choosePlayer(Player player, Card card, Color color) {
        Dialog playerDialog = new Dialog();
        playerDialog.setWidth("300px");

        VerticalLayout targetLayout = new VerticalLayout();
        targetLayout.setSpacing(true);

        for (Player target : currentGame.getPlayers()) {
            if (!player.equals(target)) {
                Button targetButton = new Button(target.getName(), e -> {
                    
                    currentGame.playCard(player, card, color, target);
                    playerDialog.close();
                    formatPage();
                });
                
                targetLayout.add(targetButton);
            }
        }
        playerDialog.add(targetLayout);
        playerDialog.open();
    }

    private String getColorHex(Color color) {
        switch (color) {
            case RED: return "#ff0000ff";
            case BLUE: return "#0080ffff";
            case GREEN: return "#18881eff";
            case YELLOW: return "#ffb700ff";
            default: return "#000000ff";
        }
    }


    // Ordering the cards in hand by num/type and color

    private static class CardComparator implements Comparator<Card> {
        @Override
        public int compare(Card c1, Card c2) {
            int colorOrder1 = getColorOrder(c1.getColor());
            int colorOrder2 = getColorOrder(c2.getColor());
            
            if (colorOrder1 != colorOrder2) {
                return Integer.compare(colorOrder1, colorOrder2);
            }

            int typeOrder1 = getTypeOrder(c1);
            int typeOrder2 = getTypeOrder(c2);

            if (typeOrder1 != typeOrder2) {
                return Integer.compare(typeOrder1, typeOrder2);
            }

            if (c1.getType() == Card.Type.NUMBER && c2.getType() == Card.Type.NUMBER) {
                return Integer.compare(c1.getNumber(), c2.getNumber());
            }

            return 0;
        }

        private int getColorOrder(Color color) {
            switch (color) {
                case BLUE: return 1;
                case GREEN: return 2;
                case RED: return 3;
                case YELLOW: return 4;
                case WILD: return 5;
                default: return 6;
            }
        }

        private int getTypeOrder(Card card) {
            Card.Type type = card.getType();
            if (type == Card.Type.NUMBER) return 0;
            if (type == Card.Type.SKIP) return 1;
            if (type == Card.Type.REVERSE) return 2;
            if (type == Card.Type.DRAW_TWO) return 3;
            if (type == Card.Type.WILD) return 4;
            if (type == Card.Type.WILD_DRAW_FOUR) return 5;
            if (type == Card.Type.PARTY) return 6;
            return 7;
        }
    }
}
