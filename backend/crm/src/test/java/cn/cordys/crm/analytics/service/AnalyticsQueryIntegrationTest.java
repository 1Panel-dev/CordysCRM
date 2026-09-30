package cn.cordys.crm.analytics.service;

import cn.cordys.common.constants.InternalUser;
import cn.cordys.common.resolver.field.NumberResolver;
import cn.cordys.crm.analytics.dto.request.AnalyticsQueryRequest.*;
import cn.cordys.crm.analytics.mapper.AnalyticsQueryMapper;
import cn.cordys.crm.base.BaseTest;
import cn.cordys.crm.system.dto.field.DatasourceMultipleField;
import cn.cordys.crm.system.dto.field.InputNumberField;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static cn.cordys.crm.analytics.service.AnalyticsServiceTest.*;
import static org.junit.jupiter.api.Assertions.*;

/** 在原有 MySQL/Redis 集成环境验证真实迁移表结构；每个用例的合成数据随事务回滚。 */
@Transactional
class AnalyticsQueryIntegrationTest extends BaseTest {
    private static final String ORG = "analytics-test-org";
    @Resource private AnalyticsAccessService accessService;
    @Resource private AnalyticsQueryMapper queryMapper;
    @Resource private DataSource dataSource;

    @Test
    void everyModuleAggregatesAgainstItsRealListAndFieldTable() {
        try (var permission = AnalyticsAccessServiceTest.permitted()) {
            for (String formKey : AnalyticsAccessService.SOURCES.keySet()) {
                var access = accessService.resolve(formKey, View.ALL, null, InternalUser.ADMIN.getValue(), ORG);
                var request = request();
                request.setFormKey(formKey);
                var rows = queryMapper.query(AnalyticsSqlBuilder.compile(access, catalog(), request));
                assertEquals(1, rows.size(), formKey);
                assertNumber("0", rows.getFirst().get("m0"));
                assertNull(rows.getFirst().get("m1"), formKey);
                assertNumber("0", rows.getFirst().get("_invalid_total"));
            }
        }
    }

    @Test
    void databaseComputesAllMetricsBeforeSelectingTopNAndExcludesOtherOrganizations() {
        seed();
        var request = request();
        request.setFormKey("product");
        // 默认月份分组不依赖数据库命名时区表；缺失时区表曾导致模型改为逐月反复查询。
        assertEquals("+08:00", request.getTimezone());
        request.setDimensions(List.of(dimension("createTime", Granularity.MONTH)));
        request.setMetrics(List.of(metric(Aggregate.COUNT, null, "count"), metric(Aggregate.SUM, "amount-field", "amount"),
                metric(Aggregate.AVG, "amount-field", "average"), metric(Aggregate.COUNT, "amount-field", "filled")));
        request.setLimit(1);
        var order = new OrderBy();
        order.setMetric("amount");
        request.setOrderBy(order);
        try (var permission = AnalyticsAccessServiceTest.permitted()) {
            var access = accessService.resolve("product", View.ALL, null, InternalUser.ADMIN.getValue(), ORG);
            var rows = queryMapper.query(AnalyticsSqlBuilder.compile(access, catalog(), request));
            assertEquals(1, rows.size());
            assertEquals("2026-10", rows.getFirst().get("d0"));
            assertNumber("2", rows.getFirst().get("m0"));
            assertNumber("30.31", rows.getFirst().get("m1"));
            assertNumber("30.31", rows.getFirst().get("m2"));
            assertNumber("1", rows.getFirst().get("m3"));
            assertNumber("2", rows.getFirst().get("_groups"));
            assertNumber("4", rows.getFirst().get("_matched"));

            var range = new TimeRange();
            range.setField("createTime");
            range.setFrom("2026-09-01");
            range.setToExclusive("2026-10-01");
            request.setTimeRange(range);
            var september = queryMapper.query(AnalyticsSqlBuilder.compile(access, catalog(), request)).getFirst();
            assertEquals("2026-09", september.get("d0"));
            assertNumber("30.30", september.get("m1"));
            assertNumber("15.15", september.get("m2"));
            assertNumber("2", september.get("_matched"));
        }
    }

