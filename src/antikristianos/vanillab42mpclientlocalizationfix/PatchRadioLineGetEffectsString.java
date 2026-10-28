package antikristianos.vanillab42mpclientlocalizationfix;

import me.zed_0xff.zombie_buddy.Patch;

@Patch(className = "zombie.radio.scripting.RadioLine", methodName = "getEffectsString",
        warmUp = true, strictMatch = true)
public final class PatchRadioLineGetEffectsString {
    private PatchRadioLineGetEffectsString() {
    }

    @Patch.OnExit
    public static void exit(@Patch.Return(readOnly = false) String result) {
        result = MediaRecipeRuntime.normalizeCodes(result);
    }
}
