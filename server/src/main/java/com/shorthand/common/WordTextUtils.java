package com.shorthand.common;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 词书导入/展示共用的文本规则。
 *
 * 词性必须在每个释义行行首；同一行用分号分隔的只是同义词，不是多个义项。
 */
public final class WordTextUtils {
    private static final Pattern POS_LINE = Pattern.compile("^([A-Za-z][A-Za-z.\\-/]*?)\\.\\s*(.+)$");
    private static final Pattern SYNONYM_SPLIT = Pattern.compile("[;；]");
    private static final Pattern KEY_NOISE = Pattern.compile("[\\s\\p{Punct}，。；、！？：（）【】《》“”‘’·]+");

    private WordTextUtils() {
    }

    public record ParsedSense(String pos, String posKey, String meaning, String meaningKey) {
    }

    public static String normalizeWord(String raw) {
        if (raw == null) return "";
        return Normalizer.normalize(raw, Normalizer.Form.NFKC)
                .trim()
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);
    }

    /** 保留大小写，march 与 March 是两个 lexeme。 */
    public static String headwordKey(String raw) {
        if (raw == null) return "";
        return Normalizer.normalize(raw, Normalizer.Form.NFKC)
                .trim()
                .replaceAll("\\s+", " ");
    }

    /** 读音键优先美音；两个都空时使用 unknown。 */
    public static String pronunciationKey(String phoneticUs, String phoneticUk) {
        String value = hasText(phoneticUs) ? phoneticUs : phoneticUk;
        if (!hasText(value)) return "unknown";
        String key = Normalizer.normalize(value, Normalizer.Form.NFKC)
                .trim()
                .replace("/", "")
                .replace("[", "")
                .replace("]", "")
                .replaceAll("\\s+", "")
                .toLowerCase(Locale.ROOT);
        return hasText(key) ? key : "unknown";
    }

    public static List<ParsedSense> parseMeaningLines(String meaning) {
        if (!hasText(meaning)) throw new BizException("释义为空");
        List<ParsedSense> result = new ArrayList<>();
        for (String line : meaning.split("\\R")) {
            String text = line.trim();
            if (text.isEmpty()) continue;
            Matcher matcher = POS_LINE.matcher(text);
            if (!matcher.matches()) {
                throw new BizException("释义行缺少词性：" + text);
            }
            String pos = matcher.group(1).toLowerCase(Locale.ROOT) + ".";
            String body = matcher.group(2).trim();
            if (body.isEmpty()) throw new BizException("释义为空：" + text);
            String posKey = posKey(matcher.group(1));
            result.add(new ParsedSense(pos, posKey, body, meaningKey(posKey, body)));
        }
        if (result.isEmpty()) throw new BizException("释义为空");
        return result;
    }

    public static String posKey(String rawPos) {
        String key = rawPos == null ? "" : rawPos.toLowerCase(Locale.ROOT).replace(".", "").trim();
        return switch (key) {
            case "interjection", "interj", "int" -> "interj";
            case "vt", "vi" -> "v";
            default -> key;
        };
    }

    public static String meaningKey(String posKey, String meaning) {
        String[] parts = SYNONYM_SPLIT.split(meaning);
        String joined = Arrays.stream(parts)
                .map(WordTextUtils::normalizeSynonym)
                .filter(s -> !s.isEmpty())
                .distinct()
                .sorted()
                .reduce((a, b) -> a + "\u0001" + b)
                .orElse("");
        return sha256(posKey + "\u0000" + joined);
    }

    public static String entryHash(List<Long> senseIds) {
        return sha256(senseIds.stream().sorted().map(String::valueOf)
                .reduce((a, b) -> a + "," + b).orElse(""));
    }

    public static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    public static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static String normalizeSynonym(String value) {
        return KEY_NOISE.matcher(Normalizer.normalize(value, Normalizer.Form.NFKC)
                        .toLowerCase(Locale.ROOT))
                .replaceAll("");
    }
}
