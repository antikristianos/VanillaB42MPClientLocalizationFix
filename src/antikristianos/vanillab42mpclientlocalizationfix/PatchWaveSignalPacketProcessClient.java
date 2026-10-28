package antikristianos.vanillab42mpclientlocalizationfix;

import me.zed_0xff.zombie_buddy.Patch;

@Patch(
        className = "zombie.network.packets.WaveSignalPacket",
        methodName = "processClient",
        warmUp = true,
        strictMatch = true)
public final class PatchWaveSignalPacketProcessClient {
    private PatchWaveSignalPacketProcessClient() {
    }

    @Patch.OnEnter
    public static void enter(@Patch.This Object packet, @Patch.Argument(0) zombie.core.raknet.UdpConnection connection) {
        LocalizationRuntime.decodeWaveSignalPacket(packet);
    }
}
