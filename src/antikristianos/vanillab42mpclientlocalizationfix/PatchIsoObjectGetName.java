package antikristianos.vanillab42mpclientlocalizationfix;

import me.zed_0xff.zombie_buddy.Patch;
import zombie.iso.IsoObject;

@Patch(className = "zombie.iso.IsoObject", methodName = "getName",
        warmUp = true, strictMatch = true)
public final class PatchIsoObjectGetName {
    private PatchIsoObjectGetName() {
    }

    @Patch.OnExit(suppress = RuntimeException.class)
    public static void exit(
            @Patch.This IsoObject object,
            @Patch.Return(readOnly = false) String value) {
        value = LocalizationRuntime.localizeObjectName(object, value);
    }
}
