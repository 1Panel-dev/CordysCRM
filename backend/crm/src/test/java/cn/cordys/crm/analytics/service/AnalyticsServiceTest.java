package cn.cordys.crm.analytics.service;

import cn.cordys.common.exception.GenericException;
import cn.cordys.crm.analytics.dto.request.AnalyticsQueryRequest;
import cn.cordys.crm.analytics.dto.request.AnalyticsQueryRequest.*;
import cn.cordys.crm.analytics.dto.response.AnalyticsQueryResponse.Scope;
import cn.cordys.crm.analytics.mapper.AnalyticsQueryMapper;
import cn.cordys.crm.system.dto.field.*;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.field.base.OptionProp;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.service.ModuleFormCacheService;
import cn.cordys.crm.system.service.ModuleFormService;
import cn.cordys.crm.opportunity.service.OpportunityService;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.builder.StaticSqlSource;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AnalyticsServiceTest {
    private static final ValidatorFactory VALIDATION = Validation.buildDefaultValidatorFactory();
    private final AnalyticsService service = new AnalyticsService();
    private final AnalyticsAccessService accessService = mock(AnalyticsAccessService.class);
    private final AnalyticsSchemaService schemaService = mock(AnalyticsSchemaService.class);
    private final AnalyticsQueryMapper mapper = mock(AnalyticsQueryMapper.class);
    private final ModuleFormService forms = mock(ModuleFormService.class);
    private final AnalyticsSchemaService.Catalog catalog = catalog();
    private final AnalyticsAccessService.Access access = access("customer", false);

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(service, "accessService", accessService);
        ReflectionTestUtils.setField(service, "schemaService", schemaService);
        ReflectionTestUtils.setField(service, "queryMapper", mapper);
        ReflectionTestUtils.setField(service, "moduleFormService", forms);
        ReflectionTestUtils.setField(service, "validator", VALIDATION.getValidator());
        when(accessService.resolve(anyString(), any(), any(), anyString(), anyString())).thenReturn(access);
        when(schemaService.load(anyString(), any(), anyString())).thenReturn(catalog);
    }

    @AfterAll
    static void closeValidator() { VALIDATION.close(); }

    static AnalyticsAccessService.Access access(String formKey, boolean empty) {
        var configuration = new Configuration();
        var statement = new MappedStatement.Builder(configuration, "test.visible",
                new StaticSqlSource(configuration, "select id from `" + AnalyticsAccessService.SOURCES.get(formKey).table() + "`"), SqlCommandType.SELECT).build();
        return new AnalyticsAccessService.Access(AnalyticsAccessService.SOURCES.get(formKey), statement,
                statement.getBoundSql(Map.of()), empty, "org-a", new Scope("ALL", "SELF", null, "test"));
    }

    static <T extends BaseField> T field(T field, String id, String type, String businessKey) {
        field.setId(id);
        field.setType(type);
        field.setName(id);
        field.setBusinessKey(businessKey);
        field.setReadable(true);
        return field;
    }

    static AnalyticsSchemaService.Catalog catalog() {
        return AnalyticsSchemaService.catalog("customer", List.of(
                field(new InputField(), "id-field", "INPUT", "id"),
                field(new MemberField(), "owner-field", "MEMBER", "owner"),
                field(new DateTimeField(), "date-field", "DATE_TIME", "createTime"),
                field(new InputNumberField(), "amount-field", "INPUT_NUMBER", null)),
                Map.of("id", "`id`", "owner", "`owner`", "createTime", "`create_time`"));
    }

    static Metric metric(Aggregate operation, String field, String alias) {
        var metric = new Metric();
        metric.setOp(operation);
        metric.setField(field);
        metric.setAlias(alias);
        return metric;
    }

    static AnalyticsQueryRequest request() {
        var request = new AnalyticsQueryRequest();
        request.setFormKey("customer");
        request.setMetrics(List.of(metric(Aggregate.COUNT, null, "count"), metric(Aggregate.SUM, "amount-field", "amount")));
        return request;
    }

    static Dimension dimension(String field, Granularity granularity) {
        var dimension = new Dimension();
        dimension.setField(field);
        dimension.setGranularity(granularity);
        return dimension;
    }

    private static Map<String, Object> row(long groups, long matched, Object amount) {
        var row = new HashMap<String, Object>();
        row.put("_invalid_total", 0L);
        row.put("_groups", groups);
        row.put("_matched", matched);
        row.put("_matched_rows", matched);
        row.put("_records", matched);
        row.put("m0", matched);
        row.put("m1", amount);
        return row;
    }

    @Test
    void completeStatisticsUseOneAuthorizedQueryAndKeepDecimalPrecision() {
        BigDecimal amount = new BigDecimal("12345678901234567890.12345678901234567890");
        when(mapper.query(any())).thenReturn(List.of(row(1, 25001, amount)));
        var response = service.query(request(), "viewer", "org-a");
        assertTrue(response.complete());
        assertEquals(25001, response.matchedRecords());
        assertEquals("12345678901234567890.1234567890123456789", response.rows().getFirst().metrics().get("amount"));
        verify(mapper, times(1)).query(any());
        verify(accessService).resolve("customer", View.ALL, null, "viewer", "org-a");
        verify(schemaService).load("customer", access.source(), "org-a");
    }

    @Test
    void omittedGroupsAndDeliberateTopNAreDifferent() {
        var request = request();
        request.setLimit(1);
        request.setDimensions(List.of(dimension("id", Granularity.RAW)));
        var row = row(3, 7, BigDecimal.TEN);
        row.put("d0", "record-a");
        when(mapper.query(any())).thenReturn(List.of(row));
        var partial = service.query(request, "viewer", "org-a");
        assertEquals("partial", partial.status());
        assertFalse(partial.complete());
        assertTrue(partial.truncated());
        var order = new OrderBy();
        order.setMetric("amount");
        request.setOrderBy(order);
        var top = service.query(request, "viewer", "org-a");
        assertTrue(top.complete());
        assertFalse(top.truncated());
        assertEquals("TOP_N", top.selection());
        assertEquals(3, top.totalGroups());
        assertEquals(7, top.matchedRecords());
    }

    @Test
    void noDataPreservesZeroCountNullSumAndEmptyGroupedRows() {
        when(mapper.query(any())).thenReturn(List.of(row(1, 0, null)));
        var scalar = service.query(request(), "viewer", "org-a");
        assertEquals("no_data", scalar.status());
        assertEquals(0L, scalar.rows().getFirst().metrics().get("count"));
        assertNull(scalar.rows().getFirst().metrics().get("amount"));
        var request = request();
        request.setDimensions(List.of(dimension("owner", Granularity.RAW)));
        var empty = row(0, 0, null);
        empty.put("_records", null);
        when(mapper.query(any())).thenReturn(List.of(empty));
        assertTrue(service.query(request, "viewer", "org-a").rows().isEmpty());
    }

    @Test
    void unresolvedLabelsAreNotSilentlyPresentedAsNames() {
        var request = request();
        request.setDimensions(List.of(dimension("owner", Granularity.RAW)));
        var row = row(1, 2, BigDecimal.TEN);
        row.put("d0", "removed-user");
        when(mapper.query(any())).thenReturn(List.of(row));
        when(forms.getOptionMap(any(), anyList())).thenReturn(Map.of());
        var response = service.query(request, "viewer", "org-a");
        assertTrue(response.complete());
        assertFalse(response.displayResolutionComplete());
        assertEquals("removed-user", response.rows().getFirst().dimensionKeys().get("owner"));
        assertNull(response.rows().getFirst().dimensions().get("owner"));
    }

    @Test
    void groupedOptionsResolveThroughTheRealFormService() {
        var stage = field(new SelectField(), "stage-field", "SELECT", null);
        stage.setOptions(List.of(new OptionProp("won", "已成交")));
        when(schemaService.load(anyString(), any(), anyString()))
                .thenReturn(AnalyticsSchemaService.catalog("customer", List.of(stage), Map.of()));
        ReflectionTestUtils.setField(service, "moduleFormService", new ModuleFormService());
        var request = request();
        request.setMetrics(List.of(metric(Aggregate.COUNT, null, "count")));
        request.setDimensions(List.of(dimension("stage-field", Granularity.RAW)));
        var row = row(1, 2, null);
        row.put("d0", "won");
        when(mapper.query(any())).thenReturn(List.of(row));
        var response = service.query(request, "viewer", "org-a");
        assertTrue(response.displayResolutionComplete());
        assertEquals("已成交", response.rows().getFirst().dimensions().get("stage-field"));
    }

    @Test
    void invalidStoredValuesAndQueryFailuresNeverReturnPlausibleTotals() {
        var row = row(1, 2, null);
        row.put("_invalid_total", 1L);
        when(mapper.query(any())).thenReturn(List.of(row));
        assertThrows(GenericException.class, () -> service.query(request(), "viewer", "org-a"));
        when(mapper.query(any())).thenThrow(new QueryTimeoutException("private SQL"));
        var error = assertThrows(GenericException.class, () -> service.query(request(), "viewer", "org-a"));
        assertFalse(error.toString().contains("private SQL"));
    }

    @Test
    void hiddenAndReferenceFieldsAreRejectedAndMultivaluedFieldsAreTyped() {
        var hidden = field(new InputNumberField(), "secret", "INPUT_NUMBER", null);
        hidden.setReadable(false);
        var reference = field(new InputNumberField(), "ref", "INPUT_NUMBER", null);
        reference.setResourceFieldId("foreign-field");
        var multiple = field(new SelectMultipleField(), "tags", "SELECT_MULTIPLE", null);
        var schema = AnalyticsSchemaService.catalog("customer", List.of(hidden, reference, multiple), Map.of());
        assertTrue(schema.require("tags").schema().multiple());
        assertEquals(1, schema.response().unsupportedFields().size());
        assertThrows(GenericException.class, () -> schema.require("secret"));
        assertThrows(GenericException.class, () -> schema.require("ref"));
    }

    @Test
    void systemFieldsDoNotResurrectAnExplicitlyHiddenField() {
        var hidden = field(new DateTimeField(), "hidden-created", "DATE_TIME", "createTime");
        hidden.setReadable(false);
        var config = new ModuleFormConfigDTO();
        config.setFields(List.of(hidden));
        var cache = mock(ModuleFormCacheService.class);
        when(cache.getConfig("customer", "org-a")).thenReturn(config);
        var service = new AnalyticsSchemaService();
        ReflectionTestUtils.setField(service, "moduleFormCacheService", cache);
        ReflectionTestUtils.setField(service, "moduleFormService", mock(ModuleFormService.class));
        ReflectionTestUtils.setField(service, "opportunityService", mock(OpportunityService.class));
        assertFalse(service.load("customer", access.source(), "org-a").fields().containsKey("createTime"));
        verify(cache, never()).getBusinessFormConfig(anyString(), anyString());
        assertEquals(1, config.getFields().size());
    }

    @Test
    void analyticsLoadsPersistedFieldsWithoutDetailUiOrMutatingCachedSchema() {
        var owner = field(new MemberField(), "owner-id", "MEMBER", null);
        owner.setInternalKey("customerOwner");
        var select = field(new SelectField(), "stage-id", "SELECT", null);
        var option = new OptionProp();
        option.setValue("stage-1");
        option.setLabel("已成交");
        select.setCustomOptions(List.of(option));
        var config = new ModuleFormConfigDTO();
        config.setFields(List.of(owner, select));
        var cache = mock(ModuleFormCacheService.class);
        when(cache.getConfig("customer", "org-a")).thenReturn(config);
        var forms = spy(new ModuleFormService());
        var service = new AnalyticsSchemaService();
        ReflectionTestUtils.setField(service, "moduleFormCacheService", cache);
        ReflectionTestUtils.setField(service, "moduleFormService", forms);
        var schema = service.load("customer", access.source(), "org-a");
        assertEquals("`owner`", schema.require("owner").column());
        assertEquals("已成交", schema.require("stage-id").schema().options().getFirst().getLabel());
        assertNull(owner.getBusinessKey());
        assertTrue(select.getOptions() == null || select.getOptions().isEmpty());
        verify(cache, never()).getBusinessFormConfig(anyString(), anyString());
        verify(forms, never()).freshInitialOptions(any());
        verify(forms, never()).resolveDetailTabs(any(), any(), any());
    }

    @Test
    void scalarValuesUseCorrectStorageAndCustomerPoolOwnerMask() {
        var request = request();
        request.setDimensions(List.of(dimension("owner", Granularity.RAW)));
        String customerSql = AnalyticsSqlBuilder.compile(access, catalog, request).sql();
        assertTrue(customerSql.contains("if(a.in_shared_pool, null, a.`owner`)"));
        assertFalse(customerSql.contains("ref_sub_id"));
        assertTrue(customerSql.contains("a.id in (select id from analytics_visible)"));
        String opportunitySql = AnalyticsSqlBuilder.compile(access("opportunity", false), catalog, request).sql();
        assertTrue(opportunitySql.contains("ref_sub_id is null"));
        assertTrue(opportunitySql.contains("row_id is null"));
        assertTrue(AnalyticsSqlBuilder.compile(access("customer", true), catalog, request).sql().contains("and 1 = 0"));
    }

    @Test
    void untrustedFieldIdsAliasesAndValuesCannotBecomeSql() {
        String hostile = "'; drop table customer; --";
        var custom = field(new InputField(), hostile, "INPUT", null);
        var schema = AnalyticsSchemaService.catalog("customer", List.of(custom), Map.of());
        var request = request();
        request.setMetrics(List.of(metric(Aggregate.COUNT, hostile, hostile)));
        var condition = new Condition();
        condition.setField(hostile);
        condition.setOperator(Operator.CONTAINS);
        condition.setValue(hostile);
        request.setConditions(List.of(condition));
        var plan = AnalyticsSqlBuilder.compile(access, schema, request);
        assertFalse(plan.sql().contains(hostile));
        assertEquals(List.of(hostile, "org-a", hostile, 200), plan.parameters());
        request.getMetrics().getFirst().setField("unknown");
        assertThrows(GenericException.class, () -> AnalyticsSqlBuilder.compile(access, schema, request));
    }

    @Test
    void timeRangeUsesTimezoneAndExclusiveEndAndRejectsAmbiguity() {
        var request = request();
        var range = new TimeRange();
        range.setField("createTime");
        range.setFrom("2026-09-01");
        range.setToExclusive("2026-10-01");
        request.setTimeRange(range);
        request.setDimensions(List.of(dimension("createTime", Granularity.MONTH)));
        var plan = AnalyticsSqlBuilder.compile(access, catalog, request);
        assertTrue(plan.sql().contains(" >= ? and f1 < ?"));
        assertTrue(plan.parameters().contains(1788192000000L));
        assertTrue(plan.parameters().contains(1790784000000L));
        assertTrue(plan.parameters().contains("+08:00"));
        request.setMetrics(List.of(metric(Aggregate.MAX, "createTime", "latest")));
        assertTrue(AnalyticsSqlBuilder.compile(access, catalog, request).sql().contains("MAX(f0) as m0"));
        range.setToExclusive("2026-08-01");
        assertThrows(GenericException.class, () -> AnalyticsSqlBuilder.compile(access, catalog, request));
        assertThrows(GenericException.class, () -> AnalyticsSqlBuilder.instant("2026-11-01T01:30:00", ZoneId.of("America/New_York")));
        assertThrows(GenericException.class, () -> AnalyticsSqlBuilder.instant("2026-03-08T02:30:00", ZoneId.of("America/New_York")));
    }

    @Test
    void invalidLimitsAndOperatorsFailBeforeExecutingSql() {
        var request = request();
        request.setLimit(501);
        assertThrows(GenericException.class, () -> service.query(request, "viewer", "org-a"));
        request.setLimit(200);
        request.setMetrics(List.of(metric(Aggregate.SUM, "owner", "wrong")));
        assertThrows(GenericException.class, () -> service.query(request, "viewer", "org-a"));
        request.setMetrics(List.of(metric(Aggregate.COUNT, null, "same"), metric(Aggregate.COUNT, null, "same")));
        assertThrows(GenericException.class, () -> service.query(request, "viewer", "org-a"));
        verifyNoInteractions(mapper);
    }

    @Test
    void jsonRejectsUnknownIdentityAndMisspelledFiltersAndPreservesNumbers() throws Exception {
        var json = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        for (String unknown : List.of("organizationId", "userId", "sql", "conditions")) {
            assertThrows(Exception.class, () -> json.readValue("{\"" + unknown + "\":\"x\"}", AnalyticsQueryRequest.class));
        }
        assertThrows(Exception.class, () -> json.readValue("{\"metrics\":[{\"op\":\"COUNT\",\"unknown\":true}]}", AnalyticsQueryRequest.class));
        var request = json.readValue("""
                {"conditions":[{"field":"amount","operator":"EQUALS","value":12345678901234567890.12345678901234567890}]}
                """, AnalyticsQueryRequest.class);
        assertEquals(new BigDecimal("12345678901234567890.12345678901234567890"), request.getConditions().getFirst().getValue());
        assertThrows(Exception.class, () -> json.readValue("{\"conditions\":[{\"value\":{\"sql\":\"x\"}}]}", AnalyticsQueryRequest.class));
    }
    static AnalyticsSchemaService.Catalog lineCatalog() {
        var product = field(new DatasourceField(), "product-id", "DATA_SOURCE", "productId");
        product.setDataSourceType("PRODUCT");
        var lines = field(new ProductSubField(), "items", "SUB_PRODUCT", "products");
        lines.setSubFields(List.of(product, field(new InputNumberField(), "line-amount", "INPUT_NUMBER", null),
                field(new SelectMultipleField(), "tags", "SELECT_MULTIPLE", null)));
        return AnalyticsSchemaService.catalog("order", List.of(
                field(new InputField(), "id-field", "INPUT", "id"),
                field(new InputNumberField(), "order-amount", "INPUT_NUMBER", "amount"), lines),
                Map.of("id", "`id`", "amount", "`amount`"));
    }

    @Test
    void subtableGrainUsesLiveFieldsAndRejectsParentMoneyFanout() {
        var root = lineCatalog();
        var lines = root.dataset("items");
        assertEquals("SUBTABLE_ROW", lines.response().grain());
        assertEquals("product", lines.require("line.productId").schema().relatedFormKey());
        assertEquals("productId", lines.require("line.productId").base().idOrBusinessKey());
        assertTrue(lines.require("line.tags").schema().multiple());
        assertThrows(GenericException.class, () -> root.dataset("hidden-table"));
        var request = request(); request.setFormKey("order"); request.setSubTableFieldId("items");
        request.setMetrics(List.of(metric(Aggregate.SUM, "amount", "duplicated")));
        assertThrows(GenericException.class, () -> AnalyticsSqlBuilder.compile(access("order", false), lines, request));
        request.setMetrics(List.of(metric(Aggregate.SUM, "line.line-amount", "amount"), metric(Aggregate.COUNT_DISTINCT, "id", "orders")));
        var plan = AnalyticsSqlBuilder.compile(access("order", false), lines, request);
        assertTrue(plan.sql().contains("union select resource_id, row_id from `sales_order_field_blob`"));
        assertTrue(plan.sql().contains("and ref_sub_id = ? and row_id = r.row_id"));
        assertFalse(plan.sql().contains("line-amount"));
        assertTrue(plan.parameters().contains("line-amount"));
    }

    @Test
    void relationCannotBypassSubjectPermissionsAndDoesNotAcceptNonRelations() {
        when(schemaService.load(anyString(), any(), anyString())).thenReturn(lineCatalog());
        when(accessService.resolve(eq("product"), any(), any(), anyString(), anyString()))
                .thenThrow(new GenericException(cn.cordys.common.response.result.CrmHttpResultCode.FORBIDDEN));
        var request = request(); request.setFormKey("order"); request.setSubTableFieldId("items");
        request.setMetrics(List.of(metric(Aggregate.COUNT, null, "count")));
        request.setRelationField("line.productId");
        assertThrows(GenericException.class, () -> service.query(request, "viewer", "org-a"));
        verify(accessService).resolve("product", View.ALL, null, "viewer", "org-a");
        verifyNoInteractions(mapper);
        request.setRelationField("id");
        assertThrows(GenericException.class, () -> service.query(request, "viewer", "org-a"));
    }

    @Test
    void multiValueFiltersUseMembershipWithoutInflatingGroupTotals() {
        var request = request(); request.setSubTableFieldId("items");
        request.setMetrics(List.of(metric(Aggregate.COUNT, null, "count")));
        request.setDimensions(List.of(dimension("line.tags", Granularity.RAW)));
        var condition = new Condition(); condition.setField("line.tags"); condition.setOperator(Operator.NOT_IN);
        condition.setValue(List.of("private-tag")); request.setConditions(List.of(condition));
        var plan = AnalyticsSqlBuilder.compile(access("order", false), lineCatalog().dataset("items"), request);
        assertTrue(plan.sql().contains("not exists (select 1 from json_table"));
        assertTrue(plan.sql().contains("select distinct analytics_filtered.*"));
        assertTrue(plan.sql().contains("count(distinct id) from analytics_filtered"));
        assertFalse(plan.sql().contains("private-tag"));
        request.setMetrics(List.of(metric(Aggregate.COUNT_DISTINCT, "line.tags", "sets")));
        assertThrows(GenericException.class, () -> AnalyticsSqlBuilder.compile(access("order", false), lineCatalog().dataset("items"), request));
    }

}
