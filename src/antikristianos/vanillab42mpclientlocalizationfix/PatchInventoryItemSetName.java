package antikristianos.vanillab42mpclientlocalizationfix;

import me.zed_0xff.zombie_buddy.Patch;
import zombie.inventory.InventoryItem;

@Patch(className = "zombie.inventory.InventoryItem", methodName = "setName",
        warmUp = true, strictMatch = true)
public final class PatchInventoryItemSetName {
    private PatchInventoryItemSetName() {
    }

    @Patch.OnExit(suppress = RuntimeException.class)
    public static void exit(
            @Patch.This InventoryItem item,
            @Patch.Argument(0) String value) {
        LocalizationRuntime.rememberItemName(item, value);
    }
}
