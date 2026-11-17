package cn.cordys.common.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CodingUtilsTest {

    @ParameterizedTest
    @CsvSource({
            "abc, 900150983cd24fb0d6963f7d28e17f72",
            "中文, a7bac2239fcdcb3a067903d8077c4a07",
    })
    void md5PreservesLowercaseHexDigest(String input, String expected) {
        assertEquals(expected, CodingUtils.md5(input));
    }

    @Test
    void md5PreservesExplicitAndDefaultCharsets() {
        assertEquals("bcce109775e8e1972e9f5fcda3e12895", CodingUtils.md5("中文", "GBK"));
        assertEquals("a7bac2239fcdcb3a067903d8077c4a07", CodingUtils.md5("中文", null));
        assertEquals("a7bac2239fcdcb3a067903d8077c4a07", CodingUtils.md5("中文", " "));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { " ", "\t" })
    void md5StillRejectsBlankInput(String input) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> CodingUtils.md5(input));

        assertEquals("Input for MD5 cannot be null or empty", exception.getMessage());
    }

    @ParameterizedTest
    @CsvSource({
            "abc, ba7816bf8f01cfea",
            "中文, 72726d8818f69306",
            "'', e3b0c44298fc1c14",
    })
    void hashStrPreservesFirstSixteenSha256HexCharacters(String input, String expected) {
        assertEquals(expected, CodingUtils.hashStr(input));
    }

    @Test
    void hashStrStillRejectsNullInput() {
        assertThrows(NullPointerException.class, () -> CodingUtils.hashStr(null));
    }

    @Test
    void generatedSecretKeyRemainsThirtyTwoLowercaseHexCharacters() {
        assertTrue(CodingUtils.generateSecretKey().matches("[0-9a-f]{32}"));
    }
}
