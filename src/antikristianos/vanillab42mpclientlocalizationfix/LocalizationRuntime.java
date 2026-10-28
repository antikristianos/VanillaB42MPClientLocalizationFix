package antikristianos.vanillab42mpclientlocalizationfix;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;
import zombie.core.Translator;
import zombie.debug.DebugLog;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.Food;
import zombie.iso.weather.ClimateManager;
import zombie.iso.IsoObject;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.radio.ZomboidRadio;
import zombie.radio.scripting.RadioLine;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.EvolvedRecipe;
import zombie.scripting.objects.Item;
import zombie.scripting.objects.ItemTag;
import zombie.scripting.objects.ItemType;

public final class LocalizationRuntime {
    private static final Field ITEM_PACKET_IS_FOOD = field(
            "zombie.network.packets.ItemStatsPacket", "isFood");
    private static final Field ITEM_PACKET_NAME = field(
            "zombie.network.packets.ItemStatsPacket", "name");
    private static final Field INVENTORY_ITEM_NAME = field(
            "zombie.inventory.InventoryItem", "name");
    private static final Field INVENTORY_ITEM_ORIGINAL_NAME = field(
            "zombie.inventory.InventoryItem", "originalName");
    private static final Field ISO_OBJECT_NAME = field("zombie.iso.IsoObject", "name");
    private static final Field HALO_PACKET_TEXT = field(
            "zombie.network.packets.HaloTextPacket", "text");
    private static final Field ADMIN_PACKET_MESSAGE = field(
            "zombie.network.packets.MessageForAdminPacket", "message");
    private static final Field WAVE_PACKET_MESSAGE = field(
            "zombie.network.packets.WaveSignalPacket", "msg");
    private static final Field WAVE_PACKET_SIGNAL_STRENGTH = field(
            "zombie.network.packets.WaveSignalPacket", "signalStrength");
    private static final Field RADIO_INTERFERENCE_APPLIED = field(
            "zombie.radio.ZomboidRadio", "hasAppliedInterference");

    private static final ThreadLocal<RenderedRadioLine> LAST_RENDERED_RADIO_LINE = new ThreadLocal<>();
    private static final Map<RadioLine, LocalizedText> RADIO_LINE_TEXTS =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<InventoryItem, CachedItemName> ITEM_NAME_STATES =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final String OBJECT_NAME_STATE = "VB42MPClientLocalizationFix.nameState";
    private static final String ITEM_NAME_STATE_KEY =
            "VB42MPClientLocalizationFix.nameState";
    private static final String ITEM_TOOLTIP_STATE_KEY =
            "VB42MPClientLocalizationFix.tooltipState";
    private static volatile boolean broadcastLocalizationLogged;

    private LocalizationRuntime() {
    }

    public static String recordTranslation(String key, Object[] arguments, String resolvedText) {
        if (!isMultiplayerRuntime() || resolvedText == null) {
            return resolvedText;
        }
        TranslationTrace.record(key, arguments, resolvedText);
        return resolvedText;
    }

    public static void encodeItemPacket(Object packet, Object[] arguments) {
        if (!isMultiplayerRuntime() || arguments == null || arguments.length != 2
                || !(arguments[1] instanceof Food food)) {
            return;
        }
        try {
            ITEM_PACKET_NAME.set(packet, getOriginalItemName(food));
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Cannot neutralize ItemStatsPacket name", exception);
        }
    }

    /** Save provenance before Vanilla applies the neutral transport name through setName. */
    public static Object captureItemPacketNameState(Object packet, InventoryItem item) {
        if (!isMultiplayerRuntime() || !(item instanceof Food)) {
            return null;
        }
        try {
            return ITEM_PACKET_IS_FOOD.getBoolean(packet)
                    ? LocalizedTextState.read(item.getModData().rawget(ITEM_NAME_STATE_KEY))
                    : null;
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Cannot preserve ItemStatsPacket name state", exception);
        }
    }

