package com.example.uno.view;

import com.example.uno.model.*;
import com.example.uno.model.Card.PartyType;
import com.example.uno.service.GameService;
import com.example.uno.service.LobbyService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
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

    private boolean canStack;
    private boolean canSkipChain;

    // Elements
    private Div playersDiv = new Div();
    private H2 roomCodeText = new H2();
    private Button refreshButton = new Button("Refresh");
    private Button startButton = new Button("Start Game");
    private Button addAIButton = new Button("Add AI Player");


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

        // LEFT SIDE (players)
        H3 playersLabel = new H3("Players");
        VerticalLayout playerSection = new VerticalLayout(playersLabel, playersDiv);
        playerSection.setPadding(false);
        playerSection.setSpacing(false);
        playerSection.setAlignItems(Alignment.CENTER);
        playerSection.setWidth("70%");

        // RIGHT SIDE (checkbox settings)
        H3 settingsLabel = new H3("Game Rules");

        Checkbox allowStacking = new Checkbox("Allow Stacking");
        Checkbox drawUntilValid = new Checkbox("Draw Until Valid Card");
        Checkbox partyKarlMarx = new Checkbox("Party Card | Karl Marx");
        Checkbox partySwapper = new Checkbox("Party Card | Swapper");
        Checkbox partyRotator = new Checkbox("Party Card | Rotator");





        // Example default values
        allowStacking.setValue(false);
        drawUntilValid.setValue(false);
        partyKarlMarx.setValue(false);
        partySwapper.setValue(false);
        partyRotator.setValue(false);

        VerticalLayout settingsSection = new VerticalLayout(
                settingsLabel,
                allowStacking,
                drawUntilValid,
                partyKarlMarx,
                partySwapper,
                partyRotator
        );

        settingsSection.setWidth("30%");
        settingsSection.setAlignItems(Alignment.START);

        // MAIN CONTENT (left + right)
        HorizontalLayout mainContent = new HorizontalLayout(playerSection, settingsSection);
        mainContent.setSizeFull();
        mainContent.setAlignItems(Alignment.START);

        // Refresh button
        refreshButton.addClickListener(e -> refreshPlayers());

        // Bottom buttons
        HorizontalLayout buttonRow = new HorizontalLayout();
        buttonRow.setSpacing(true);
        buttonRow.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        buttonRow.add(refreshButton);

        if (player.equals(currentLobby.getHost())) {
            startButton.addClickListener(e -> {

                // Read checkbox values when starting
                currentLobby.setLobbyRules(
                        allowStacking.getValue(),
                        drawUntilValid.getValue(),
                        partyKarlMarx.getValue(),
                        partyRotator.getValue(),
                        partySwapper.getValue()
                );


                gameService.startGame(currentLobby);
                
                

                getUI().ifPresent(ui ->
                        ui.navigate("game/" + currentLobby.getRoomCode()));
            });
            buttonRow.add(startButton);

            addAIButton.addClickListener(e -> {
                currentLobby.addAIPlayer();
            });
            buttonRow.add(addAIButton);
        }

        add(roomCodeText, mainContent, buttonRow);
        expand(mainContent); // pushes buttons to bottom
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