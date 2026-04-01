package com.example.uno.view;

import com.example.uno.model.Player;
import com.example.uno.service.LobbyService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

@Route("")
public class MainView extends VerticalLayout {

    public MainView(LobbyService lobbyService) {

        
        
        TextField nameField = new TextField("Username");

        Button hostButton = new Button("Host Game", e -> {

            //Add player name as an attribute
            Player player = new Player(nameField.getValue());
            UI.getCurrent().getSession().setAttribute(Player.class, player);

            var lobby = lobbyService.createLobby(player);
            getUI().ifPresent(ui ->
                ui.navigate("lobby/" + lobby.getRoomCode()));
        });

        TextField joinField = new TextField("Room Code");

        Button joinButton = new Button("Join Game", e -> {

            //Add player name as an attribute
            Player player = new Player(nameField.getValue());
            UI.getCurrent().getSession().setAttribute(Player.class, player);

            getUI().ifPresent(ui ->
                ui.navigate("lobby/" + joinField.getValue()));
        });

        add(nameField, joinField, hostButton, joinButton);
    }
}