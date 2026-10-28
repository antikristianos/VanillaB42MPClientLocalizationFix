package antikristianos.vanillab42mpclientlocalizationfix;

import me.zed_0xff.zombie_buddy.Patch;
import zombie.characters.IsoPlayer;

@Patch(className = "zombie.radio.devices.WaveSignalDevice", methodName = "AddDeviceText",
        warmUp = true, strictMatch = true)
public final class PatchWaveSignalDeviceAddDeviceText {
    private PatchWaveSignalDeviceAddDeviceText() {
    }

    @Patch.OnEnter
    public static void enter(
            @Patch.Argument(0) IsoPlayer player,
            @Patch.Argument(value = 1, readOnly = false) String message,
            @Patch.Argument(2) float red,
            @Patch.Argument(3) float green,
            @Patch.Argument(4) float blue,
            @Patch.Argument(5) String guid,
            @Patch.Argument(6) String codes,
            @Patch.Argument(7) int distance) {
        message = LocalizationRuntime.decodeBroadcastDisplayText(message);
    }
}
