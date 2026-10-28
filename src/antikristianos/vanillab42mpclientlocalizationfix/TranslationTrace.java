package antikristianos.vanillab42mpclientlocalizationfix;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import zombie.core.Translator;

final class TranslationTrace {
    private static final int MAX_RECENT = 256;
    private static final ThreadLocal<ArrayDeque<Entry>> RECENT =
            ThreadLocal.withInitial(ArrayDeque::new);
    private static volatile ReverseIndex reverseIndex;

    private TranslationTrace() {
    }

    static void record(String key, Object[] arguments, String resolved) {
        if (key == null || resolved == null || resolved.isEmpty()) {
            return;
        }
        if (key.equals(resolved) && !isKnownTranslationKey(key)) {
            return;
        }
        ArrayDeque<Entry> recent = RECENT.get();
        List<LocalizedText> encodedArguments = new ArrayList<>();
        if (arguments != null) {
            for (Object argument : arguments) {
                String value = argument == null ? "" : String.valueOf(argument);
                LocalizedText nested = findExact(recent, value);
                if (nested == null) {
                    nested = find(value, false);
                }
                if (nested == null) {
                    nested = staticExact(value);
                }
                if (nested == null && key.startsWith("AEBS_") && value.startsWith("AEBS_")) {
                    // ISWeatherChannel deliberately passes some Vanilla AEBS fragment keys
                    // as arguments. Preserve that exact input as a nested translation instead
                    // of freezing the key text into the rendered client message.
                    nested = LocalizedText.translation(value, List.of());
                }
                encodedArguments.add(nested == null ? LocalizedText.literal(value) : nested);
            }
        }
        recent.addFirst(new Entry(
                resolved,
                LocalizedText.translation(key, encodedArguments)));
        while (recent.size() > MAX_RECENT) {
            recent.removeLast();
        }
    }

    static void recordItemName(String fullType, String resolved, boolean evolvedRecipeName) {
        if (fullType == null || resolved == null || resolved.isEmpty()) {
            return;
        }
        recordResolved(
                resolved,
                evolvedRecipeName
                        ? LocalizedText.evolvedItem(fullType)
                        : LocalizedText.item(fullType));
    }

    static void recordResolved(String resolved, LocalizedText text) {
        if (resolved == null || resolved.isEmpty() || text == null || !text.hasTranslation()) {
            return;
        }
        ArrayDeque<Entry> recent = RECENT.get();
        recent.addFirst(new Entry(resolved, text));
        while (recent.size() > MAX_RECENT) {
            recent.removeLast();
        }
    }

    static LocalizedText consume(String value, boolean includeStaticDictionary) {
        try {
            return find(value, includeStaticDictionary);
        } finally {
            RECENT.get().clear();
        }
    }

    static boolean hasRecentComponent(String value) {
        if (value == null) {
            return false;
        }
        for (Entry entry : RECENT.get()) {
            if (entry.resolved.length() >= 2 && value.contains(entry.resolved)) {
                return true;
            }
        }
        return false;
    }

    static void discard() {
        RECENT.get().clear();
    }

    static String renderForComparison(LocalizedText text) {
        ArrayDeque<Entry> pending = RECENT.get();
        RECENT.set(new ArrayDeque<>());
        try {
            return text.render();
        } finally {
            // A name comparison is not a new naming source. Preserve the caller's
            // pending setter provenance, even when rendering invokes patched getters.
            RECENT.set(pending);
        }
    }

    static LocalizedText find(String value, boolean includeStaticDictionary) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        ArrayDeque<Entry> recent = RECENT.get();
        LocalizedText exact = findExact(recent, value);
        if (exact != null) {
            return exact;
        }
        if (includeStaticDictionary) {
            exact = staticExact(value);
            if (exact != null) {
                return exact;
            }
        }

        Map<Character, List<Entry>> candidates = new HashMap<>();
        Set<String> seen = new HashSet<>();
        for (Entry entry : recent) {
            addCandidate(candidates, seen, entry);
        }
        if (includeStaticDictionary) {
            ReverseIndex index = index();
            for (Map.Entry<Character, List<Entry>> bucket : index.byFirstCharacter.entrySet()) {
                List<Entry> target = candidates.computeIfAbsent(bucket.getKey(), ignored -> new ArrayList<>());
                for (Entry entry : bucket.getValue()) {
                    if (seen.add(entry.resolved)) {
                        target.add(entry);
                    }
                }
            }
        }
        for (List<Entry> bucket : candidates.values()) {
            bucket.sort(Comparator.comparingInt((Entry entry) -> entry.resolved.length()).reversed());
        }

