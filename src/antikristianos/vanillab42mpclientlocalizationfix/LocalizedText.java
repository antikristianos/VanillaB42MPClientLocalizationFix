package antikristianos.vanillab42mpclientlocalizationfix;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import zombie.core.Translator;

final class LocalizedText {
    enum Kind {
        LITERAL,
        TRANSLATION,
        COMPOSITE,
        ITEM,
        EVOLVED_ITEM
    }

    private final Kind kind;
    private final String value;
    private final List<LocalizedText> children;

    private LocalizedText(Kind kind, String value, List<LocalizedText> children) {
        this.kind = kind;
        this.value = value;
        this.children = List.copyOf(children);
    }

    static LocalizedText literal(String value) {
        return new LocalizedText(Kind.LITERAL, value == null ? "" : value, List.of());
    }

    static LocalizedText translation(String key, List<LocalizedText> arguments) {
        return new LocalizedText(Kind.TRANSLATION, key, arguments);
    }

    static LocalizedText composite(List<LocalizedText> parts) {
        return new LocalizedText(Kind.COMPOSITE, "", parts);
    }

    static LocalizedText item(String fullType) {
        return new LocalizedText(Kind.ITEM, fullType, List.of());
    }

    static LocalizedText evolvedItem(String fullType) {
        return new LocalizedText(Kind.EVOLVED_ITEM, fullType, List.of());
    }

    Kind kind() {
        return kind;
    }

    String value() {
        return value;
    }

    List<LocalizedText> children() {
        return children;
    }

    String key() {
        return kind == Kind.TRANSLATION ? value : null;
    }

    String plainRadioKey() {
        String key = plainTranslationKey();
        return key != null && (key.startsWith("RD_") || key.startsWith("AEBS_"))
                ? value
                : null;
    }

    String plainTranslationKey() {
        return kind == Kind.TRANSLATION && children.isEmpty() ? value : null;
    }

    boolean isAebsWireTree() {
        boolean[] foundTranslation = {false};
        return containsOnlyAebsWireNodes(foundTranslation) && foundTranslation[0];
    }

    private boolean containsOnlyAebsWireNodes(boolean[] foundTranslation) {
        if (kind == Kind.ITEM || kind == Kind.EVOLVED_ITEM) {
            return false;
        }
        if (kind == Kind.TRANSLATION) {
            if (value == null || !value.startsWith("AEBS_")) {
                return false;
            }
            foundTranslation[0] = true;
        }
        for (LocalizedText child : children) {
            if (!child.containsOnlyAebsWireNodes(foundTranslation)) {
                return false;
            }
        }
        return true;
    }

    boolean hasTranslation() {
        if (kind == Kind.TRANSLATION || kind == Kind.ITEM || kind == Kind.EVOLVED_ITEM) {
            return true;
        }
        for (LocalizedText child : children) {
            if (child.hasTranslation()) {
                return true;
            }
        }
        return false;
    }

    boolean hasItemReference() {
        if (kind == Kind.ITEM || kind == Kind.EVOLVED_ITEM) {
            return true;
        }
        for (LocalizedText child : children) {
            if (child.hasItemReference()) {
                return true;
            }
        }
        return false;
    }

    LocalizedText rewriteLiterals(Map<String, LocalizedText> replacements) {
        if (replacements == null || replacements.isEmpty()) {
            return this;
        }
        if (kind == Kind.LITERAL) {
            return rewriteLiteral(value, replacements);
        }
        if (children.isEmpty()) {
            return this;
        }
        boolean changed = false;
        List<LocalizedText> rewritten = new ArrayList<>(children.size());
        for (LocalizedText child : children) {
            LocalizedText replacement = child.rewriteLiterals(replacements);
            rewritten.add(replacement);
            changed |= replacement != child;
        }
        return changed ? new LocalizedText(kind, value, rewritten) : this;
    }

    private static LocalizedText rewriteLiteral(
            String literal,
            Map<String, LocalizedText> replacements) {
        List<Map.Entry<String, LocalizedText>> candidates = replacements.entrySet().stream()
                .filter(entry -> entry.getKey() != null && !entry.getKey().isEmpty()
                        && entry.getValue() != null && entry.getValue().hasItemReference())
                .sorted(Comparator.comparingInt(
                        (Map.Entry<String, LocalizedText> entry) -> entry.getKey().length())
                        .reversed())
                .toList();
        if (candidates.isEmpty()) {
            return literal(literal);
        }

        List<LocalizedText> parts = new ArrayList<>();
        StringBuilder unchanged = new StringBuilder();
        boolean changed = false;
        int offset = 0;
        while (offset < literal.length()) {
            Map.Entry<String, LocalizedText> match = null;
            for (Map.Entry<String, LocalizedText> candidate : candidates) {
                if (literal.startsWith(candidate.getKey(), offset)) {
                    match = candidate;
                    break;
                }
            }
            if (match == null) {
                unchanged.append(literal.charAt(offset++));
                continue;
            }
            if (unchanged.length() > 0) {
                parts.add(literal(unchanged.toString()));
                unchanged.setLength(0);
            }
            parts.add(match.getValue());
            offset += match.getKey().length();
            changed = true;
        }
        if (!changed) {
            return literal(literal);
        }
        if (unchanged.length() > 0) {
            parts.add(literal(unchanged.toString()));
        }
        return parts.size() == 1 ? parts.get(0) : composite(parts);
    }

    String render() {
        if (kind == Kind.LITERAL) {
            return value;
        }
        if (kind == Kind.TRANSLATION) {
            Object[] arguments = new Object[children.size()];
            for (int index = 0; index < children.size(); index++) {
                arguments[index] = children.get(index).render();
            }
            return Translator.getText(value, arguments);
        }
        if (kind == Kind.ITEM) {
            return Translator.getItemNameFromFullType(value);
        }
        if (kind == Kind.EVOLVED_ITEM) {
            return selectEvolvedRecipeName(
                    value,
                    Translator.getItemEvolvedRecipeName(value),
                    Translator.getItemNameFromFullType(value));
        }
        StringBuilder result = new StringBuilder();
        for (LocalizedText child : children) {
            result.append(child.render());
        }
        return result.toString();
    }

    static String selectEvolvedRecipeName(
            String fullType,
            String evolvedRecipeName,
            String displayName) {
        if (evolvedRecipeName == null || evolvedRecipeName.isEmpty()
                || evolvedRecipeName.equals(fullType)) {
            return displayName;
        }
        return evolvedRecipeName;
    }
}
