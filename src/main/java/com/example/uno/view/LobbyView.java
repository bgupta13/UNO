package com.example.uno.view;

import com.example.uno.model.*;
import com.example.uno.service.GameService;
import com.example.uno.service.LobbyService;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;

@Route("lobby/:code")
public class LobbyView extends VerticalLayout implements BeforeEnterObserver {

    private LobbyService lobbyService;
    private Div playersDiv = new Div();

    private Lobby currentLobby;
    private GameService gameService;

    



    public LobbyView(LobbyService lobbyService, GameService gameService) {

        //Time interval for checking if game starts
        UI.getCurrent().setPollInterval(2000);
        UI.getCurrent().addPollListener(e -> {
            if (gameService.getGame(currentLobby.getRoomCode()) != null) {
                UI.getCurrent().navigate("game/" + currentLobby.getRoomCode());
            }
        });

        this.lobbyService = lobbyService;
        this.gameService = gameService;
        add("Players");
        add(playersDiv);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        String code = event.getRouteParameters().get("code").orElse("");
        Lobby lobby = lobbyService.getLobby(code);

        //Valid lobby check
        if (lobby == null) {
            add("Lobby not found");
            return;
        }

        this.currentLobby = lobby;

        //Checking that player has a name
        Player player = UI.getCurrent().getSession().getAttribute(Player.class);
        if (player == null) {
            add("Invalid name");
            return;
        }

        //Adding player to lobby
        if (!lobby.getPlayers().contains(player)) {
            lobby.addPlayer(player);
        }

        add("Game code: " + code);

        //Temp refresh button
        Button refreshButton = new Button("Refresh", e -> refreshPlayers(lobby));
        add(refreshButton);


        
        refreshPlayers(lobby);
        addStartButton(player);
    }

    //TODO: have automatic refresh work without button
    private void refreshPlayers(Lobby lobby) {
        playersDiv.removeAll();
        lobby.getPlayers().forEach(p ->
            playersDiv.add(new Div(p.getName()))
        );
    }

    private void addStartButton(Player player) {

        //Check if player is host
        if (!player.equals(currentLobby.getHost())) {
            return;
        }



        Button startButton = new Button("Start Game", e -> {
            gameService.startGame(currentLobby);

            // Navigate host
            getUI().ifPresent(ui ->
                ui.navigate("game/" + currentLobby.getRoomCode())
            );
        });

        add(startButton);
    }
}