        List<LocalizedText> parts = new ArrayList<>();
        StringBuilder literal = new StringBuilder();
        boolean translated = false;
        int offset = 0;
        while (offset < value.length()) {
            Entry match = null;
            List<Entry> bucket = candidates.get(value.charAt(offset));
            if (bucket != null) {
                for (Entry candidate : bucket) {
                    if (value.startsWith(candidate.resolved, offset)) {
                        match = candidate;
                        break;
                    }
                }
            }
            if (match == null) {
                literal.append(value.charAt(offset++));
                continue;
            }
            if (literal.length() > 0) {
                parts.add(LocalizedText.literal(literal.toString()));
                literal.setLength(0);
            }
            parts.add(match.text);
            translated = true;
            offset += match.resolved.length();
        }
        if (literal.length() > 0) {
            parts.add(LocalizedText.literal(literal.toString()));
        }
        if (!translated) {
            return null;
        }
        return parts.size() == 1 ? parts.get(0) : LocalizedText.composite(parts);
    }

    private static void addCandidate(
            Map<Character, List<Entry>> candidates,
            Set<String> seen,
            Entry entry) {
        if (entry.resolved.length() < 2 || !seen.add(entry.resolved)) {
            return;
        }
        candidates.computeIfAbsent(entry.resolved.charAt(0), ignored -> new ArrayList<>()).add(entry);
    }

    private static LocalizedText findExact(ArrayDeque<Entry> recent, String value) {
        for (Entry entry : recent) {
            if (entry.resolved.equals(value)) {
                return entry.text;
            }
        }
        return null;
    }

    private static LocalizedText staticExact(String value) {
        Entry entry = index().byResolved.get(value);
        return entry == null ? null : entry.text;
    }

    private static boolean isKnownTranslationKey(String key) {
        for (Map<String, String> translations : Translator.BY_NAME.values()) {
            if (translations.containsKey(key)) {
                return true;
            }
        }
        return false;
    }

    private static ReverseIndex index() {
        ReverseIndex current = reverseIndex;
        Object language = Translator.language;
        int size = Translator.BY_NAME.values().stream().mapToInt(Map::size).sum();
        if (current != null && current.language == language && current.sourceSize == size) {
            return current;
        }
        synchronized (TranslationTrace.class) {
            current = reverseIndex;
            if (current != null && current.language == language && current.sourceSize == size) {
                return current;
            }
            Map<String, Entry> unique = new HashMap<>();
            Set<String> ambiguous = new HashSet<>();
            for (Map<String, String> translations : Translator.BY_NAME.values()) {
                for (Map.Entry<String, String> translation : translations.entrySet()) {
                    String key = translation.getKey();
                    String resolved = translation.getValue();
                    if (key == null || resolved == null || resolved.isEmpty()
                            || resolved.matches(".*%[1-9].*")) {
                        continue;
                    }
                    Entry entry = new Entry(
                            resolved,
                            LocalizedText.translation(key, List.of()));
                    Entry previous = unique.putIfAbsent(resolved, entry);
                    if (previous != null && !key.equals(previous.text.key())) {
                        ambiguous.add(resolved);
                    }
                }
            }
            for (String duplicate : ambiguous) {
                unique.remove(duplicate);
            }
            Map<Character, List<Entry>> buckets = new HashMap<>();
            for (Entry entry : unique.values()) {
                // Static reverse matches are intentionally conservative inside composites.
                // Exact matches still support short translations, while short words are not
                // allowed to rewrite coincidental literal fragments.
                if (entry.resolved.length() >= 4) {
                    buckets.computeIfAbsent(entry.resolved.charAt(0), ignored -> new ArrayList<>()).add(entry);
                }
            }
            for (List<Entry> bucket : buckets.values()) {
                bucket.sort(Comparator.comparingInt((Entry entry) -> entry.resolved.length()).reversed());
            }
            current = new ReverseIndex(language, size, Map.copyOf(unique), Map.copyOf(buckets));
            reverseIndex = current;
            return current;
        }
    }

    private static final class Entry {
        private final String resolved;
        private final LocalizedText text;

        private Entry(String resolved, LocalizedText text) {
            this.resolved = resolved;
            this.text = text;
        }
    }

    private static final class ReverseIndex {
        private final Object language;
        private final int sourceSize;
        private final Map<String, Entry> byResolved;
        private final Map<Character, List<Entry>> byFirstCharacter;

        private ReverseIndex(
                Object language,
                int sourceSize,
                Map<String, Entry> byResolved,
                Map<Character, List<Entry>> byFirstCharacter) {
            this.language = language;
            this.sourceSize = sourceSize;
            this.byResolved = byResolved;
            this.byFirstCharacter = byFirstCharacter;
        }
    }
}
