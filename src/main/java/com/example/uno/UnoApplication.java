package com.example.uno;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.StyleSheet;

@SpringBootApplication
@StyleSheet("styles.css")
public class UnoApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(UnoApplication.class, args);
    }
}
