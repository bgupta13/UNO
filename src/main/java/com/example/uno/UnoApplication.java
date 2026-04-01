package com.example.uno;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;

@SpringBootApplication
@StyleSheet("styles.css")
public class UnoApplication implements AppShellConfigurator {

    public static void main(String[] args) {
        SpringApplication.run(UnoApplication.class, args);
    }
}
