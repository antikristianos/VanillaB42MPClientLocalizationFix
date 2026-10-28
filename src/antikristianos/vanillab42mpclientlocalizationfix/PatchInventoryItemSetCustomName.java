package antikristianos.vanillab42mpclientlocalizationfix;

import me.zed_0xff.zombie_buddy.Patch;
import zombie.inventory.InventoryItem;

@Patch(className = "zombie.inventory.InventoryItem", methodName = "setCustomName",
        warmUp = true, strictMatch = true)
public final class PatchInventoryItemSetCustomName {
    private PatchInventoryItemSetCustomName() {
    }

    @Patch.OnExit(suppress = RuntimeException.class)
    public static void exit(
            @Patch.This InventoryItem item,
            @Patch.Argument(0) boolean customName) {
        LocalizationRuntime.updateItemCustomName(item, customName);
    }
}
