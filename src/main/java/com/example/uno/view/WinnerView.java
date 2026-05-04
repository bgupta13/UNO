package com.example.uno.view;

import com.example.uno.service.GameListener;
import com.example.uno.model.GameState;
import com.example.uno.service.GameService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.shared.Registration;

@Route("winner/:code")
public class WinnerView extends VerticalLayout implements BeforeEnterObserver {

    private final GameService gameService;

    private String code;
    private GameState game;

    private Registration listenerRegistration;

    private final Button menuButton = new Button("Menu");

    public WinnerView(GameService gameService) {
        this.gameService = gameService;

        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        removeAll();

        code = event.getRouteParameters().get("code").orElse("");
        game = gameService.getGame(code);

        menuButton.addClickListener(e -> {
            cleanupAndExit();
        });

        add(menuButton);

        if (game == null) {
            add(new Span("Game not found."));
            return;
        }

        registerListener();

        if (game.getWinner() != null) {
            showWinner();
        } else {
            add(new Span("Waiting for game to finish..."));
        }
    }

    private void registerListener() {
        UI ui = UI.getCurrent();

        GameListener listener = new GameListener() {
            @Override
            public void onGameUpdated(GameState g) {
                if (!g.equals(game)) return;

                ui.access(() -> {
                    if (g.getWinner() != null) {
                        showWinner();
                    }
                });
            }

            @Override
            public void onGameEnded(GameState g) {
                if (!g.equals(game)) return;

                ui.access(() -> showWinner());
            }
        };

        game.addListener(listener);
        listenerRegistration = () -> game.removeListener(listener);
    }

    private void showWinner() {
        removeAll();
        add(menuButton);

        if (game.getWinner() != null) {
            add(new Span(game.getWinner().getName() + " has won the game!"));
        } else {
            add(new Span("Game ended."));
        }
    }

    private void cleanupAndExit() {
        gameService.endSession(code);

        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }

        UI.getCurrent().navigate("");
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }
    }
}