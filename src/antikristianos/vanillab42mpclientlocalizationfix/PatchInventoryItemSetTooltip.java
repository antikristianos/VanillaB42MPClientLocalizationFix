package antikristianos.vanillab42mpclientlocalizationfix;

import me.zed_0xff.zombie_buddy.Patch;
import zombie.inventory.InventoryItem;

@Patch(className = "zombie.inventory.InventoryItem", methodName = "setTooltip",
        warmUp = true, strictMatch = true)
public final class PatchInventoryItemSetTooltip {
    private PatchInventoryItemSetTooltip() {
    }

    @Patch.OnEnter
    public static void enter(
            @Patch.This InventoryItem item,
            @Patch.Argument(value = 0, readOnly = false) String value) {
        value = LocalizationRuntime.rememberItemTooltip(item, value);
    }
}
