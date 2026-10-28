package antikristianos.vanillab42mpclientlocalizationfix;

import java.util.ArrayList;
import java.util.List;

final class AebsTextCodec {
    private static final int MAX_DEPTH = 16;
    private static final int MAX_NODES = 128;
    private static final int MAX_CHILDREN = 32;
    private static final int MAX_KEY_LENGTH = 256;
    private static final int MAX_LITERAL_LENGTH = 4096;
    private static final int MAX_MESSAGE_LENGTH = 16384;

    private AebsTextCodec() {
    }

    static String encode(LocalizedText text) {
        if (text == null || !text.isAebsWireTree()) {
            return null;
        }
        StringBuilder result = new StringBuilder();
        writeNode(result, text, 0, new int[] {0});
        if (result.length() > MAX_MESSAGE_LENGTH) {
            throw new IllegalArgumentException("AEBS message exceeds limit");
        }
        return result.toString();
    }

    static boolean isEncoded(String value) {
        return decode(value) != null;
    }

    static LocalizedText decode(String value) {
        if (value == null || value.length() > MAX_MESSAGE_LENGTH
                || value.isEmpty() || (value.charAt(0) != '[' && value.charAt(0) != '{')) {
            return null;
        }
        try {
            Cursor cursor = new Cursor(value, 0);
            LocalizedText text = readNode(cursor, 0, new int[] {0});
            if (cursor.offset != value.length() || !text.isAebsWireTree()) {
                return null;
            }
            return text;
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static void writeNode(
            StringBuilder target,
            LocalizedText text,
            int depth,
            int[] count) {
        validateNode(text, depth, count);
        if (text.kind() == LocalizedText.Kind.LITERAL) {
            target.append('\'');
            writeValue(target, text.value());
            return;
        }
        if (text.kind() == LocalizedText.Kind.TRANSLATION) {
            target.append('{');
            writeValue(target, text.value());
        } else {
            target.append('[');
        }
        target.append(text.children().size()).append(':');
        for (LocalizedText child : text.children()) {
            writeNode(target, child, depth + 1, count);
        }
    }

    private static LocalizedText readNode(Cursor cursor, int depth, int[] count) {
        if (depth > MAX_DEPTH || ++count[0] > MAX_NODES) {
            throw new IllegalArgumentException("AEBS tree exceeds limits");
        }
        char kind = cursor.next();
        if (kind == '\'') {
            String literal = cursor.readValue(MAX_LITERAL_LENGTH);
            return LocalizedText.literal(literal);
        }
        String key = kind == '{' ? cursor.readValue(MAX_KEY_LENGTH) : "";
        if (kind != '{' && kind != '[') {
            throw new IllegalArgumentException("Unknown AEBS node kind");
        }
        int childCount = cursor.readNumber();
        if (childCount > MAX_CHILDREN || (kind == '[' && childCount == 0)) {
            throw new IllegalArgumentException("Invalid AEBS child count");
        }
        List<LocalizedText> children = new ArrayList<>(childCount);
        for (int index = 0; index < childCount; index++) {
            children.add(readNode(cursor, depth + 1, count));
        }
        if (kind == '{') {
            if (!key.startsWith("AEBS_")) {
                throw new IllegalArgumentException("Non-AEBS translation key");
            }
            return LocalizedText.translation(key, children);
        }
        return LocalizedText.composite(children);
    }

    private static void validateNode(LocalizedText text, int depth, int[] count) {
        if (depth > MAX_DEPTH || ++count[0] > MAX_NODES
                || text.children().size() > MAX_CHILDREN) {
            throw new IllegalArgumentException("AEBS tree exceeds limits");
        }
        if (text.kind() == LocalizedText.Kind.LITERAL) {
            if (text.value().length() > MAX_LITERAL_LENGTH || !text.children().isEmpty()) {
                throw new IllegalArgumentException("Invalid AEBS literal");
            }
        } else if (text.kind() == LocalizedText.Kind.TRANSLATION) {
            if (!text.value().startsWith("AEBS_") || text.value().length() > MAX_KEY_LENGTH) {
                throw new IllegalArgumentException("Invalid AEBS translation key");
            }
        } else if (text.kind() != LocalizedText.Kind.COMPOSITE || text.children().isEmpty()) {
            throw new IllegalArgumentException("Invalid AEBS node");
        }
    }

    private static void writeValue(StringBuilder target, String value) {
        target.append(value.length()).append(':').append(value);
    }

    private static final class Cursor {
        private final String value;
        private int offset;

        private Cursor(String value, int offset) {
            this.value = value;
            this.offset = offset;
        }

        private char next() {
            if (offset >= value.length()) {
                throw new IllegalArgumentException("Unexpected end of AEBS message");
            }
            return value.charAt(offset++);
        }

        private int readNumber() {
            int delimiter = value.indexOf(':', offset);
            if (delimiter < 0 || delimiter == offset || delimiter - offset > 5) {
                throw new IllegalArgumentException("Invalid AEBS length");
            }
            int number = Integer.parseInt(value.substring(offset, delimiter));
            if (number < 0) {
                throw new IllegalArgumentException("Negative AEBS length");
            }
            offset = delimiter + 1;
            return number;
        }

        private String readValue(int maximumLength) {
            int length = readNumber();
            if (length > maximumLength || offset + length > value.length()) {
                throw new IllegalArgumentException("AEBS value exceeds bounds");
            }
            String result = value.substring(offset, offset + length);
            offset += length;
            return result;
        }
    }
}
