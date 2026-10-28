package antikristianos.vanillab42mpclientlocalizationfix;

import me.zed_0xff.zombie_buddy.Patch;

@Patch(className = "zombie.network.packets.MessageForAdminPacket", methodName = "parse",
        warmUp = true, strictMatch = true)
public final class PatchMessageForAdminPacketParse {
    private PatchMessageForAdminPacketParse() {
    }

    @Patch.OnExit(suppress = RuntimeException.class)
    public static void exit(@Patch.This Object packet, @Patch.Argument(0) zombie.core.network.ByteBufferReader input,
            @Patch.Argument(1) zombie.network.IConnection connection) {
        LocalizationRuntime.decodeAdminPacket(packet);
    }
}