    public static void decodeItemPacket(Object packet, InventoryItem item, Object preservedNameState) {
        if (!isMultiplayerRuntime() || !(item instanceof Food)) {
            return;
        }
        try {
            if (!ITEM_PACKET_IS_FOOD.getBoolean(packet)) {
                return;
            }
            if (preservedNameState instanceof LocalizedText state) {
                // A neutral transport name is not a newly authored name. The packet
                // carries no replacement provenance. Evolved recipes are rebuilt
                // below from the newly applied ingredients instead of a snapshot.
                rememberItemNameState(item, state, true);
            }
            resetItemToLanguageNeutralName(item);
            rememberCurrentItemNameState(item);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Cannot apply language-neutral ItemStatsPacket name", exception);
        }
    }

    public static void decodeSerializedItemName(InventoryItem item) {
        if (!isMultiplayerRuntime() || item == null) {
            return;
        }
        ITEM_NAME_STATES.remove(item);
        try {
            LocalizedText state = rebuildEvolvedRecipeState(item);
            if (state == null) {
                state = LocalizedTextState.read(item.getModData().rawget(ITEM_NAME_STATE_KEY));
            }
            if (state != null) {
                rememberItemNameState(item, state, rebuildEvolvedRecipeState(item) == null);
                resetItemToLanguageNeutralName(item);
            } else if (item instanceof Food && !item.isCustomName()) {
                // An untracked generated food name is not an authoritative translation.
                // Match the neutral stats-packet result immediately at full-item reception.
                resetItemToLanguageNeutralName(item);
            }
        } catch (RuntimeException exception) {
            throw new IllegalStateException("Cannot restore language-neutral item state", exception);
        }
    }

    public static void rememberItemName(InventoryItem item, String value) {
        if (!isMultiplayerRuntime() || item == null) {
            return;
        }
        if (value == null) {
            clearRememberedItemName(item);
            TranslationTrace.discard();
            return;
        }

        LocalizedText state = item.isCustomName()
                ? encodeItemTextOrNull(item, value, false)
                : findItemNameState(item, value, false);
        if (state == null) {
            clearRememberedItemName(item);
        } else {
            rememberItemNameState(item, state, rebuildEvolvedRecipeState(item) == null);
            resetItemToLanguageNeutralName(item);
        }
    }

    public static void updateItemCustomName(InventoryItem item, boolean customName) {
        if (isMultiplayerRuntime() && item != null && customName) {
            LocalizedText remembered = rememberedItemNameState(item);
            String currentName = getRawItemName(item);
            if (!Objects.equals(currentName, getOriginalItemName(item))
                    && !isMatchingItemState(currentName, remembered)) {
                clearRememberedItemName(item);
            }
            TranslationTrace.discard();
        }
    }

    public static String recordInventoryItemDisplayName(
            InventoryItem item,
            String resolvedText) {
        if (!isMultiplayerRuntime() || item == null || resolvedText == null) {
            return resolvedText;
        }
        String localized = localizeItemNameView(item, resolvedText);

        LocalizedText remembered = preferredItemNameState(item);
        if (remembered != null && GameClient.client) {
            TranslationTrace.recordResolved(localized, remembered);
        } else if (Objects.equals(getRawItemName(item), getOriginalItemName(item))) {
            TranslationTrace.recordItemName(item.getFullType(), localized, false);
        }
        return localized;
    }

    public static String localizeItemNameView(InventoryItem item, String resolvedText) {
        if (!isMultiplayerRuntime() || !GameClient.client || item == null || resolvedText == null) {
            return resolvedText;
        }
        return renderRememberedItemName(item, resolvedText);
    }

    public static String recordInventoryItemEvolvedRecipeName(
            InventoryItem item,
            String resolvedText) {
        if (!isMultiplayerRuntime()) {
            return resolvedText;
        }
        if (item != null && Objects.equals(item.getFullType(), resolvedText)) {
            resolvedText = Translator.getItemNameFromFullType(item.getFullType());
        }
        if (item != null && resolvedText != null && !resolvedText.isEmpty()) {
            TranslationTrace.recordItemName(item.getFullType(), resolvedText, true);
        }
        return resolvedText;
    }

