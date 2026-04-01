package com.example.uno.model;

public class Card {

    public enum Color {
        RED, BLUE, GREEN, YELLOW, WILD
    }

    public enum Type {
        NUMBER,
        SKIP,
        REVERSE,
        DRAW_TWO,
        WILD,
        WILD_DRAW_FOUR,
        PARTY
    }

    private Color color;
    private Type type;
    private int number; // only used if NUMBER
    private String partyName; // optional for party cards

    // Constructor for number cards
    public Card(Color color, int number) {
        this.color = color;
        this.type = Type.NUMBER;
        this.number = number;
    }

    // Constructor for action cards
    public Card(Color color, Type type) {
        this.color = color;
        this.type = type;
    }

    // Constructor for party cards
    public Card(String partyName) {
        this.color = Color.WILD;
        this.type = Type.PARTY;
        this.partyName = partyName;
    }

    public Color getColor() {
        return color;
    }

    public Type getType() {
        return type;
    }

    public int getNumber() {
        return number;
    }

    public String getPartyName() {
        return partyName;
    }

    @Override
    public String toString() {
        if (type == Type.NUMBER) {
            return color + " " + number;
        }
        if (type == Type.PARTY) {
            return "PARTY: " + partyName;
        }
        return color + " " + type;
    }
}