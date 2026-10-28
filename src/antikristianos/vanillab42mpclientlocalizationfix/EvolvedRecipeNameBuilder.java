package antikristianos.vanillab42mpclientlocalizationfix;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class EvolvedRecipeNameBuilder {
    static final int GENERATED_NAME_LIMIT = 3;

    private EvolvedRecipeNameBuilder() {
    }

    static Result build(
            String baseItemType,
            String recipeUntranslatedName,
            List<Ingredient> extraItems,
            List<Ingredient> spices) {
        List<LocalizedText> baseNames = groupedNames(baseItemType, extraItems);
        int baseGroupCount = baseNames.size();
        boolean collapsed = baseGroupCount > GENERATED_NAME_LIMIT;
        List<LocalizedText> displayedBaseNames = collapsed ? List.of() : baseNames;

        LocalizedText recipeName = LocalizedText.translation(
                "ContextMenu_EvolvedRecipe_" + recipeUntranslatedName,
                List.of());
        LocalizedText result = displayedBaseNames.isEmpty()
                ? recipeName
                : LocalizedText.translation(
                        "ContextMenu_EvolvedRecipe_RecipeName",
                        List.of(join(displayedBaseNames), recipeName));

        List<Ingredient> namedSpices = new ArrayList<>();
        for (Ingredient spice : spices) {
            if (spice != null && !spice.minorIngredient()) {
                namedSpices.add(spice);
            }
        }
        List<LocalizedText> spiceNames = groupedNames(baseItemType, namedSpices);
        int displayedBaseCount = displayedBaseNames.size();
        if (!spiceNames.isEmpty()
                && displayedBaseCount + spiceNames.size() <= GENERATED_NAME_LIMIT) {
            LocalizedText joinedSpices = join(spiceNames);
            if (displayedBaseNames.isEmpty()) {
                result = LocalizedText.translation(
                        "ContextMenu_EvolvedRecipe_RecipeNameNew2",
                        List.of(
                                recipeName,
                                LocalizedText.translation(
                                        "ContextMenu_EvolvedRecipe_with", List.of()),
                                joinedSpices));
            } else {
                result = LocalizedText.translation(
                        "ContextMenu_EvolvedRecipe_RecipeNameNew",
                        List.of(join(displayedBaseNames), recipeName, joinedSpices));
            }
        }

        return new Result(result, baseGroupCount, displayedBaseCount, spiceNames.size(), collapsed);
    }

    private static List<LocalizedText> groupedNames(
            String baseItemType,
            List<Ingredient> ingredients) {
        Map<String, Group> groups = new LinkedHashMap<>();
        for (Ingredient ingredient : ingredients) {
            if (ingredient == null || ingredient.fullType() == null
                    || ingredient.fullType().isBlank()) {
                continue;
            }
            String foodType = ingredient.foodType();
            if (foodType == null || foodType.isBlank() || "NoExplicit".equals(foodType)) {
                if (groups.isEmpty()) {
                    groups.putIfAbsent("NoExplicit", new Group(ingredient.fullType(), false));
                }
                continue;
            }

            groups.remove("NoExplicit");
            Group previous = groups.get(foodType);
            if (previous == null) {
                groups.put(foodType, new Group(ingredient.fullType(), false));
            } else if (!previous.fullType().equals(ingredient.fullType())) {
                groups.put(foodType, new Group(previous.fullType(), true));
            }
        }

        List<LocalizedText> names = new ArrayList<>();
        for (Map.Entry<String, Group> entry : groups.entrySet()) {
            Group group = entry.getValue();
            if ("FruitSalad".equals(baseItemType)
                    && group.collapsed()
                    && "Fruits".equals(entry.getKey())) {
                continue;
            }
            names.add(group.collapsed()
                    ? LocalizedText.translation(
                            "ContextMenu_FoodType_" + entry.getKey(), List.of())
                    : LocalizedText.evolvedItem(group.fullType()));
        }
        return names;
    }

    private static LocalizedText join(List<LocalizedText> names) {
        if (names.size() == 1) {
            return names.get(0);
        }
        List<LocalizedText> parts = new ArrayList<>();
        for (int index = 0; index < names.size(); index++) {
            if (index > 0) {
                if (index == names.size() - 1) {
                    parts.add(LocalizedText.literal(" "));
                    parts.add(LocalizedText.translation(
                            "ContextMenu_EvolvedRecipe_and", List.of()));
                    parts.add(LocalizedText.literal(" "));
                } else {
                    parts.add(LocalizedText.translation(
                            "ContextMenu_EvolvedRecipe_comma", List.of()));
                    parts.add(LocalizedText.literal(" "));
                }
            }
            parts.add(names.get(index));
        }
        return LocalizedText.composite(parts);
    }

    record Ingredient(String fullType, String foodType, boolean minorIngredient) {
    }

    record Result(
            LocalizedText text,
            int baseGroupCount,
            int displayedBaseGroupCount,
            int spiceGroupCount,
            boolean collapsed) {
    }

    private record Group(String fullType, boolean collapsed) {
    }
}
