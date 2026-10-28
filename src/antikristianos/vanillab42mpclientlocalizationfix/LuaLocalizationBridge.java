package antikristianos.vanillab42mpclientlocalizationfix;

import me.zed_0xff.zombie_buddy.Exposer;
import zombie.inventory.InventoryItem;

@Exposer.LuaClass(name = "VB42Localization.Runtime")
public final class LuaLocalizationBridge {
    private LuaLocalizationBridge() {
    }

    public static boolean applyEvolvedRecipeName(InventoryItem baseItem) {
        return LocalizationRuntime.applyEvolvedRecipeName(baseItem);
    }

    public static boolean applyFishingRodName(InventoryItem rod) {
        return LocalizationRuntime.applyFishingRodName(rod);
    }

    public static String normalizeMediaRecipeCodes(String codes) {
        return MediaRecipeRuntime.normalizeCodes(codes);
    }

    public static boolean isRegisteredRecipeKey(String key) {
        return MediaRecipeRuntime.isRegisteredRecipeKey(key);
    }
}
