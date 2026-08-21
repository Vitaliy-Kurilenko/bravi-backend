package ua.com.bravi.bravi.seller.tags.domain;

import java.util.List;

/**
 * The colours a tag falls back on when nobody picked one — which is every tag minted implicitly
 * from a name typed on a product card. The set is fixed rather than configured: it does not vary
 * between environments, and the same values seed the backfill of the colour column.
 */
public final class TagPalette {

    private static final List<String> COLORS = List.of(
            "#E5484D", "#F76B15", "#FFB224", "#46A758",
            "#12A594", "#0091FF", "#8E4EC6", "#E93D82");

    private TagPalette() {
    }

    /**
     * Picks a colour from the tag's own dedup key, so one name always comes out the same colour.
     * Deriving it rather than counting what the store already has keeps the choice free of both a
     * second query and the race between two saves minting the same name at once.
     */
    public static String pick(String nameKey) {
        return COLORS.get(Math.floorMod(nameKey.hashCode(), COLORS.size()));
    }

    public static List<String> colors() {
        return COLORS;
    }
}
