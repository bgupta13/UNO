package com.example.uno.view;

import com.example.uno.model.*;
import com.example.uno.model.Card.PartyType;
import com.example.uno.service.GameService;
import com.example.uno.service.LobbyListener;
import com.example.uno.service.LobbyService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.orderedlayout.*;
import com.vaadin.flow.router.*;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.shared.Registration;

@Route("lobby/:code")
public class LobbyView extends VerticalLayout implements BeforeEnterObserver {

    private final LobbyService lobbyService;
    private final GameService gameService;

    private Lobby currentLobby;
    private String code;

    private Registration listenerRegistration;

    // UI elements
    private final Div playersDiv = new Div();
    private final H2 roomCodeText = new H2();
    
    // Rule Checkboxes
    private Checkbox allowStacking;
    private Checkbox drawUntilValid;
    private Checkbox partyKarlMarx;
    private Checkbox partySwapper;
    private Checkbox partyRotator;

    public LobbyView(LobbyService lobbyService, GameService gameService) {
        this.lobbyService = lobbyService;
        this.gameService = gameService;

        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        removeAll();

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
            lobbyService.notifyLobbyUpdated(currentLobby);
        }

        registerLobbyListener();

        formatPage(player);
        refreshPlayers();
    }

    private void registerLobbyListener() {
        UI ui = UI.getCurrent();

        LobbyListener listener = new LobbyListener() {
            @Override
            public void onLobbyUpdated(Lobby lobby) {
                if (!lobby.getRoomCode().equals(code)) return;
                currentLobby = lobby;
                ui.access(() -> {
                    refreshPlayers();
                    refreshRules();
                });
            }

            @Override
            public void onGameStarted(String roomCode) {
                if (!roomCode.equals(code)) return;
                ui.access(() -> ui.navigate("game/" + roomCode));
            }
        };

        lobbyService.addListener(listener);
        listenerRegistration = () -> lobbyService.removeListener(listener);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }
    }

    private void formatPage(Player player) {
        removeAll();

        roomCodeText.setText("Lobby Code: " + code);

        // Player section
        H3 playersLabel = new H3("Players");
        VerticalLayout playerSection = new VerticalLayout(playersLabel, playersDiv);
        playerSection.setAlignItems(Alignment.CENTER);
        playerSection.setWidth("70%");

        // Settings Section
        H3 settingsLabel = new H3("Game Rules");
        
        allowStacking = new Checkbox("Allow Stacking");
        drawUntilValid = new Checkbox("Draw Until Valid Card");
        partyKarlMarx = new Checkbox("Party Card | Karl Marx");
        partySwapper = new Checkbox("Party Card | Swapper");
        partyRotator = new Checkbox("Party Card | Rotator");

        refreshRules();

        VerticalLayout settingsSection = new VerticalLayout(
                settingsLabel,
                allowStacking,
                drawUntilValid,
                partyKarlMarx,
                partySwapper,
                partyRotator
        );
        settingsSection.setWidth("30%");

        HorizontalLayout mainContent = new HorizontalLayout(playerSection, settingsSection);
        mainContent.setSizeFull();

        // Buttons
        Button startButton = new Button("Start Game");
        Button addAIButton = new Button("Add AI Player");

        HorizontalLayout buttonRow = new HorizontalLayout();

        boolean isHost = player.equals(currentLobby.getHost());

        if (isHost) {
            allowStacking.setEnabled(true);
            drawUntilValid.setEnabled(true);
            partyKarlMarx.setEnabled(true);
            partySwapper.setEnabled(true);
            partyRotator.setEnabled(true);

            allowStacking.addValueChangeListener(e -> updateRules());
            drawUntilValid.addValueChangeListener(e -> updateRules());
            partyKarlMarx.addValueChangeListener(e -> updateRules());
            partySwapper.addValueChangeListener(e -> updateRules());
            partyRotator.addValueChangeListener(e -> updateRules());

            startButton.addClickListener(e -> {
                updateRules(); 
                gameService.startGame(currentLobby);
            });

            addAIButton.addClickListener(e -> {
                currentLobby.addAIPlayer();
                lobbyService.notifyLobbyUpdated(currentLobby);
            });

            buttonRow.add(startButton, addAIButton);
        } else {
            allowStacking.setEnabled(false);
            drawUntilValid.setEnabled(false);
            partyKarlMarx.setEnabled(false);
            partySwapper.setEnabled(false);
            partyRotator.setEnabled(false);
            Span hostOnlyMsg = new Span("(Only the host can change rules)");
            hostOnlyMsg.getStyle().set("font-size", "smaller").set("color", "#888");
            settingsSection.add(hostOnlyMsg);
        }

        add(roomCodeText, mainContent, buttonRow);
        expand(mainContent);
    }

    private void updateRules() {
        if (currentLobby == null) return;
        currentLobby.setLobbyRules(
                allowStacking.getValue(),
                drawUntilValid.getValue(),
                partyKarlMarx.getValue(),
                partyRotator.getValue(),
                partySwapper.getValue()
        );
        lobbyService.notifyLobbyUpdated(currentLobby);
    }

    private void refreshRules() {
        if (currentLobby == null || currentLobby.getRules() == null) return;
        LobbyRules rules = currentLobby.getRules();
        if (allowStacking.getValue() != rules.isStackingEnabled()) {
            allowStacking.setValue(rules.isStackingEnabled());
        }
        if (drawUntilValid.getValue() != rules.isDrawUntilValidEnabled()) {
            drawUntilValid.setValue(rules.isDrawUntilValidEnabled());
        }
        if (partyKarlMarx.getValue() != rules.isPartyCardEnabled(PartyType.KARL_MARX)) {
            partyKarlMarx.setValue(rules.isPartyCardEnabled(PartyType.KARL_MARX));
        }
        if (partySwapper.getValue() != rules.isPartyCardEnabled(PartyType.SWAPPER)) {
            partySwapper.setValue(rules.isPartyCardEnabled(PartyType.SWAPPER));
        }
        if (partyRotator.getValue() != rules.isPartyCardEnabled(PartyType.ROTATER)) {
            partyRotator.setValue(rules.isPartyCardEnabled(PartyType.ROTATER));
        }
    }

    private void refreshPlayers() {
        playersDiv.removeAll();

        if (currentLobby == null) return;

        Player currentPlayer = UI.getCurrent().getSession().getAttribute(Player.class);

        if (currentPlayer != null && !currentLobby.getPlayers().contains(currentPlayer)) {
            UI.getCurrent().navigate("");
            return;
        }

        boolean isHost = currentPlayer != null && currentPlayer.equals(currentLobby.getHost());

        for (Player p : currentLobby.getPlayers()) {
            HorizontalLayout playerRow = new HorizontalLayout();
            playerRow.setWidthFull();
            playerRow.setAlignItems(Alignment.CENTER);
            playerRow.setJustifyContentMode(JustifyContentMode.START);

            Div nameDiv = new Div();
            nameDiv.setText(p.getName() + (p.equals(currentLobby.getHost()) ? " (Host)" : ""));
            nameDiv.getStyle().set("padding", "10px");
            nameDiv.getStyle().set("flex-grow", "1");


            if (isHost && !p.equals(currentLobby.getHost())) {
                Button kickButton = new Button("Kick");
                kickButton.getStyle().set("background-color", "#ff0000ff");
                kickButton.getStyle().set("color", "white");
                kickButton.getStyle().set("border-radius", "4px");
                kickButton.getStyle().set("padding", "5px 10px");
                
                kickButton.addClickListener(e -> {
                    boolean kicked = currentLobby.removePlayer(p);
                    
                    if (kicked) {
                        lobbyService.notifyLobbyUpdated(currentLobby);
                    }
                });
                
                playerRow.add(nameDiv, kickButton);
            } else {
                playerRow.add(nameDiv);
            }

            playersDiv.add(playerRow);
        }
    }
}