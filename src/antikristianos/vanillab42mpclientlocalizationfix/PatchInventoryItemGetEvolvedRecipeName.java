package antikristianos.vanillab42mpclientlocalizationfix;

import me.zed_0xff.zombie_buddy.Patch;
import zombie.inventory.InventoryItem;

@Patch(className = "zombie.inventory.InventoryItem", methodName = "getEvolvedRecipeName",
        warmUp = true, strictMatch = true)
public final class PatchInventoryItemGetEvolvedRecipeName {
    private PatchInventoryItemGetEvolvedRecipeName() {
    }

    @Patch.OnExit(suppress = RuntimeException.class)
    public static void exit(
            @Patch.This InventoryItem item,
            @Patch.Return(readOnly = false) String resolvedText) {
        resolvedText = LocalizationRuntime.recordInventoryItemEvolvedRecipeName(item, resolvedText);
    }
}
