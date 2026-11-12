package cn.cordys.common.handler;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PageHelperCountSqlParserTest {
    private final PageHelperCountSqlParser parser = new PageHelperCountSqlParser();

    @Test
    void countPreservesHintsParametersAndExplicitOrdering() {
        String optimized = parser.getSmartCountSql("/*+ test_hint */ SELECT name FROM customer ORDER BY name", "0");
        assertTrue(optimized.startsWith("/*+ test_hint */"));
        assertFalse(optimized.contains("ORDER BY"));
        assertTrue(parser.getSmartCountSql("SELECT name FROM customer ORDER BY ?", "0").contains("ORDER BY ?"));
        assertTrue(parser.getSmartCountSql("/*keep orderby*/ SELECT name FROM customer ORDER BY name", "0")
                .contains("ORDER BY name"));
    }
}
