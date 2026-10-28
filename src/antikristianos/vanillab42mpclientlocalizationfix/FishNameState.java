package antikristianos.vanillab42mpclientlocalizationfix;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** Completes proven Vanilla fish names from the owning item's identity. */
final class FishNameState {
    private static final Set<String> SIZE_KEYS = Set.of(
            "IGUI_Fish_Small", "IGUI_Fish_Medium", "IGUI_Fish_Big", "IGUI_Fish_Legendary");

    private FishNameState() {
    }

    static LocalizedText complete(LocalizedText text, String fullType, String... speciesNames) {
        if (text == null || fullType == null || fullType.indexOf('.') <= 0
                || text.kind() != LocalizedText.Kind.COMPOSITE || text.children().isEmpty()
                || text.children().get(0).plainTranslationKey() == null
                || !SIZE_KEYS.contains(text.children().get(0).plainTranslationKey())) {
            return text;
        }
        // The size key proves system provenance. Match only this item's known base
        // names; never reverse-translate arbitrary fish-looking custom names.
        Map<String, LocalizedText> origins = new HashMap<>();
        for (String speciesName : speciesNames) {
            if (speciesName != null && !speciesName.isEmpty()) {
                origins.put(speciesName, LocalizedText.item(fullType));
            }
        }
        return text.rewriteLiterals(origins);
    }
}
