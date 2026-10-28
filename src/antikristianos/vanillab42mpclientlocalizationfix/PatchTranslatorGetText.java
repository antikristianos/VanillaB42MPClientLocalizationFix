package antikristianos.vanillab42mpclientlocalizationfix;

import me.zed_0xff.zombie_buddy.Patch;

@Patch(
        className = "zombie.core.Translator",
        methodName = "getText",
        warmUp = true,
        strictMatch = true)
public final class PatchTranslatorGetText {
    private PatchTranslatorGetText() {
    }

    @Patch.OnExit(suppress = RuntimeException.class)
    public static void exit(
            @Patch.Argument(0) String key,
            @Patch.Argument(1) Object[] arguments,
            @Patch.Return(readOnly = false) String resolvedText) {
        resolvedText = LocalizationRuntime.recordTranslation(key, arguments, resolvedText);
    }
}