    @Test
    void decimalPrecisionAndEmptyGroupedResultsSurviveTheDatabase() {
        JdbcTemplate jdbc = seed();
        String amount = "12345678901234567890.12345678901234567890";
        jdbc.update("update product_field set field_value = ? where id = ?", amount, "analytics-f1");
        try (var permission = AnalyticsAccessServiceTest.permitted()) {
            var access = accessService.resolve("product", View.ALL, null, InternalUser.ADMIN.getValue(), ORG);
            var request = request();
            assertNumber(new BigDecimal(amount).add(new BigDecimal("50.51")).toPlainString(),
                    queryMapper.query(AnalyticsSqlBuilder.compile(access, catalog(), request)).getFirst().get("m1"));
            var condition = new Condition();
            condition.setField("id");
            condition.setOperator(Operator.EQUALS);
            condition.setValue("no-such-record");
            request.setConditions(List.of(condition));
            var scalar = queryMapper.query(AnalyticsSqlBuilder.compile(access, catalog(), request)).getFirst();
            assertNumber("0", scalar.get("m0"));
            assertNull(scalar.get("m1"));
            request.setDimensions(List.of(dimension("id", Granularity.RAW)));
            var grouped = queryMapper.query(AnalyticsSqlBuilder.compile(access, catalog(), request)).getFirst();
            assertNumber("0", grouped.get("_groups"));
            assertNumber("0", grouped.get("_matched"));
            assertNull(grouped.get("_records"));
        }
    }

    @Test
    void duplicateScalarValuesAreRejectedAndMalformedNumbersAreFlagged() {
        JdbcTemplate jdbc = seed();
        try (var permission = AnalyticsAccessServiceTest.permitted()) {
            var access = accessService.resolve("product", View.ALL, null, InternalUser.ADMIN.getValue(), ORG);
            var plan = AnalyticsSqlBuilder.compile(access, catalog(), request());
            jdbc.update("update product_field set field_value = 'invalid' where id = ?", "analytics-f1");
            assertNumber("1", queryMapper.query(plan).getFirst().get("_invalid_total"));
            jdbc.update("update product_field set field_value = '10.10' where id = ?", "analytics-f1");
            jdbc.update("insert into product_field(id,resource_id,field_id,field_value) values (?,?,?,?)",
                    "analytics-duplicate", "analytics-p1", "amount-field", "100");
            assertThrows(DataAccessException.class, () -> queryMapper.query(plan));
        }
    }

    @Test
    void persistedScientificNumbersRemainExactAndOutOfRangeValuesFailClosed() {
        var jdbc = seed();
        var resolver = new NumberResolver();
        var field = field(new InputNumberField(), "amount-field", "INPUT_NUMBER", null);
        var source = scopedAccess("product", "no-hidden-product");
        var request = request();
        var plan = AnalyticsSqlBuilder.compile(source, catalog(), request);
        for (Number number : List.of(10000000.0, new BigDecimal("1E+7"), new BigDecimal("1E-4"))) {
            resolver.validate(field, number);
            jdbc.update("update product_field set field_value = ? where id = 'analytics-f1'", resolver.convertToString(field, number));
            var row = queryMapper.query(plan).getFirst();
            assertNumber(new BigDecimal(number.toString()).add(new BigDecimal("50.51")).toPlainString(), row.get("m1"));
            assertNumber("0", row.get("_invalid_total"));
        }
        for (String value : List.of("1e-20", "10e-21", "-1.25E+30", "0.1e45",
                "1.2345678901234567890123E+30", "12345678901234567890123456789012345678901234567890e-5")) {
            jdbc.update("update product_field set field_value = ? where id = 'analytics-f1'", value);
            assertNumber(new BigDecimal(value).add(new BigDecimal("50.51")).toPlainString(), queryMapper.query(plan).getFirst().get("m1"));
        }
        for (String value : List.of("1e-21", "1e45", "1e999", "1e+", "NaN")) {
            jdbc.update("update product_field set field_value = ? where id = 'analytics-f1'", value);
            assertNumber("1", queryMapper.query(plan).getFirst().get("_invalid_total"));
        }
    }

