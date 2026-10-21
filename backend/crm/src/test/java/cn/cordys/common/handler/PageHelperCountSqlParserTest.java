package cn.cordys.common.handler;

import cn.cordys.crm.TestApplication;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = TestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class PageHelperCountSqlParserTest {
    private final PageHelperCountSqlParser parser = new PageHelperCountSqlParser();

    @Resource
    private JdbcTemplate jdbc;

    @ParameterizedTest
    @ValueSource(strings = {
            "SELECT name FROM (SELECT 'a' name UNION ALL SELECT 'b') rows_to_count ORDER BY name",
            "SELECT DISTINCT name FROM (SELECT 'a' name UNION ALL SELECT 'a') rows_to_count",
            "SELECT name, COUNT(*) FROM (SELECT 'a' name UNION ALL SELECT 'a') rows_to_count GROUP BY name",
            "SELECT 'a' name UNION ALL SELECT 'b' name ORDER BY name",
            "WITH rows_to_count AS (SELECT 'a' name UNION ALL SELECT 'b') SELECT * FROM rows_to_count ORDER BY name",
            "/*+ MAX_EXECUTION_TIME(1000) */ SELECT 'a' name"
    })
    void optimizedCountMatchesActualMysqlResults(String sql) {
        long actualRows = jdbc.queryForList(sql).size();
        assertEquals(actualRows, jdbc.queryForObject(parser.getSmartCountSql(sql, "0"), Long.class));
    }

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
