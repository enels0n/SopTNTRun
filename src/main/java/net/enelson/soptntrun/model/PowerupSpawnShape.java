package net.enelson.soptntrun.model;

import java.util.Locale;

public enum PowerupSpawnShape {

    CIRCLE,
    SQUARE;

    public static PowerupSpawnShape parse(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return CIRCLE;
        }
        try {
            return PowerupSpawnShape.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return CIRCLE;
        }
    }
}
