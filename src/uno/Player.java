package uno;

import java.util.Objects;

public class Player {
    private String name;
    private boolean ai;

    public Player(String name) {
        this(name, false);
    }

    public Player(String name, boolean ai) {
        this.name = name;
        this.ai = ai;
    }

    public String getName() {
        return name;
    }

    public boolean isAI() {
        return ai;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Player)) return false;
        Player player = (Player) o;
        return Objects.equals(name, player.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return name + (ai ? " (AI)" : "");
    }
}