    public static String recordScriptItemDisplayName(Item item, String resolvedText) {
        if (isMultiplayerRuntime() && item != null && resolvedText != null) {
            TranslationTrace.recordItemName(item.getFullName(), resolvedText, false);
        }
        return resolvedText;
    }

    public static String recordEvolvedRecipeLookup(String fullType, String resolvedText) {
        if (!isMultiplayerRuntime()) {
            return resolvedText;
        }
        if (fullType != null && fullType.indexOf('.') > 0
                && Objects.equals(fullType, resolvedText)) {
            resolvedText = Translator.getItemNameFromFullType(fullType);
        }
        if (resolvedText != null) {
            TranslationTrace.recordItemName(fullType, resolvedText, true);
        }
        return resolvedText;
    }

    /** Called only after Vanilla's client fishing attach/remove/consume command. */
    public static boolean applyFishingRodName(InventoryItem item) {
        if (!GameClient.client || item == null) {
            return false;
        }
        String fullType = item.getFullType();
        if (fullType == null || fullType.indexOf('.') <= 0) {
            return false;
        }
        LocalizedText state = LocalizedText.item(fullType);
        Object lure = item.getModData().rawget("fishing_Lure");
        if (lure != null) {
            if (!(lure instanceof String lureType) || lureType.indexOf('.') <= 0) {
                return false;
            }
            state = LocalizedText.composite(List.of(
                    state, LocalizedText.literal(" "),
                    LocalizedText.translation("UI_AttachLure_With", List.of()),
                    LocalizedText.literal(" "), LocalizedText.item(lureType)));
        }
        rememberItemNameState(item, state, true);
        resetItemToLanguageNeutralName(item);
        return true;
    }

    public static boolean applyEvolvedRecipeName(InventoryItem item) {
        if (!isMultiplayerRuntime() || !(item instanceof Food food) || item.isCustomName()) {
            return false;
        }
        List<EvolvedRecipeNameBuilder.Ingredient> extraItems = recipeIngredients(
                item.getExtraItems(), false);
        List<EvolvedRecipeNameBuilder.Ingredient> spices = recipeIngredients(
                food.getSpices(), true);
        if (extraItems.isEmpty() && spices.isEmpty()) {
            return false;
        }

        String recipeUntranslatedName = resolveEvolvedRecipeName(item);
        if (recipeUntranslatedName == null) {
            return false;
        }

        EvolvedRecipeNameBuilder.Result result = EvolvedRecipeNameBuilder.build(
                item.getType(), recipeUntranslatedName, extraItems, spices);
        item.getModData().rawset(ITEM_NAME_STATE_KEY, null);
        resetItemToLanguageNeutralName(item);
        ITEM_NAME_STATES.put(item, new CachedItemName(null, result.text()));
        TranslationTrace.discard();
        return true;
    }

    public static void captureRadioLine(RadioLine line) {
        if (!GameServer.server || line == null) {
            return;
        }
        String resolved = line.getText();
        if (!TranslationTrace.hasRecentComponent(resolved)) {
            TranslationTrace.discard();
            return;
        }
        LocalizedText text = TranslationTrace.consume(resolved, true);
        if (text != null) {
            RADIO_LINE_TEXTS.put(line, text);
        }
    }

    public static void rememberRenderedRadioLine(RadioLine line, String resolvedText) {
        if (!GameServer.server) {
            return;
        }
        LocalizedText text = RADIO_LINE_TEXTS.get(line);
        if (text != null) {
            LAST_RENDERED_RADIO_LINE.set(new RenderedRadioLine(text, resolvedText));
        }
    }

