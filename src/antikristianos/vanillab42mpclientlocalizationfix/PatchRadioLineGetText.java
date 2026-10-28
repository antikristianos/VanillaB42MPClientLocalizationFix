package antikristianos.vanillab42mpclientlocalizationfix;

import me.zed_0xff.zombie_buddy.Patch;
import zombie.radio.scripting.RadioLine;

@Patch(
        className = "zombie.radio.scripting.RadioLine",
        methodName = "getText",
        warmUp = true,
        strictMatch = true)
public final class PatchRadioLineGetText {
    private PatchRadioLineGetText() {
    }

    @Patch.OnExit(suppress = RuntimeException.class)
    public static void exit(
            @Patch.This RadioLine line,
            @Patch.Return String resolvedText) {
        LocalizationRuntime.rememberRenderedRadioLine(line, resolvedText);
    }
}
