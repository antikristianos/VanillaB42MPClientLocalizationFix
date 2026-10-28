package antikristianos.vanillab42mpclientlocalizationfix;

import java.util.List;

/** Completes the proven Vanilla wild-food formula, never parses a rendered suffix. */
final class WildFoodNameState {
    private WildFoodNameState() {
    }

    static LocalizedText complete(LocalizedText text, String fullType) {
        if (text == null || fullType == null || fullType.indexOf('.') <= 0
                || text.kind() != LocalizedText.Kind.COMPOSITE) {
            return text;
        }
        List<LocalizedText> parts = text.children();
        // doWildFoodSpawn: owning item base + " (" + UI_foraging_WildFood + ")".
        // A missing getter trace can leave the base and opening bracket as one
        // literal. The exact formula and preserved key supply provenance; the
        // item's fullType supplies identity, independent of the sender's language.
        if (parts.size() != 3
                || parts.get(0).kind() != LocalizedText.Kind.LITERAL
                || parts.get(0).value().length() <= 2
                || !parts.get(0).value().endsWith(" (")
                || !"UI_foraging_WildFood".equals(parts.get(1).plainTranslationKey())
                || parts.get(2).kind() != LocalizedText.Kind.LITERAL
                || !")".equals(parts.get(2).value())) {
            return text;
        }
        return LocalizedText.composite(List.of(LocalizedText.item(fullType),
                LocalizedText.literal(" ("), parts.get(1), parts.get(2)));
    }
}