    public static String encodeServerTransmission(String message) {
        RenderedRadioLine rendered = LAST_RENDERED_RADIO_LINE.get();
        LAST_RENDERED_RADIO_LINE.remove();
        if (!GameServer.server || rendered == null || message == null) {
            return message;
        }
        if (!Objects.equals(rendered.resolvedText, message)) {
            return message;
        }
        String vanillaKey = rendered.text.plainRadioKey();
        if (vanillaKey != null) {
            return vanillaKey;
        }
        String encoded = AebsTextCodec.encode(rendered.text);
        return encoded == null ? message : encoded;
    }

    public static String encodeWeatherInterference(Object radio, String message, int signalStrength) {
        if (!GameServer.server
                || (!isPlainRadioKey(message) && !AebsTextCodec.isEncoded(message))) {
            return null;
        }

        int interference = Math.max(0, Math.min(100,
                (int) (ClimateManager.getInstance().getWeatherInterference() * 100.0F)));
        if (interference > 0) {
            try {
                RADIO_INTERFERENCE_APPLIED.setBoolean(radio, true);
            } catch (IllegalAccessException exception) {
                throw new IllegalStateException("Cannot preserve radio interference state", exception);
            }
        }
        return message;
    }

    public static String decodeRadioTransmission(String message, int signalStrength) {
        if (message == null) {
            return message;
        }
        LocalizedText structured = AebsTextCodec.decode(message);
        if (structured != null) {
            return applyClientRadioInterference(structured.render(), signalStrength);
        }
        if (!isPlainRadioKey(message)) {
            return message;
        }
        String localizedText = Translator.getTextOrNull(message);
        if (localizedText == null) {
            return message;
        }

        return applyClientRadioInterference(localizedText, signalStrength);
    }

    public static String decodeBroadcastDisplayText(String message) {
        if (!GameClient.client) {
            return message;
        }
        String localized = decodeRadioTransmission(message, -1);
        if (!Objects.equals(message, localized)) {
            logBroadcastLocalizationOnce();
        }
        return localized;
    }

    public static void decodeWaveSignalPacket(Object packet) {
        if (GameServer.server || packet == null) {
            return;
        }
        try {
            String message = (String) WAVE_PACKET_MESSAGE.get(packet);
            int signalStrength = WAVE_PACKET_SIGNAL_STRENGTH.getInt(packet);
            String localized = decodeRadioTransmission(message, signalStrength);
            if (!Objects.equals(message, localized)) {
                WAVE_PACKET_MESSAGE.set(packet, localized);
                logBroadcastLocalizationOnce();
            }
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Cannot localize WaveSignalPacket", exception);
        }
    }

    private static boolean isPlainRadioKey(String message) {
        return message != null && (message.startsWith("RD_") || message.startsWith("AEBS_"));
    }

    private static String applyClientRadioInterference(String message, int signalStrength) {
        int interference = Math.max(0, Math.min(100,
                (int) (ClimateManager.getInstance().getWeatherInterference() * 100.0F)));
        return interference == 0
                ? message
                : ZomboidRadio.getInstance().scrambleString(
                        message, interference, signalStrength == -1, null);
    }

    private static void logBroadcastLocalizationOnce() {
        if (!broadcastLocalizationLogged) {
            broadcastLocalizationLogged = true;
            DebugLog.log("[VB42LOC] Client broadcast-key localization is active.");
        }
    }

    public static void encodeHaloPacket(Object packet) {
        encodePacketField(packet, HALO_PACKET_TEXT, true, true);
    }

    public static void decodeHaloPacket(Object packet) {
        decodePacketField(packet, HALO_PACKET_TEXT);
    }

    public static void encodeAdminPacket(Object packet) {
        encodePacketField(packet, ADMIN_PACKET_MESSAGE, false, true);
    }

    public static void decodeAdminPacket(Object packet) {
        decodePacketField(packet, ADMIN_PACKET_MESSAGE);
    }

    private static void encodePacketField(
            Object packet,
            Field field,
            boolean includeStaticDictionary,
            boolean requireRecentComponent) {
        if (!GameServer.server) {
            return;
        }
        try {
            String value = (String) field.get(packet);
            if (requireRecentComponent && !TranslationTrace.hasRecentComponent(value)) {
                TranslationTrace.discard();
                return;
            }
            LocalizedText text = TranslationTrace.consume(value, includeStaticDictionary);
            String key = text == null ? null : text.plainTranslationKey();
            if (key != null) {
                field.set(packet, key);
            }
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Cannot encode localized packet field", exception);
        }
    }

