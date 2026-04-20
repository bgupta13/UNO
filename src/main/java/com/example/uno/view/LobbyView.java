package com.example.uno.view;

import com.example.uno.model.*;
import com.example.uno.service.GameService;
import com.example.uno.service.LobbyService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;

@Route("lobby/:code")
public class LobbyView extends VerticalLayout implements BeforeEnterObserver {

    private LobbyService lobbyService;
    private GameService gameService;

    private Lobby currentLobby;
    private String code = "";

    // Elements
    private Div playersDiv = new Div();
    private H2 roomCodeText = new H2();
    private Button refreshButton = new Button("Refresh");
    private Button startButton = new Button("Start Game");

    public LobbyView(LobbyService lobbyService, GameService gameService) {
        this.lobbyService = lobbyService;
        this.gameService = gameService;

        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        timeInterval();
    }

    // Time interval for checking if game starts
    public void timeInterval() {
        UI.getCurrent().setPollInterval(2000);
        UI.getCurrent().addPollListener(e -> {
            if (currentLobby != null &&
                gameService.getGame(currentLobby.getRoomCode()) != null) {

                UI.getCurrent().navigate("game/" + currentLobby.getRoomCode());
            }
        });
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {

        code = event.getRouteParameters().get("code").orElse("");
        currentLobby = lobbyService.getLobby(code);

        if (currentLobby == null) {
            add("Lobby not found");
            return;
        }

        Player player = UI.getCurrent().getSession().getAttribute(Player.class);

        if (player == null) {
            add("Invalid name");
            return;
        }

        if (!currentLobby.getPlayers().contains(player)) {
            currentLobby.addPlayer(player);
        }

        formatPage(player);
        refreshPlayers();
    }

    public void formatPage(Player player) {
        removeAll();

        roomCodeText.setText("Lobby Code: " + code);

        H3 playersLabel = new H3("Players");

        VerticalLayout playerSection = new VerticalLayout(playersLabel, playersDiv);

        playerSection.setPadding(false);
        playerSection.setSpacing(false);
        playerSection.setAlignItems(Alignment.CENTER);

        refreshButton.addClickListener(e -> refreshPlayers());

        // Horizontal buttons at bottom
        HorizontalLayout buttonRow = new HorizontalLayout();
        buttonRow.setSpacing(true);
        buttonRow.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        buttonRow.add(refreshButton);

        if (player.equals(currentLobby.getHost())) {
            startButton.addClickListener(e -> {
                gameService.startGame(currentLobby);
                getUI().ifPresent(ui ->
                        ui.navigate("game/" + currentLobby.getRoomCode()));
            });

            buttonRow.add(startButton);
        }

        add(roomCodeText, playerSection, buttonRow);
        expand(playerSection); // pushes buttons to bottom
    }

    private void refreshPlayers() {
        playersDiv.removeAll();

        currentLobby.getPlayers().forEach(player -> {
            Div playerCard = new Div();
            playerCard.setText(player.getName());

            playerCard.getStyle().set("padding", "10px");
            playersDiv.add(playerCard);
        });
    }
}