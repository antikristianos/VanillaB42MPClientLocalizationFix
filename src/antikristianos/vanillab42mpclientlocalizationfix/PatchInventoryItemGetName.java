package antikristianos.vanillab42mpclientlocalizationfix;

import me.zed_0xff.zombie_buddy.Patch;
import zombie.characters.IsoPlayer;
import zombie.inventory.InventoryItem;

@Patch(className = "zombie.inventory.InventoryItem", methodName = "getName",
        warmUp = true, strictMatch = true)
public final class PatchInventoryItemGetName {
    private PatchInventoryItemGetName() {
    }

    @Patch.OnExit(suppress = RuntimeException.class)
    public static void exit(
            @Patch.This InventoryItem item,
            @Patch.Argument(0) IsoPlayer player,
            @Patch.Return(readOnly = false) String resolvedText) {
        resolvedText = LocalizationRuntime.localizeItemNameView(item, resolvedText);
    }
}