    private static void decodePacketField(Object packet, Field field) {
        try {
            String value = (String) field.get(packet);
            String localized = Translator.getTextOrNull(value);
            if (localized != null) {
                field.set(packet, localized);
            }
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Cannot decode localized packet field", exception);
        }
    }

    public static String rememberItemTooltip(InventoryItem item, String value) {
        if (!GameServer.server || item == null) {
            return value;
        }
        LocalizedText state = TranslationTrace.consume(value, false);
        item.getModData().rawset(ITEM_TOOLTIP_STATE_KEY, LocalizedTextState.write(state));
        return state == null ? value : null;
    }

    public static String localizeItemTooltip(InventoryItem item, String value) {
        if (!isMultiplayerRuntime() || item == null) {
            return value;
        }
        LocalizedText state = LocalizedTextState.read(
                item.getModData().rawget(ITEM_TOOLTIP_STATE_KEY));
        return state == null ? value : state.render();
    }

    public static void rememberObjectName(IsoObject object, String value) {
        if (!GameServer.server || object == null) {
            return;
        }
        LocalizedText state = TranslationTrace.consume(value, false);
        object.getModData().rawset(OBJECT_NAME_STATE, LocalizedTextState.write(state));
        if (state != null) {
            try {
                ISO_OBJECT_NAME.set(object, null);
            } catch (IllegalAccessException exception) {
                throw new IllegalStateException("Cannot neutralize object name", exception);
            }
        }
    }

    public static String localizeObjectName(IsoObject object, String value) {
        if (!isMultiplayerRuntime() || object == null) {
            return value;
        }
        LocalizedText state = LocalizedTextState.read(
                object.getModData().rawget(OBJECT_NAME_STATE));
        return state == null ? value : state.render();
    }

    private static LocalizedText findItemNameState(
            InventoryItem item,
            String currentName,
            boolean includeStaticDictionary) {
        if (item == null || currentName == null) {
            return null;
        }

        if (item.isCustomName()) {
            LocalizedText remembered = rememberedItemNameState(item);
            if (remembered == null) {
                remembered = rebuildEvolvedRecipeState(item);
            }
            if (Objects.equals(currentName, getOriginalItemName(item))
                    && remembered != null) {
                TranslationTrace.discard();
                return remembered;
            }
            if (isMatchingItemState(currentName, remembered)) {
                TranslationTrace.discard();
                return remembered;
            }
            clearRememberedItemName(item);
            TranslationTrace.discard();
            return null;
        }

        LocalizedText rebuilt = rebuildEvolvedRecipeState(item);
        if (rebuilt != null) {
            rememberItemNameState(item, rebuilt, false);
            return rebuilt;
        }
        LocalizedText remembered = rememberedItemNameState(item);
        if (isMatchingItemState(currentName, remembered)) {
            TranslationTrace.discard();
            return remembered;
        }

        String fullType = item.getFullType();
        String localBaseName = Translator.getItemNameFromFullType(fullType);
        if (fullType != null && fullType.indexOf('.') > 0
                && (Objects.equals(currentName, getOriginalItemName(item))
                        || Objects.equals(currentName, localBaseName))) {
            TranslationTrace.discard();
            LocalizedText baseState = LocalizedText.item(fullType);
            return baseState;
        }
        clearRememberedItemName(item);

        LocalizedText state = encodeItemTextOrNull(item, currentName, includeStaticDictionary);
        if (state != null) {
            rememberItemNameState(item, state, true);
        }
        return state;
    }

    private static boolean isMatchingItemState(
            String currentName,
            LocalizedText state) {
        if (state == null) {
            return false;
        }
        String localized = TranslationTrace.renderForComparison(state);
        return Objects.equals(localized, currentName);
    }

