package antikristianos.vanillab42mpclientlocalizationfix;

import me.zed_0xff.zombie_buddy.Patch;
import zombie.scripting.objects.Item;

@Patch(className = "zombie.scripting.objects.Item", methodName = "getDisplayName",
        warmUp = true, strictMatch = true)
public final class PatchScriptItemGetDisplayName {
    private PatchScriptItemGetDisplayName() {
    }

    @Patch.OnExit(suppress = RuntimeException.class)
    public static void exit(
            @Patch.This Item item,
            @Patch.Return(readOnly = false) String resolvedText) {
        resolvedText = LocalizationRuntime.recordScriptItemDisplayName(item, resolvedText);
    }
}
