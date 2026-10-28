package antikristianos.vanillab42mpclientlocalizationfix;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import se.krka.kahlua.vm.KahluaTable;
import zombie.core.Translator;
import zombie.inventory.InventoryItem;
import zombie.scripting.objects.Item;

/** Rebuild only the variant already selected by Vanilla, never run an OnCreate again. */
final class LiteratureNameState {
    private static final Map<InventoryItem, Cached> CACHE =
            Collections.synchronizedMap(new WeakHashMap<>());

    private LiteratureNameState() {
    }

    static LocalizedText rebuild(InventoryItem item) {
        Item script = item.getScriptItem();
        if (item.isCustomName() || script == null) {
            return null;
        }
        String callback = script.getLuaCreate();
        if (callback == null || !callback.startsWith("ItemCodeOnCreate.")) {
            return null;
        }
        Object printMedia = item.getModData().rawget("printMedia");
        Object title = printMedia instanceof KahluaTable table ? table.rawget("title") : null;
        Object literature = item.getModData().rawget("literatureTitle");
        Object collectible = item.getModData().rawget("collectibleKey");
        int dictionaries = dictionarySize("IG_UI") + dictionarySize("Print_Media")
                + dictionarySize("Sandbox");
        Cached cached = CACHE.get(item);
        if (cached != null && Objects.equals(callback, cached.callback())
                && cached.language() == Translator.language
                && dictionaries == cached.dictionaries()
                && Objects.equals(title, cached.title())
                && Objects.equals(literature, cached.literature())
                && Objects.equals(collectible, cached.collectible())) {
            return cached.text();
        }
        LocalizedText text = build(item, callback.substring("ItemCodeOnCreate.".length()),
                title, literature, collectible);
        CACHE.put(item, new Cached(callback, title, literature, collectible,
                Translator.language, dictionaries, text));
        return text;
    }

    private static LocalizedText build(InventoryItem item, String callback,
            Object title, Object literature, Object collectible) {
        LocalizedText base = LocalizedText.item(item.getFullType());
        if (title instanceof String key && key.startsWith("Print_Media_")
                && key.endsWith("_title")) {
            return switch (callback) {
                case "onCreateFlier", "onCreateFlierNolan", "onCreateBrochure" ->
                        translated("IGUI_Newspaper_Name", base, translated(key));
                case "onCreateRecentNewspaper", "onCreateDispatchNewNewspaper",
                        "onCreateHeraldNewNewspaper", "onCreateKnewsNewNewspaper",
                        "onCreateTimesNewNewspaper" -> translated(key);
                default -> null;
            };
        }
        if (!(literature instanceof String identity)) {
            return null;
        }
        if (callback.equals("onCreateSubjectBook") && identity.startsWith("IGUI_BookTitle_")) {
            return translated("IGUI_MagazineNameNoIssue", base, translated(identity));
        }
        if (callback.equals("onCreateCatalogue") || callback.equals("onCreateRpgManual")) {
            String prefix = item.getType() + "_";
            if (identity.startsWith(prefix)) {
                String key = identity.substring(prefix.length());
                if (key.startsWith("IGUI_")) {
                    return translated("IGUI_MagazineNameNoIssue", base, translated(key));
                }
            }
        }
        if (callback.equals("onCreateSubjectMagazine") || callback.equals("onCreateTVMagazine")
                || callback.equals("onCreateHottieZ")) {
            int yearSeparator = identity.lastIndexOf('_');
            int monthSeparator = identity.lastIndexOf('_', yearSeparator - 1);
            if (monthSeparator <= 0 || yearSeparator <= monthSeparator) {
                return null;
            }
            String key = identity.substring(0, monthSeparator);
            String month = identity.substring(monthSeparator + 1, yearSeparator);
            String year = identity.substring(yearSeparator + 1);
            if (!month.matches("[1-9]|1[0-2]") || !year.matches("[0-9]{4}")) {
                return null;
            }
            LocalizedText name;
            if (callback.equals("onCreateSubjectMagazine") && key.startsWith("IGUI_MagazineTitle_")) {
                name = translated(key);
            } else if ((callback.equals("onCreateTVMagazine") && key.equals("TVMagazine"))
                    || (callback.equals("onCreateHottieZ") && key.equals("HottieZ"))) {
                name = base;
            } else if (callback.equals("onCreateHottieZ") && key.equals("HunkZ")) {
                name = translated("IGUI_MagazineTitle_HunkZ");
            } else {
                return null;
            }
            return translated("IGUI_MagazineName", name,
                    translated("Sandbox_StartMonth_option" + month), LocalizedText.literal(year));
        }
        if (List.of("onCreatePhoto", "onCreateSecretPhoto", "onCreateRacyPhoto",
                "onCreateVeryOldPhoto", "onCreateDoodle", "onCreateDoodleKids").contains(callback)
                && collectible instanceof String key && key.startsWith("IGUI_")
                && identity.matches("[^\\r\\n]*_" + java.util.regex.Pattern.quote(key) + "_[0-9]+")) {
            return translated("IGUI_PhotoOf", base, translated(key));
        }
        // Unknown or incomplete metadata is not permission to infer a variant from its name.
        return null;
    }

    private static LocalizedText translated(String key, LocalizedText... arguments) {
        String dictionary = key.startsWith("Print_Media_") ? "Print_Media"
                : key.startsWith("Sandbox_") ? "Sandbox" : "IG_UI";
        Map<String, String> translations = Translator.BY_NAME.get(dictionary);
        if (translations == null || !translations.containsKey(key)) {
            return null;
        }
        for (LocalizedText argument : arguments) {
            if (argument == null) {
                return null;
            }
        }
        return LocalizedText.translation(key, List.of(arguments));
    }

    private static int dictionarySize(String name) {
        Map<String, String> dictionary = Translator.BY_NAME.get(name);
        return dictionary == null ? 0 : dictionary.size();
    }

    private record Cached(String callback, Object title, Object literature,
            Object collectible, Object language, int dictionaries, LocalizedText text) {
    }
}