    private static void rememberItemNameState(
            InventoryItem item,
            LocalizedText state,
            boolean persist) {
        if (item != null && state != null) {
            state = completeItemNameState(item, state);
            Object source = persist ? LocalizedTextState.write(state) : null;
            item.getModData().rawset(ITEM_NAME_STATE_KEY, source);
            ITEM_NAME_STATES.put(item, new CachedItemName(source, state));
        }
        TranslationTrace.discard();
    }

    private static void rememberCurrentItemNameState(InventoryItem item) {
        LocalizedText state = rebuildEvolvedRecipeState(item);
        if (state == null) {
            state = LocalizedTextState.read(item.getModData().rawget(ITEM_NAME_STATE_KEY));
        }
        if (state != null) {
            rememberItemNameState(item, state, rebuildEvolvedRecipeState(item) == null);
        }
    }

    private static LocalizedText rebuildEvolvedRecipeState(InventoryItem item) {
        if (!(item instanceof Food food) || item.isCustomName()) {
            return null;
        }
        List<EvolvedRecipeNameBuilder.Ingredient> extraItems = recipeIngredients(
                item.getExtraItems(), false);
        List<EvolvedRecipeNameBuilder.Ingredient> spices = recipeIngredients(
                food.getSpices(), true);
        if (extraItems.isEmpty() && spices.isEmpty()) {
            return null;
        }

        String recipeName = resolveEvolvedRecipeName(item);
        if (recipeName == null) {
            return null;
        }
        return EvolvedRecipeNameBuilder.build(
                item.getType(), recipeName, extraItems, spices).text();
    }

    private record CachedItemName(Object source, LocalizedText text) {
    }

    private static LocalizedText rememberedItemNameState(InventoryItem item) {
        if (item == null) {
            return null;
        }
        Object source = item.getModData().rawget(ITEM_NAME_STATE_KEY);
        CachedItemName cached = ITEM_NAME_STATES.get(item);
        if (cached != null && cached.source() == source) {
            return cached.text();
        }
        // Full item loads and modData packets replace the stored name table.
        // A cached base/dynamic name must never hide that newer authoritative state.
        LocalizedText stored = LocalizedTextState.read(source);
        if (stored == null) {
            ITEM_NAME_STATES.remove(item);
            return null;
        }
        LocalizedText completed = completeItemNameState(item, stored);
        if (completed != stored) {
            source = LocalizedTextState.write(completed);
            item.getModData().rawset(ITEM_NAME_STATE_KEY, source);
        }
        ITEM_NAME_STATES.put(item, new CachedItemName(source, completed));
        return completed;
    }

    private static LocalizedText preferredItemNameState(InventoryItem item) {
        LocalizedText rebuilt = rebuildEvolvedRecipeState(item);
        if (rebuilt != null) {
            rememberItemNameState(item, rebuilt, false);
            return rebuilt;
        }
        return rememberedItemNameState(item);
    }

    private static String renderRememberedItemName(InventoryItem item, String resolvedText) {
        LocalizedText state = preferredItemNameState(item);
        if (state == null) {
            if (!(item instanceof Food) || item.isCustomName()) {
                return resolvedText;
            }
            String fullType = item.getFullType();
            if (fullType == null || fullType.indexOf('.') <= 0) {
                return resolvedText;
            }
            // Already loaded legacy food must have the same view before and after
            // any later stats/full-item packet. Do not guess or invent missing suffixes.
            return replaceRawItemNameView(resolvedText, getRawItemName(item),
                    Translator.getItemNameFromFullType(fullType));
        }
        String localized = state.render();
        String rawName = getRawItemName(item);
        return replaceRawItemNameView(resolvedText, rawName, localized);
    }

    private static String replaceRawItemNameView(
            String resolvedText,
            String rawName,
            String localizedName) {
        if (Objects.equals(resolvedText, rawName)) {
            return localizedName;
        }
        if (rawName == null || rawName.isEmpty()) {
            return resolvedText;
        }
        int offset = resolvedText.indexOf(rawName);
        if (offset < 0) {
            return resolvedText;
        }
        return resolvedText.substring(0, offset)
                + localizedName
                + resolvedText.substring(offset + rawName.length());
    }

