package cn.cordys.common.mapper;

import cn.cordys.crm.clue.mapper.ExtClueMapper;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 已转客户的线索仍参与字段唯一性校验，同时保留原有组织、字段和自身排除条件。
 */
class ClueUniqueSqlRenderTest {

    private static Configuration configuration;

    @BeforeAll
    static void loadMapperXml() throws Exception {
        configuration = new Configuration();
        for (Class<?> mapper : List.of(CommonMapper.class, ExtClueMapper.class)) {
            String resource = mapper.getName().replace('.', '/') + ".xml";
            try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource)) {
                assertNotNull(in, "找不到 Mapper XML：" + resource);
                new XMLMapperBuilder(in, configuration, resource, configuration.getSqlFragments()).parse();
            }
        }
    }

    static Stream<Arguments> commonUniqueQueries() {
        return Stream.of("clue", "customer").flatMap(table -> Stream.of(
                Arguments.of(table, "checkAddExist", """
                        select count(1) from `%1$s`
                        where `name` COLLATE utf8mb4_bin = ? and organization_id = ? limit 1
                        """.formatted(table), List.of("重复公司", "org-a")),
                Arguments.of(table, "checkUpdateExist", """
                        select count(1) from `%1$s`
                        where `name` COLLATE utf8mb4_bin = ? and organization_id = ?
                        and id not in ( ? , ? ) limit 1
                        """.formatted(table), List.of("重复公司", "org-a", "resource-a", "resource-b")),
                Arguments.of(table, "getCheckValList", """
                        select `name` as fieldValue, id as resourceId from `%1$s` t
                        where organization_id = ?
                        """.formatted(table), List.of("org-a")),
                Arguments.of(table, "checkFieldRepeatName", """
                        select d.name from `%1$s_field` f join `%1$s` d on f.resource_id = d.id
                        where f.field_id = ? and f.field_value = ? and d.organization_id = ? limit 1
                        """.formatted(table), List.of("company-field", "重复公司", "org-a")),
                Arguments.of(table, "getCheckFieldValList", """
                        select f.field_value, f.resource_id from `%1$s_field` f join `%1$s` d on f.resource_id = d.id
                        where f.field_id = ? and d.organization_id = ?
                        """.formatted(table), List.of("company-field", "org-a")),
                Arguments.of(table, "checkInternalRepeatName", """
                        select d.name from `%1$s` d
                        where d.`name` = ? and d.organization_id = ? limit 1
                        """.formatted(table), List.of("重复公司", "org-a"))));
    }

    @DisplayName("线索包含已转客户数据，其他表单保留原有唯一性校验")
    @ParameterizedTest(name = "{0}.{1}")
    @MethodSource("commonUniqueQueries")
    void commonUniqueQueriesKeepExistingConditions(String table, String statement, String expectedSql,
                                                   List<String> expectedValues) {
        Map<String, Object> parameters = Map.of(
                "tableName", table,
                "dataTable", table,
                "fieldTable", table + "_field",
                "fieldName", "name",
                "businessName", "name",
                "fieldId", "company-field",
                "fieldValue", "重复公司",
                "value", "重复公司",
                "orgId", "org-a",
                "excludeIds", List.of("resource-a", "resource-b"));

        assertSql(CommonMapper.class, statement, parameters, expectedSql, expectedValues);
    }

    @Test
    @DisplayName("线索自定义字段查重不按转换状态排除数据")
    void clueCustomFieldUniqueQueryIncludesConvertedClues() {
        assertSql(ExtClueMapper.class, "checkFieldValueRepeat",
                Map.of("field", Map.of("fieldId", "company-field", "fieldValue", "重复公司")), """
                        select count(1) > 0 from clue_field cf join clue c on cf.resource_id = c.id
                        where cf.field_id = ? and cf.field_value = ?
                        """, List.of("company-field", "重复公司"));
    }

    private static void assertSql(Class<?> mapper, String statement, Map<String, Object> parameters,
                                  String expectedSql, List<String> expectedValues) {
        BoundSql boundSql = configuration.getMappedStatement(mapper.getName() + "." + statement)
                .getBoundSql(parameters);
        assertEquals(normalize(expectedSql), normalize(boundSql.getSql()));
        List<Object> values = boundSql.getParameterMappings().stream().map(mapping -> {
            String property = mapping.getProperty();
            return boundSql.hasAdditionalParameter(property) ? boundSql.getAdditionalParameter(property)
                    : configuration.newMetaObject(parameters).getValue(property);
        }).toList();
        assertEquals(expectedValues, values, "字段值、组织和排除记录的绑定值应保持不变");
    }

    private static String normalize(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }
}
