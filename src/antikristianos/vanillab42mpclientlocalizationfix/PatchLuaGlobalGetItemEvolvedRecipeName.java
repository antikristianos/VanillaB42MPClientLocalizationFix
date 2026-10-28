package antikristianos.vanillab42mpclientlocalizationfix;

import me.zed_0xff.zombie_buddy.Patch;

@Patch(
        className = "zombie.Lua.LuaManager$GlobalObject",
        methodName = "getItemEvolvedRecipeName",
        warmUp = true,
        strictMatch = true)
public final class PatchLuaGlobalGetItemEvolvedRecipeName {
    private PatchLuaGlobalGetItemEvolvedRecipeName() {
    }

    @Patch.OnExit(suppress = RuntimeException.class)
    public static void exit(
            @Patch.Argument(0) String fullType,
            @Patch.Return(readOnly = false) String resolvedText) {
        resolvedText = LocalizationRuntime.recordEvolvedRecipeLookup(fullType, resolvedText);
    }
}
