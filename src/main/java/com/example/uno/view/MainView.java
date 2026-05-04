package com.example.uno.view;

import com.example.uno.model.Player;
import com.example.uno.service.LobbyService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.component.html.Span;

@Route("")
public class MainView extends VerticalLayout {

    public MainView(LobbyService lobbyService) {

        // Menu Styling
        VerticalLayout menu = new VerticalLayout();
        menu.setAlignItems(FlexComponent.Alignment.CENTER);
        menu.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        menu.setSizeFull();

        // Tile styling & layout
        Span titleText = new Span("UNO");
        titleText.getStyle().set("font-size", "150px");
        titleText.getStyle().set("color", "yellow");
    

        // Host button logic
        Button hostButton = new Button("Host Game", e -> {
            Dialog dialog = new Dialog();

            TextField nameField = new TextField("Enter Username:");
            Button confirm = new Button("Host Game", event -> {

                Player player = new Player(nameField.getValue()); // Create player object
                UI.getCurrent().getSession().setAttribute(Player.class, player); // Add player object as attribute

                var lobby = lobbyService.createLobby(player); // Make player the lobby host

                dialog.close();

                getUI().ifPresent(ui ->
                    ui.navigate("lobby/" + lobby.getRoomCode()));
            });

            dialog.add(nameField, confirm);
            dialog.open();
        });


        // Join button logic
        Button joinButton = new Button("Join Game", e -> {
            Dialog dialog = new Dialog();

            TextField nameField = new TextField("Enter Username:");
            TextField roomField = new TextField("Enter Room Code:");

            Button confirm = new Button("Join Lobby", event -> {

                Player player = new Player(nameField.getValue()); // Create player object
                UI.getCurrent().getSession().setAttribute(Player.class, player); // Add player object as attribute

                dialog.close();

                getUI().ifPresent(ui ->
                    ui.navigate("lobby/" + roomField.getValue()));
            });

            dialog.add(nameField, roomField, confirm);
            dialog.open();
        });

        menu.add(titleText, hostButton, joinButton); 
        add(menu);
    }
}