package com.shorthand.common;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WordTextUtilsTest {
    @Test
    void parseMeaningLinesRequiresPosOnEveryLine() {
        List<WordTextUtils.ParsedSense> senses = WordTextUtils.parseMeaningLines(
                "n. 放任；狂热\nv. 遗弃；放弃");
        assertEquals(2, senses.size());
        assertEquals("n.", senses.get(0).pos());
        assertEquals("v.", senses.get(1).pos());
        assertEquals(WordTextUtils.meaningKey("n", "狂热；放任"), senses.get(0).meaningKey());
    }

    @Test
    void parseMeaningLinesRejectsMissingPos() {
        BizException error = assertThrows(BizException.class,
                () -> WordTextUtils.parseMeaningLines("没有词性的释义"));
        assertEquals("释义行缺少词性：没有词性的释义", error.getMessage());
    }

    @Test
    void headwordKeyKeepsCaseButNormalizedKeyDoesNot() {
        assertNotEquals(WordTextUtils.headwordKey("March"), WordTextUtils.headwordKey("march"));
        assertEquals(WordTextUtils.normalizeWord("March"), WordTextUtils.normalizeWord("march"));
    }

    @Test
    void meaningKeyIgnoresSynonymOrderAndPunctuation() {
        assertEquals(WordTextUtils.meaningKey("n", "放任；狂热"),
                WordTextUtils.meaningKey("n", "狂热；放任"));
    }

    @Test
    void pronunciationKeyFallsBackToUkAndMarksUnknown() {
        assertEquals("əˈbændən", WordTextUtils.pronunciationKey("", "/əˈbændən/"));
        assertEquals("unknown", WordTextUtils.pronunciationKey("", "[]"));
    }
}
