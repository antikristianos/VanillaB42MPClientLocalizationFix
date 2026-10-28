package antikristianos.vanillab42mpclientlocalizationfix;

import me.zed_0xff.zombie_buddy.Patch;
import zombie.inventory.InventoryItem;

@Patch(className = "zombie.inventory.InventoryItem", methodName = "load",
        warmUp = true, strictMatch = true)
public final class PatchInventoryItemLoad {
    private PatchInventoryItemLoad() {
    }

    @Patch.OnExit(suppress = RuntimeException.class)
    public static void exit(@Patch.This InventoryItem item, @Patch.Argument(0) java.nio.ByteBuffer input,
            @Patch.Argument(1) int worldVersion) {
        LocalizationRuntime.decodeSerializedItemName(item);
    }
}
