package antikristianos.vanillab42mpclientlocalizationfix;

import me.zed_0xff.zombie_buddy.Patch;

@Patch(className = "zombie.inventory.types.Radio", methodName = "AddDeviceText",
        warmUp = true, strictMatch = true)
public final class PatchRadioAddDeviceText {
    private PatchRadioAddDeviceText() {
    }

    @Patch.OnEnter
    public static void enter(
            @Patch.Argument(value = 0, readOnly = false) String message,
            @Patch.Argument(1) float red,
            @Patch.Argument(2) float green,
            @Patch.Argument(3) float blue,
            @Patch.Argument(4) String guid,
            @Patch.Argument(5) String codes,
            @Patch.Argument(6) int distance) {
        message = LocalizationRuntime.decodeBroadcastDisplayText(message);
    }
}
