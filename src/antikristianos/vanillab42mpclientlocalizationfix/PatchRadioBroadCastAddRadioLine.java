package antikristianos.vanillab42mpclientlocalizationfix;

import me.zed_0xff.zombie_buddy.Patch;
import zombie.radio.scripting.RadioLine;

@Patch(
        className = "zombie.radio.scripting.RadioBroadCast",
        methodName = "AddRadioLine",
        warmUp = true,
        strictMatch = true)
public final class PatchRadioBroadCastAddRadioLine {
    private PatchRadioBroadCastAddRadioLine() {
    }

    @Patch.OnEnter
    public static void enter(@Patch.Argument(0) RadioLine line) {
        try {
            LocalizationRuntime.captureRadioLine(line);
        } catch (RuntimeException ignored) {
            // Keep Vanilla loading behavior if the optional localization bridge cannot capture this line.
        }
    }
}
