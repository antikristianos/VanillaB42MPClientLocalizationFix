package antikristianos.vanillab42mpclientlocalizationfix;

import java.util.ArrayList;
import java.util.List;
import se.krka.kahlua.j2se.J2SEPlatform;
import se.krka.kahlua.vm.KahluaTable;
import zombie.Lua.LuaManager;

final class LocalizedTextState {
    private static final int MAX_DEPTH = 8;
    private static final int MAX_NODES = 128;
    private static final int MAX_VALUE_LENGTH = 16_384;

    private LocalizedTextState() {
    }

    static KahluaTable write(LocalizedText text) {
        if (text == null || !text.hasTranslation()) {
            return null;
        }
        return writeNode(text, 0, new int[] {0});
    }

    static LocalizedText read(Object state) {
        if (!(state instanceof KahluaTable table)) {
            return null;
        }
        try {
            LocalizedText text = readNode(table, 0, new int[] {0});
            return text != null && text.hasTranslation() ? text : null;
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static KahluaTable writeNode(LocalizedText text, int depth, int[] count) {
        validate(depth, count);
        KahluaTable table = newTable();
        table.rawset("kind", text.kind().name());
        table.rawset("value", text.value());
        if (!text.children().isEmpty()) {
            KahluaTable children = newTable();
            for (int index = 0; index < text.children().size(); index++) {
                children.rawset(index + 1, writeNode(text.children().get(index), depth + 1, count));
            }
            table.rawset("children", children);
        }
        return table;
    }

    private static LocalizedText readNode(KahluaTable table, int depth, int[] count) {
        validate(depth, count);
        Object kindValue = table.rawget("kind");
        Object textValue = table.rawget("value");
        if (!(kindValue instanceof String kindName) || !(textValue instanceof String value)
                || value.length() > MAX_VALUE_LENGTH) {
            throw new IllegalArgumentException("Invalid localized text state");
        }
        LocalizedText.Kind kind = LocalizedText.Kind.valueOf(kindName);
        List<LocalizedText> children = new ArrayList<>();
        Object childState = table.rawget("children");
        if (childState instanceof KahluaTable childTable) {
            int length = childTable.len();
            for (int index = 1; index <= length; index++) {
                Object child = childTable.rawget(index);
                if (!(child instanceof KahluaTable nested)) {
                    throw new IllegalArgumentException("Invalid localized child state");
                }
                children.add(readNode(nested, depth + 1, count));
            }
        }
        return switch (kind) {
            case LITERAL -> LocalizedText.literal(value);
            case TRANSLATION -> LocalizedText.translation(value, children);
            case COMPOSITE -> children.size() >= 2
                    ? LocalizedText.composite(children)
                    : throwInvalid();
            case ITEM -> children.isEmpty()
                    ? LocalizedText.item(value)
                    : throwInvalid();
            case EVOLVED_ITEM -> children.isEmpty()
                    ? LocalizedText.evolvedItem(value)
                    : throwInvalid();
        };
    }

    private static LocalizedText throwInvalid() {
        throw new IllegalArgumentException("Invalid localized text node");
    }

    private static KahluaTable newTable() {
        J2SEPlatform platform = LuaManager.platform;
        return (platform == null ? J2SEPlatform.getInstance() : platform).newTable();
    }

    private static void validate(int depth, int[] count) {
        if (depth > MAX_DEPTH || ++count[0] > MAX_NODES) {
            throw new IllegalArgumentException("Localized text state exceeds bounds");
        }
    }
}