    @Test
    void multipleRelationConditionsMatchOnlyAuthorizedMembers() {
        var jdbc = seed();
        var relation = field(new DatasourceMultipleField(), "refs", "DATA_SOURCE_MULTIPLE", null);
        relation.setDataSourceType("PRODUCT");
        var catalog = AnalyticsSchemaService.catalog("product", List.of(relation), Map.of());
        jdbc.update("insert into product_field_blob(id,resource_id,field_id,field_value) values ('refs','analytics-p1','refs','[\"analytics-p1\",\"analytics-p2\"]')");
        var source = scopedAccess("product", "no-hidden-product");
        var related = scopedAccess("product", "analytics-p2");
        var request = request(); request.setFormKey("product"); request.setRelationField("refs");
        request.setMetrics(List.of(metric(Aggregate.COUNT, null, "count")));
        var condition = new Condition(); condition.setField("refs"); request.setConditions(List.of(condition));
        for (Operator operator : List.of(Operator.EQUALS, Operator.IN, Operator.NOT_EQUALS, Operator.NOT_IN, Operator.EMPTY, Operator.NOT_EMPTY)) {
            condition.setOperator(operator);
            condition.setValue(switch (operator) {
                case EMPTY, NOT_EMPTY -> null;
                case IN, NOT_IN -> List.of("analytics-p2");
                default -> "analytics-p2";
            });
            boolean matches = List.of(Operator.NOT_EQUALS, Operator.NOT_IN, Operator.NOT_EMPTY).contains(operator);
            var row = queryMapper.query(AnalyticsSqlBuilder.compile(source, catalog, request, related)).getFirst();
            assertEquals(matches ? 1 : 0, ((Number) row.get("m0")).intValue(), operator.name());
            request.setDimensions(List.of(dimension("refs", Granularity.RAW)));
            var group = queryMapper.query(AnalyticsSqlBuilder.compile(source, catalog, request, related)).getFirst();
            assertEquals(matches ? "analytics-p1" : null, group.get("d0"));
            assertNumber(matches ? "1" : "0", group.get("_groups"));
            request.setDimensions(List.of());
        }
        condition.setOperator(Operator.IN); condition.setValue(List.of("analytics-p1"));
        assertNumber("1", queryMapper.query(AnalyticsSqlBuilder.compile(source, catalog, request, related)).getFirst().get("m0"));
    }