    private static void clearRememberedItemName(InventoryItem item) {
        if (item == null) {
            return;
        }
        ITEM_NAME_STATES.remove(item);
        item.getModData().rawset(ITEM_NAME_STATE_KEY, null);
    }

    private static void resetItemToLanguageNeutralName(InventoryItem item) {
        try {
            INVENTORY_ITEM_NAME.set(item, getOriginalItemName(item));
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Cannot reset language-neutral item name", exception);
        }
    }

    private static String getOriginalItemName(InventoryItem item) {
        try {
            return (String) INVENTORY_ITEM_ORIGINAL_NAME.get(item);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Cannot read original item name", exception);
        }
    }

    private static String getRawItemName(InventoryItem item) {
        try {
            return (String) INVENTORY_ITEM_NAME.get(item);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Cannot read raw item name", exception);
        }
    }

    private static boolean isMultiplayerRuntime() {
        return GameServer.server || GameClient.client;
    }

    private static LocalizedText encodeItemTextOrNull(
            InventoryItem item,
            String value,
            boolean includeStaticDictionary) {
        LocalizedText text = TranslationTrace.consume(value, includeStaticDictionary);
        if (text == null) {
            return null;
        }
        Map<String, LocalizedText> origins = itemIngredientOrigins(item);
        if (!origins.isEmpty()) {
            text = text.rewriteLiterals(origins);
        }
        return completeItemNameState(item, text);
    }

    private static LocalizedText completeItemNameState(InventoryItem item, LocalizedText text) {
        if (item instanceof Food) {
            text = WildFoodNameState.complete(text, item.getFullType());
        }
        Item scriptItem = item.getScriptItem();
        return FishNameState.complete(text, item.getFullType(), getOriginalItemName(item),
                scriptItem == null ? null : scriptItem.getDisplayName());
    }

    private static Map<String, LocalizedText> itemIngredientOrigins(InventoryItem item) {
        if (item == null) {
            return Map.of();
        }
        Map<String, ItemNameOrigin> origins = new HashMap<>();
        Set<String> ambiguous = new HashSet<>();
        if (item.getExtraItems() != null) {
            for (String fullType : item.getExtraItems()) {
                addItemNameOrigins(origins, ambiguous, fullType);
            }
        }
        if (item instanceof Food food && food.getSpices() != null) {
            for (String fullType : food.getSpices()) {
                addItemNameOrigins(origins, ambiguous, fullType);
            }
        }
        Map<String, LocalizedText> result = new HashMap<>();
        for (Map.Entry<String, ItemNameOrigin> entry : origins.entrySet()) {
            if (!ambiguous.contains(entry.getKey())) {
                ItemNameOrigin origin = entry.getValue();
                result.put(entry.getKey(), origin.evolvedRecipeName
                        ? LocalizedText.evolvedItem(origin.fullType)
                        : LocalizedText.item(origin.fullType));
            }
        }
        if (item.getExtraItems() != null) {
            for (String fullType : item.getExtraItems()) {
                if (fullType != null && fullType.indexOf('.') > 0) {
                    result.put(fullType, LocalizedText.evolvedItem(fullType));
                }
            }
        }
        if (item instanceof Food food && food.getSpices() != null) {
            for (String fullType : food.getSpices()) {
                if (fullType != null && fullType.indexOf('.') > 0) {
                    result.put(fullType, LocalizedText.evolvedItem(fullType));
                }
            }
        }
        return result;
    }

