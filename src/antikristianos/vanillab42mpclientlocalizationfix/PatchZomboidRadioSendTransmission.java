package antikristianos.vanillab42mpclientlocalizationfix;

import me.zed_0xff.zombie_buddy.Patch;

@Patch(
        className = "zombie.radio.ZomboidRadio",
        methodName = "SendTransmission",
        warmUp = true,
        strictMatch = true)
public final class PatchZomboidRadioSendTransmission {
    private PatchZomboidRadioSendTransmission() {
    }

    @Patch.OnEnter
    public static void enter(@Patch.Argument(value = 3, readOnly = false) String message) {
        try {
            message = LocalizationRuntime.encodeServerTransmission(message);
        } catch (RuntimeException ignored) {
            // Retain the original message when this optional boundary cannot be encoded.
        }
    }
}
