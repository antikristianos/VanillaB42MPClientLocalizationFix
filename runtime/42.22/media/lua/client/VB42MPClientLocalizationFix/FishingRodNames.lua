if not isClient() then return end

-- Register after Vanilla: its commands update both the name and fishing_Lure.
require "ServerCommands"

local function VB42LOC_onFishingCommand(module, command, args)
    if not isClient() or module ~= "fishing" then return end
    if command ~= "attachLure" and command ~= "removeLure" and command ~= "consumeLure" then
        return
    end
    if not args or not args.rodId then return end
    if not VB42Localization or not VB42Localization.Runtime then return end

    -- Match Vanilla's recipient and inventory lookup for these three commands.
    local player
    if args.playerOnlineId ~= nil then
        -- 42.21 addresses a specific online player, including local split-screen.
        -- An unknown recipient must never fall back to another player's rod.
        player = getPlayerByOnlineID and getPlayerByOnlineID(args.playerOnlineId)
    else
        -- 42.20 commands carry no online player ID.
        player = getPlayer()
    end
    local inventory = player and player:getInventory()
    local rod = inventory and inventory:getItemWithID(args.rodId)
    if rod then
        VB42Localization.Runtime.applyFishingRodName(rod)
    end
end

Events.OnServerCommand.Add(VB42LOC_onFishingCommand)
