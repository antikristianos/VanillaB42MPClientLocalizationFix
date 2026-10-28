package antikristianos.vanillab42mpclientlocalizationfix;

import me.zed_0xff.zombie_buddy.Patch;
import zombie.inventory.InventoryItem;

@Patch(className = "zombie.inventory.InventoryItem", methodName = "getTooltip",
        warmUp = true, strictMatch = true)
public final class PatchInventoryItemGetTooltip {
    private PatchInventoryItemGetTooltip() {
    }

    @Patch.OnExit(suppress = RuntimeException.class)
    public static void exit(
            @Patch.This InventoryItem item,
            @Patch.Return(readOnly = false) String value) {
        value = LocalizationRuntime.localizeItemTooltip(item, value);
    }
}
