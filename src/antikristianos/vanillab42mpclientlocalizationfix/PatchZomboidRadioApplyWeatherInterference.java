package antikristianos.vanillab42mpclientlocalizationfix;

import me.zed_0xff.zombie_buddy.Patch;

@Patch(
        className = "zombie.radio.ZomboidRadio",
        methodName = "applyWeatherInterference",
        warmUp = true,
        strictMatch = true)
public final class PatchZomboidRadioApplyWeatherInterference {
    private PatchZomboidRadioApplyWeatherInterference() {
    }

    @Patch.OnEnter(skipOn = true)
    public static boolean enter(
            @Patch.This Object radio,
            @Patch.Argument(0) String message,
            @Patch.Argument(1) int signalStrength,
            @Patch.Local("localizedTransmissionHandled") boolean handled,
            @Patch.Local("localizedTransmissionResult") String replacement) {
        try {
            replacement = LocalizationRuntime.encodeWeatherInterference(radio, message, signalStrength);
            handled = replacement != null;
        } catch (RuntimeException ignored) {
            replacement = null;
            handled = false;
        }
        return handled;
    }

    @Patch.OnExit(suppress = RuntimeException.class)
    public static void exit(
            @Patch.Local("localizedTransmissionHandled") boolean handled,
            @Patch.Local("localizedTransmissionResult") String replacement,
            @Patch.Return(readOnly = false) String result) {
        if (handled) {
            result = replacement;
        }
    }
}
