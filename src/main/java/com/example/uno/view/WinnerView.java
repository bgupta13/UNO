package com.example.uno.view;

import com.example.uno.model.GameState;
import com.example.uno.service.GameService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;

@Route("winner/:code")
public class WinnerView extends VerticalLayout implements BeforeEnterObserver {

    private GameService gameService;
    private Button menuButton = new Button("Menu");

    public WinnerView(GameService gameService) {
        this.gameService = gameService;

        setSizeFull();  
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        removeAll();

        String code = event.getRouteParameters()
                .get("code")
                .orElse("");

        GameState game = gameService.getGame(code);

        if (game == null || game.getWinner() == null) {
            add(new Span("No winner found."));
            return;
        }

        add(new Span(game.getWinner().getName() + " has won the game!"));

        menuButton.addClickListener(e -> {
            UI.getCurrent().navigate("");
        });
        add(menuButton);
    }
}