package antikristianos.vanillab42mpclientlocalizationfix;

import me.zed_0xff.zombie_buddy.Patch;
import zombie.characters.IsoGameCharacter;
import zombie.scripting.entity.components.crafting.CraftRecipe;

@Patch(className = "zombie.characters.IsoGameCharacter", methodName = "isRecipeKnown",
        warmUp = true, strictMatch = true)
public final class PatchCharacterIsCraftRecipeKnown {
    private PatchCharacterIsCraftRecipeKnown() {
    }

    @Patch.OnEnter(skipOn = true)
    public static boolean enter(
            @Patch.This IsoGameCharacter player,
            @Patch.Argument(0) CraftRecipe recipe,
            @Patch.Argument(1) boolean actual,
            @Patch.Local("mediaRecipeHandled") boolean handled,
            @Patch.Local("mediaRecipeKnown") boolean known) {
        handled = MediaRecipeRuntime.isMultiplayer();
        if (handled) {
            known = MediaRecipeRuntime.isRecipeKnown(player, recipe, actual);
        }
        return handled;
    }

    @Patch.OnExit
    public static void exit(
            @Patch.Local("mediaRecipeHandled") boolean handled,
            @Patch.Local("mediaRecipeKnown") boolean known,
            @Patch.Return(readOnly = false) boolean result) {
        if (handled) {
            result = known;
        }
    }
}
