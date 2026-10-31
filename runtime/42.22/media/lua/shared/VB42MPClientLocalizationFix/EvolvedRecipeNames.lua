require "TimedActions/ISAddItemInRecipe"

local function VB42LOC_applyEvolvedRecipeName(baseItem)
    if not VB42Localization or not VB42Localization.Runtime then
        return false
    end
    return VB42Localization.Runtime.applyEvolvedRecipeName(baseItem)
end

local VB42LOC_checkName

local function VB42LOC_isMultiplayer()
    return isClient() or isServer()
end

VB42LOC_checkName = function(baseItem, recipe)
    if baseItem and recipe and VB42LOC_applyEvolvedRecipeName(baseItem) then
        if not isServer() then
            ISInventoryPage.dirtyUI()
        end
        return
    end
end

local function VB42LOC_install()
    if not VB42LOC_isMultiplayer() then
        return
    end
    if ISAddItemInRecipe.checkName == VB42LOC_checkName then
        return
    end

    ISAddItemInRecipe.checkName = VB42LOC_checkName
end

VB42LOC_install()
Events.OnGameBoot.Add(VB42LOC_install)
Events.OnGameStart.Add(VB42LOC_install)
Events.OnServerStarted.Add(VB42LOC_install)
