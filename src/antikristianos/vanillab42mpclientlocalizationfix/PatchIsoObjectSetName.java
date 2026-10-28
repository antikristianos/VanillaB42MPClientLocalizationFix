package antikristianos.vanillab42mpclientlocalizationfix;

import me.zed_0xff.zombie_buddy.Patch;
import zombie.iso.IsoObject;

@Patch(className = "zombie.iso.IsoObject", methodName = "setName",
        warmUp = true, strictMatch = true)
public final class PatchIsoObjectSetName {
    private PatchIsoObjectSetName() {
    }

    @Patch.OnExit(suppress = RuntimeException.class)
    public static void exit(
            @Patch.This IsoObject object,
            @Patch.Argument(0) String value) {
        LocalizationRuntime.rememberObjectName(object, value);
    }
}
