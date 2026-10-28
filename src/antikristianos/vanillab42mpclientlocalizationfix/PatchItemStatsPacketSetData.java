package antikristianos.vanillab42mpclientlocalizationfix;

import me.zed_0xff.zombie_buddy.Patch;

@Patch(
        className = "zombie.network.packets.ItemStatsPacket",
        methodName = "setData",
        warmUp = true,
        strictMatch = true)
public final class PatchItemStatsPacketSetData {
    private PatchItemStatsPacketSetData() {
    }

    @Patch.OnExit(suppress = RuntimeException.class)
    public static void exit(
            @Patch.This Object packet,
            @Patch.Argument(0) Object[] arguments) {
        LocalizationRuntime.encodeItemPacket(packet, arguments);
    }
}
