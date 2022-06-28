package de.dargmuesli.spotilist.models.enums;

import java.util.HashMap;
import java.util.Map;

public enum AlbumType {
    ALBUM("album"),
    APPEARS_ON("appears_on"),
    COMPILATION("compilation"),
    SINGLE("single");

    private static final Map<String, AlbumType> map = new HashMap<>();

    static {
        for (AlbumType albumType : AlbumType.values()) {
            map.put(albumType.type, albumType);
        }
    }

    public final String type;

    AlbumType(final String type) {
        this.type = type;
    }

    public static AlbumType keyOf(String type) {
        return map.get(type);
    }

    public String getType() {
        return type;
    }
}
