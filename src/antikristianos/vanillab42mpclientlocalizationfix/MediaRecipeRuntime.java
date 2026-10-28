package antikristianos.vanillab42mpclientlocalizationfix;

import java.util.Map;
import zombie.SandboxOptions;
import zombie.characters.IsoGameCharacter;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.scripting.ScriptManager;
import zombie.scripting.entity.components.crafting.CraftRecipe;

/** Media source corrections only: never translates or rewrites stored knowledge. */
public final class MediaRecipeRuntime {
    private static final Map<String, String> SOURCE_IDS = Map.of(
            "Make Cake Batter", "MakeCakeBatter",
            "Make Fishing Rod", "MakeFishingRod",
            "Make Pie Dough", "MakePieDough",
            "Make Crate Trap", "MakeWoodenBoxTrap",
            "MakeFliesCure", "MakeFliesCureFromCigarettes");

    private MediaRecipeRuntime() {
    }

    public static boolean isMultiplayer() {
        return GameServer.server || GameClient.client;
    }

    public static String normalizeCodes(String codes) {
        if (!isMultiplayer() || codes == null || !codes.contains("RCP=")) {
            return codes;
        }
        String[] tokens = codes.split(",", -1);
        for (int index = 0; index < tokens.length; index++) {
            String token = tokens[index].trim();
            if (token.startsWith("RCP=")) {
                String key = token.substring(4).trim();
                tokens[index] = "RCP=" + SOURCE_IDS.getOrDefault(key, key);
            }
        }
        return String.join(",", tokens);
    }

    /** Farming season keys are checked against the loaded Lua crop definitions. */
    public static boolean isRegisteredRecipeKey(String key) {
        if (key == null || key.isEmpty()) {
            return false;
        }
        ScriptManager scripts = ScriptManager.instance;
        if (scripts.getCraftRecipe(key) != null || scripts.getRecipe(key) != null) {
            return true;
        }
        for (CraftRecipe recipe : scripts.getAllCraftRecipes()) {
            if (key.equals(recipe.getMetaRecipe())) {
                return true;
            }
        }
        for (CraftRecipe recipe : scripts.getAllBuildableRecipes()) {
            if (key.equals(recipe.getMetaRecipe())) {
                return true;
            }
        }
        return false;
    }

    /** Vanilla's CraftRecipe predicate, with only its translated-name fallback removed. */
    public static boolean isRecipeKnown(IsoGameCharacter player, CraftRecipe recipe, boolean actual) {
        if ((!actual && SandboxOptions.instance.seeNotLearntRecipe.getValue())
                || player.isKnowAllRecipes() || !recipe.needToBeLearn()) {
            return true;
        }
        return player.getKnownRecipes().contains(recipe.getName())
                || player.getKnownRecipes().contains(recipe.getMetaRecipe());
    }
}
