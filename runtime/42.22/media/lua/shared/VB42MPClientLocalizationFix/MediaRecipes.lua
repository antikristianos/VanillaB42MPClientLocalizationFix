require "RadioCom/ISRadioInteractions"

local module = "VB42MPClientLocalizationFix"
local command = "MediaRecipeLearned"
local messageKey = "IGUI_HaloNote_LearnedRecipe"
local installed
local pending = {}
local warned = {}

local function warnOnce(key)
    if not warned[key] then
        warned[key] = true
        print("[VB42MPClientLocalizationFix] Media recipe unavailable: " .. key)
    end
end

local function validRecipe(key, runtime)
    if runtime.isRegisteredRecipeKey(key) then return true end
    -- These are Vanilla knowledge keys, not craft recipes. Keep internal spaces.
    if farming_vegetableconf and farming_vegetableconf.props then
        for _, props in pairs(farming_vegetableconf.props) do
            if props.seasonRecipe == key then return true end
        end
    end
    return false
end

local function eligible(player, x, y, z, line)
    if not player or line == nil or player:isDead() or player:isAsleep()
            or not player:getStats() or not player:getXp() then return false end
    if x == -1 and y == -1 and z == -1 then return true end
    if not ISRadioInteractions:getInstance().playerInRange(player, x, y, z) then return false end
    local source = getCell():getGridSquare(x, y, z)
    local square = player:getSquare()
    return not source or (square and source:isOutside() == square:isOutside())
end

local function install()
    if not isClient() and not isServer() then return end
    local interactions = ISRadioInteractions:getInstance()
    if interactions.checkPlayer == installed then return end
    local original = interactions.checkPlayer
    installed = function(player, guid, codes, x, y, z, line, source)
        if not isClient() and not isServer() then
            return original(player, guid, codes, x, y, z, line, source)
        end
        if not codes or not string.find(codes, "RCP", 1, true) then
            return original(player, guid, codes, x, y, z, line, source)
        end
        local runtime = VB42Localization and VB42Localization.Runtime
        if runtime then codes = runtime.normalizeMediaRecipeCodes(codes) end
        local effects, recipes = {}, {}
        for token in string.gmatch(codes, "[^,]+") do
            local trimmed = string.match(token, "^%s*(.-)%s*$")
            if string.sub(trimmed, 1, 3) == "RCP" then
                if string.sub(trimmed, 1, 4) == "RCP=" then
                    recipes[#recipes + 1] = string.sub(trimmed, 5)
                end
            else
                effects[#effects + 1] = token
            end
        end
        -- Vanilla retains ownership of XP, stats, cooldowns and seen-media history.
        original(player, guid, table.concat(effects, ","), x, y, z, line, source)
        if not isServer() or not eligible(player, x, y, z, line) then return end
        if not runtime then warnOnce("Java bridge missing") return end
        local learned = {}
        for _, key in ipairs(recipes) do
            if validRecipe(key, runtime) then
                -- Vanilla alone decides whether the recipe is already known.
                if player:learnRecipe(key) then learned[#learned + 1] = key end
            else
                warnOnce(key)
            end
        end
        if #learned > 0 then
            -- Includes any meta-recipe unlocks performed by Vanilla learnRecipe.
            sendSyncPlayerFields(player, 0x00000001)
            for _, key in ipairs(learned) do
                sendServerCommand(player, module, command, {
                    playerID = player:getOnlineID(), recipeID = key, messageKey = messageKey
                })
            end
        end
    end
    interactions.checkPlayer = installed
end

local function showPending()
    if not isClient() or #pending == 0 then return end
    for index = #pending, 1, -1 do
        local entry = pending[index]
        entry.ticks = entry.ticks - 1
        local target
        for slot = 0, 3 do
            local player = getSpecificPlayer(slot)
            if player and player:getOnlineID() == entry.playerID then target = player break end
        end
        -- This gates the notification on received server state; it never grants knowledge.
        if target and target:getKnownRecipes():contains(entry.recipeID) then
            local displayID = string.match(entry.recipeID, "^[^.]+%.(.+)$") or entry.recipeID
            HaloTextHelper.addGoodText(target, getText(messageKey, getRecipeDisplayName(displayID)))
            if ISInventoryPage then ISInventoryPage.dirtyUI() end
            table.remove(pending, index)
        elseif entry.ticks <= 0 then
            warnOnce("knowledge sync timeout: " .. entry.recipeID)
            table.remove(pending, index)
        end
    end
end

local function onServerCommand(receivedModule, receivedCommand, args)
    if not isClient() or receivedModule ~= module or receivedCommand ~= command then return end
    if not args or args.messageKey ~= messageKey or type(args.recipeID) ~= "string"
            or type(args.playerID) ~= "number" or #args.recipeID > 256 then return end
    if #pending >= 128 then warnOnce("notification queue full") return end
    pending[#pending + 1] = { playerID = args.playerID, recipeID = args.recipeID, ticks = 1800 }
    showPending()
end

install()
Events.OnGameBoot.Add(install)
Events.OnGameStart.Add(install)
Events.OnServerStarted.Add(install)
Events.OnServerCommand.Add(onServerCommand)
Events.OnTick.Add(showPending)
