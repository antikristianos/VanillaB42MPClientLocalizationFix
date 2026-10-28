package antikristianos.vanillab42mpclientlocalizationfix;

import me.zed_0xff.zombie_buddy.Patch;

@Patch(className = "zombie.radio.media.MediaData$MediaLineData", methodName = "getCodes",
        warmUp = true, strictMatch = true)
public final class PatchMediaLineGetCodes {
    private PatchMediaLineGetCodes() {
    }

    @Patch.OnExit
    public static void exit(@Patch.Return(readOnly = false) String result) {
        result = MediaRecipeRuntime.normalizeCodes(result);
    }
}
