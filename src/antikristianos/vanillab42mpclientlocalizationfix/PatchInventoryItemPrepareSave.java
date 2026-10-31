package antikristianos.vanillab42mpclientlocalizationfix;

import java.nio.ByteBuffer;
import me.zed_0xff.zombie_buddy.Patch;
import zombie.inventory.InventoryItem;

@Patch(className = "zombie.inventory.InventoryItem", methodName = "save",
        warmUp = true, strictMatch = true)
public final class PatchInventoryItemPrepareSave {
    private PatchInventoryItemPrepareSave() {
    }

    @Patch.OnEnter
    public static void enter(@Patch.This InventoryItem item,
            @Patch.Argument(0) ByteBuffer output, @Patch.Argument(1) boolean net) {
        LocalizationRuntime.prepareSerializedItemName(item);
    }
}
