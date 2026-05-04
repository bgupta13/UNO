package com.example.uno.model;

import java.util.Objects;

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

    public enum PartyType {
        KARL_MARX,
        SWAPPER,
        ROTATER
    }

    private final Color color;
    private final Type type;
    private final int number;
    private final PartyType partyType;

    public Card(Color color, int number) {
        if (color == null || color == Color.WILD) {
            throw new IllegalArgumentException("Number cards must have a normal color.");
        }

        if (number < 0 || number > 9) {
            throw new IllegalArgumentException("UNO number cards must be between 0 and 9.");
        }

        this.color = color;
        this.type = Type.NUMBER;
        this.number = number;
        this.partyType = null;
    }

    public Card(Color color, Type type) {
        if (color == null || type == null) {
            throw new IllegalArgumentException("Card color and type cannot be null.");
        }

        if (type == Type.NUMBER || type == Type.PARTY) {
            throw new IllegalArgumentException("Use the number or party constructor for this card type.");
        }

        this.color = color;
        this.type = type;
        this.number = -1;
        this.partyType = null;
    }

    public Card(PartyType partyType) {
        if (partyType == null) {
            throw new IllegalArgumentException("Party card type cannot be null.");
        }

        this.color = Color.WILD;
        this.type = Type.PARTY;
        this.number = -1;
        this.partyType = partyType;
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

    public PartyType getPartyType() {
        return partyType;
    }

    public boolean isWildLike() {
        return type == Type.WILD ||
               type == Type.WILD_DRAW_FOUR ||
               type == Type.PARTY;
    }

    public boolean isStackCard() {
        return type == Type.DRAW_TWO || type == Type.WILD_DRAW_FOUR;
    }

    public String getImageName(Card.Color activeColor) {
        if (type == Type.NUMBER) {
            return color + "_" + number + ".PNG";
        }

        if (type == Type.PARTY) {
            return activeColor + "_" + type + "_" + partyType + ".PNG";
        }

        if (type == Type.WILD || type == Type.WILD_DRAW_FOUR) {
            return activeColor + "_" + type + ".PNG";
        }

        return color + "_" + type + ".PNG";
    }


    @Override
    public String toString() {
        if (type == Type.NUMBER) {
            return color + "_" + number;
        }

        if (type == Type.PARTY) {
            return "PARTY" + "_" + partyType;
        }

        return color + "_" + type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Card)) return false;
        Card card = (Card) o;
        return number == card.number &&
               color == card.color &&
               type == card.type &&
               partyType == card.partyType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(color, type, number, partyType);
    }
}