    @Test
    void subtableRowsMultiValuesAndBothRelationScopesAggregateExactly() {
        var jdbc = seed();
        for (String id : List.of("analytics-order", "hidden-order", "foreign-order")) {
            jdbc.update("insert into sales_order(id,number,name,stage,approval_status,organization_id,create_time,update_time,create_user,update_user,amount) values (?,?,?,'PENDING','NONE',?,0,0,'test','test',999)",
                    id, id, "test", id.equals("foreign-order") ? "foreign-org" : ORG);
            for (int i = 1; i <= 2; i++) {
                jdbc.update("insert into sales_order_field(id,resource_id,field_id,field_value,ref_sub_id,row_id) values (?,?,?,?,'items',?)",
                        id + "p" + i, id, "productId", "analytics-p" + i, "" + i);
                jdbc.update("insert into sales_order_field(id,resource_id,field_id,field_value,ref_sub_id,row_id) values (?,?,?,?,'items',?)",
                        id + "a" + i, id, "line-amount", "" + (10 * i), "" + i);
            }
        }
        jdbc.update("insert into sales_order_field_blob(id,resource_id,field_id,field_value,ref_sub_id,row_id) values ('tag1','analytics-order','tags','[\"a\",\"a\",\"b\"]','items','1')");
        jdbc.update("insert into sales_order_field_blob(id,resource_id,field_id,field_value,ref_sub_id,row_id) values ('tag2','analytics-order','tags','[\"b\",\"c\"]','items','2')");
        jdbc.update("insert into sales_order_field_blob(id,resource_id,field_id,field_value,ref_sub_id,row_id) values ('blob-only','analytics-order','tags','[]','items','3')");
        jdbc.update("insert into sales_order_field(id,resource_id,field_id,field_value,ref_sub_id,row_id) values ('empty-line','analytics-order','id','row4','items','4')");
        var source = scopedAccess("order", "hidden-order");
        var related = scopedAccess("product", "analytics-p2");
        var catalog = lineCatalog().dataset("items");
        var request = request(); request.setFormKey("order"); request.setSubTableFieldId("items");
        request.setMetrics(List.of(metric(Aggregate.COUNT, null, "count"), metric(Aggregate.SUM, "line.line-amount", "amount")));
        var all = queryMapper.query(AnalyticsSqlBuilder.compile(source, catalog, request)).getFirst();
        assertNumber("4", all.get("m0")); assertNumber("30", all.get("m1"));
        assertNumber("1", all.get("_matched")); assertNumber("4", all.get("_matched_rows"));
        request.setRelationField("line.productId");
        var visible = queryMapper.query(AnalyticsSqlBuilder.compile(source, catalog, request, related)).getFirst();
        assertNumber("1", visible.get("m0")); assertNumber("10", visible.get("m1"));
        request.setRelationField(null);
        request.setDimensions(List.of(dimension("line.tags", Granularity.RAW)));
        var groups = queryMapper.query(AnalyticsSqlBuilder.compile(source, catalog, request));
        assertEquals(4, groups.size());
        var b = groups.stream().filter(row -> "b".equals(row.get("d0"))).findFirst().orElseThrow();
        assertNumber("2", b.get("m0")); assertNumber("30", b.get("m1"));
        assertNumber("4", b.get("_matched_rows"));
        var condition = new Condition(); condition.setField("line.tags"); condition.setOperator(Operator.NOT_IN); condition.setValue(List.of("b"));
        request.setConditions(List.of(condition));
        var emptyTags = queryMapper.query(AnalyticsSqlBuilder.compile(source, catalog, request));
        assertEquals(1, emptyTags.size()); assertNull(emptyTags.getFirst().get("d0"));
        assertNumber("2", emptyTags.getFirst().get("m0"));
        request.setConditions(List.of());
        jdbc.update("update sales_order_field_blob set field_value = 'invalid' where id = 'tag1'");
        assertNumber("1", queryMapper.query(AnalyticsSqlBuilder.compile(source, catalog, request)).getFirst().get("_invalid_total"));
    }

    private static AnalyticsAccessService.Access scopedAccess(String key, String hidden) {
        var config = new org.apache.ibatis.session.Configuration();
        var source = AnalyticsAccessService.SOURCES.get(key);
        var mappings = List.of(new org.apache.ibatis.mapping.ParameterMapping.Builder(config, "org", String.class).build(),
                new org.apache.ibatis.mapping.ParameterMapping.Builder(config, "hidden", String.class).build());
        var statement = new org.apache.ibatis.mapping.MappedStatement.Builder(config, "visible." + key,
                new org.apache.ibatis.builder.StaticSqlSource(config, "select id from `" + source.table()
                        + "` where organization_id = ? and id <> ?", mappings), org.apache.ibatis.mapping.SqlCommandType.SELECT).build();
        return new AnalyticsAccessService.Access(source, statement, statement.getBoundSql(Map.of("org", ORG, "hidden", hidden)),
                false, ORG, new cn.cordys.crm.analytics.dto.response.AnalyticsQueryResponse.Scope("ALL", "SELF", null, "test"));
    }

    private JdbcTemplate seed() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        for (int i = 1; i <= 5; i++) {
            jdbc.update("insert into product(id,name,price,status,organization_id,create_time,update_time,create_user,update_user,pos) values (?,?,0,'ON_SHELF',?,?,0,'test','test',0)",
                    "analytics-p" + i, "analytics-product", i == 5 ? "analytics-foreign-org" : ORG,
                    i <= 2 ? 1788192000000L : 1790784000000L);
            if (i != 4) jdbc.update("insert into product_field(id,resource_id,field_id,field_value) values (?,?,?,?)",
                    "analytics-f" + i, "analytics-p" + i, "amount-field", Map.of(1, "10.10", 2, "20.20", 3, "30.31", 5, "99999").get(i));
        }
        return jdbc;
    }

    private static void assertNumber(String expected, Object actual) {
        assertNotNull(actual);
        assertEquals(0, new BigDecimal(expected).compareTo(new BigDecimal(actual.toString())));
    }
}
