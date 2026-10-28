package antikristianos.vanillab42mpclientlocalizationfix;

import me.zed_0xff.zombie_buddy.Patch;
import zombie.inventory.InventoryItem;

@Patch(
        className = "zombie.network.packets.ItemStatsPacket",
        methodName = "applyItemStats",
        warmUp = true,
        strictMatch = true)
public final class PatchItemStatsPacketApplyItemStats {
    private PatchItemStatsPacketApplyItemStats() {
    }

    @Patch.OnEnter
    public static void enter(
            @Patch.This Object packet,
            @Patch.Argument(0) InventoryItem item,
            @Patch.Local("preservedItemNameState") Object nameState) {
        nameState = LocalizationRuntime.captureItemPacketNameState(packet, item);
    }

    @Patch.OnExit(suppress = RuntimeException.class)
    public static void exit(
            @Patch.This Object packet,
            @Patch.Argument(0) InventoryItem item,
            @Patch.Local("preservedItemNameState") Object nameState) {
        LocalizationRuntime.decodeItemPacket(packet, item, nameState);
    }
}