    private static List<EvolvedRecipeNameBuilder.Ingredient> recipeIngredients(
            List<String> fullTypes,
            boolean spices) {
        if (fullTypes == null || fullTypes.isEmpty()) {
            return List.of();
        }
        List<EvolvedRecipeNameBuilder.Ingredient> result = new ArrayList<>();
        for (String fullType : fullTypes) {
            if (fullType == null) {
                continue;
            }
            Item scriptItem = ScriptManager.instance.getItem(fullType);
            if (scriptItem == null || scriptItem.getItemType() != ItemType.FOOD) {
                continue;
            }
            result.add(new EvolvedRecipeNameBuilder.Ingredient(
                    fullType,
                    scriptItem.foodType,
                    spices && scriptItem.hasTag(ItemTag.MINOR_INGREDIENT)));
        }
        return result;
    }

    private static String resolveEvolvedRecipeName(InventoryItem item) {
        if (item == null) {
            return null;
        }
        String selectedName = null;
        String selectedDisplayName = null;
        for (EvolvedRecipe recipe : ScriptManager.instance.getAllEvolvedRecipesList()) {
            if (!isMatchingEvolvedRecipeResult(recipe, item)) {
                continue;
            }
            String candidateName = recipe.getUntranslatedName();
            if (candidateName == null || candidateName.isBlank()) {
                continue;
            }
            String candidateDisplayName = Translator.getTextOrNull(
                    "ContextMenu_EvolvedRecipe_" + candidateName);
            if (candidateDisplayName == null) {
                continue;
            }
            if (selectedName == null) {
                selectedName = candidateName;
                selectedDisplayName = candidateDisplayName;
            } else if (!Objects.equals(selectedName, candidateName)
                    && !Objects.equals(selectedDisplayName, candidateDisplayName)) {
                return null;
            }
        }
        return selectedName;
    }

    private static boolean isMatchingEvolvedRecipeResult(
            EvolvedRecipe recipe,
            InventoryItem item) {
        if (recipe == null || item == null) {
            return false;
        }
        String resultItem = recipe.getFullResultItem();
        if (resultItem == null || resultItem.isBlank()) {
            return false;
        }
        if (resultItem.indexOf('.') >= 0) {
            return Objects.equals(resultItem, item.getFullType());
        }
        return Objects.equals(resultItem, item.getType())
                && recipe.getModule() != null
                && Objects.equals(recipe.getModule().name, item.getModule());
    }

    private static void addItemNameOrigins(
            Map<String, ItemNameOrigin> origins,
            Set<String> ambiguous,
            String fullType) {
        if (fullType == null || fullType.indexOf('.') <= 0) {
            return;
        }
        String evolvedName = Translator.getItemEvolvedRecipeName(fullType);
        addItemNameOrigin(origins, ambiguous, evolvedName,
                new ItemNameOrigin(fullType, true));
        String displayName = Translator.getItemNameFromFullType(fullType);
        addItemNameOrigin(origins, ambiguous, displayName,
                new ItemNameOrigin(fullType, false));
    }

    private static void addItemNameOrigin(
            Map<String, ItemNameOrigin> origins,
            Set<String> ambiguous,
            String resolved,
            ItemNameOrigin origin) {
        if (resolved == null || resolved.isEmpty() || ambiguous.contains(resolved)) {
            return;
        }
        ItemNameOrigin previous = origins.putIfAbsent(resolved, origin);
        if (previous == null || previous.fullType.equals(origin.fullType)) {
            return;
        }
        origins.remove(resolved);
        ambiguous.add(resolved);
    }

    private static Field field(String className, String fieldName) {
        try {
            Field result = Class.forName(className).getDeclaredField(fieldName);
            result.setAccessible(true);
            return result;
        } catch (ReflectiveOperationException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private static final class RenderedRadioLine {
        private final LocalizedText text;
        private final String resolvedText;

        private RenderedRadioLine(LocalizedText text, String resolvedText) {
            this.text = text;
            this.resolvedText = resolvedText;
        }
    }

    private static final class ItemNameOrigin {
        private final String fullType;
        private final boolean evolvedRecipeName;

        private ItemNameOrigin(String fullType, boolean evolvedRecipeName) {
            this.fullType = fullType;
            this.evolvedRecipeName = evolvedRecipeName;
        }
    }
}
