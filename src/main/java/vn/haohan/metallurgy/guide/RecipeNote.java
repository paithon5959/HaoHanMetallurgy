package vn.haohan.metallurgy.guide;

import java.util.Objects;

/** A short, single-row operating note rendered by the forge guide. */
public record RecipeNote(String text, Tone tone) {
    public RecipeNote {
        if (text == null || text.isBlank()) throw new IllegalArgumentException("note text cannot be blank");
        Objects.requireNonNull(tone, "tone");
    }

    public enum Tone {
        INFO,
        GOOD,
        WARNING,
        DANGER
    }
}
