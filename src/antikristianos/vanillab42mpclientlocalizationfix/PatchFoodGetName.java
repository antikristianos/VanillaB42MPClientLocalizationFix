package antikristianos.vanillab42mpclientlocalizationfix;

import me.zed_0xff.zombie_buddy.Patch;
import zombie.characters.IsoPlayer;
import zombie.inventory.types.Food;

@Patch(className = "zombie.inventory.types.Food", methodName = "getName",
        warmUp = true, strictMatch = true)
public final class PatchFoodGetName {
    private PatchFoodGetName() {
    }

    @Patch.OnExit(suppress = RuntimeException.class)
    public static void exit(
            @Patch.This Food food,
            @Patch.Argument(0) IsoPlayer player,
            @Patch.Return(readOnly = false) String resolvedText) {
        resolvedText = LocalizationRuntime.localizeItemNameView(food, resolvedText);
    }
}
