package cn.cordys.crm.system.service;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.statistic.StatisticSqlMapper;
import cn.cordys.common.util.JSON;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.system.constants.FieldType;
import cn.cordys.crm.system.constants.StatisticType;
import cn.cordys.crm.system.domain.ModuleField;
import cn.cordys.crm.system.dto.StatisticFieldSourceDTO;
import cn.cordys.crm.system.dto.field.DatasourceField;
import cn.cordys.crm.system.dto.field.StatisticField;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.mapper.ExtStatisticMapper;
import cn.cordys.crm.system.service.StatisticFieldService.HostRecord;
import cn.cordys.crm.system.service.StatisticFieldService.StatisticDeleteScope;
import cn.cordys.mybatis.BaseMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 删除目标数据后重算统计字段: 捕获与刷新两段。
 *
 * <p>不起 Spring 上下文, 依赖全部用 Mockito 顶掉 —— 这段逻辑的输入只有「关联字段读出什么值」
 * 与「宿主还在不在」两件事, 用假数据比连库更能把边界钉死(Docker 在本机不可用,
 * 真库那条路交给 {@code ExtStatisticMapperTests})。</p>
 */
class StatisticDeleteScopeTests {

    private static final String ORG_ID = "100001";
    /**
     * 宿主表单: 客户。
     */
    private static final String HOST_FORM_KEY = FormKey.CUSTOMER.getKey();
    /**
     * 被删数据所属表单, 也就是统计目标: 跟进记录。
     */
    private static final String TARGET_FORM_KEY = FormKey.FOLLOW_RECORD.getKey();
    private static final String RELATED_FIELD_ID = "relatedField";
    private static final String HOST_DATA_ID = "customer1";

    private StatisticFieldService service;
    private ExtStatisticMapper extStatisticMapper;
    private ModuleFormCacheService moduleFormCacheService;
    private StatisticSqlMapper targetSqlMapper;
    private StatisticFieldService self;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        service = new StatisticFieldService();
        extStatisticMapper = mock(ExtStatisticMapper.class);
        moduleFormCacheService = mock(ModuleFormCacheService.class);
        targetSqlMapper = mock(StatisticSqlMapper.class);
        self = mock(StatisticFieldService.class);

        StatisticSqlMapperRegistry registry = mock(StatisticSqlMapperRegistry.class);
        when(registry.get(TARGET_FORM_KEY)).thenReturn(targetSqlMapper);

        ReflectionTestUtils.setField(service, "extStatisticMapper", extStatisticMapper);
        ReflectionTestUtils.setField(service, "moduleFormCacheService", moduleFormCacheService);
        ReflectionTestUtils.setField(service, "statisticSqlMapperRegistry", registry);
        // self 是 @Lazy 自注入, 只为让 writeStatisticValue 上的 @Transactional 走到代理;
        // 单测里换成 mock, 既不用真写库, 又能数清「写回发生了几次」。
        ReflectionTestUtils.setField(service, "self", self);
        ReflectionTestUtils.setField(service, "moduleFieldMapper", mock(BaseMapper.class));

        givenTargetFormRelatedField();
    }

    @AfterEach
    void tearDown() {
        // 服务在 finally 里恢复的是「进来时的组织」, 进来时为空时不写回, 组织会留在 ThreadLocal 上
        OrganizationContext.clear();
    }

    @Test
    void captureReturnsEmptyScopeWhenNoStatisticFieldTargetsForm() {
        // 字段存在, 但统计的是合同, 与本次要删的跟进记录无关
        StatisticFieldSourceDTO source = statisticFieldSource("field1");
        StatisticField field = JSON.parseObject(source.getProp(), StatisticField.class);
        field.setTargetFormId(FormKey.CONTRACT.getKey());
        source.setProp(JSON.toJSONString(field));
        when(extStatisticMapper.selectStatisticFields(anyString(), anyString(), any()))
                .thenReturn(List.of(source));

        StatisticDeleteScope scope = service.captureRelatedHosts(TARGET_FORM_KEY, List.of("record1"), ORG_ID);

        assertTrue(scope.hosts().isEmpty());
        // 没人统计这张表单时不能有任何额外查询: 这条路径挂在每一次删除上
        verify(moduleFormCacheService, never()).getConfig(any(), any());
        verify(extStatisticMapper, never()).selectFieldValue(any(), any(), any(), any(), anyBoolean());
    }

    @Test
    void captureResolvesHostIdFromTargetRelationField() {
        givenStatisticFieldTargetsFollowRecords("field1");
        when(extStatisticMapper.selectFieldValue(anyString(), anyString(), eq(RELATED_FIELD_ID),
                eq("record1"), anyBoolean())).thenReturn(HOST_DATA_ID);

        StatisticDeleteScope scope = service.captureRelatedHosts(TARGET_FORM_KEY, List.of("record1"), ORG_ID);

        assertEquals(TARGET_FORM_KEY, scope.targetFormKey());
        assertEquals(ORG_ID, scope.orgId());
        assertEquals(1, scope.hosts().size());
        HostRecord host = scope.hosts().getFirst();
        assertEquals(HOST_FORM_KEY, host.hostFormKey());
        assertEquals("customer", host.hostDataTable());
        assertEquals(HOST_DATA_ID, host.hostDataId());
        assertEquals("field1", host.hostField().getId());
    }

    @Test
    void captureDeduplicatesSameHostAcrossTargetIds() {
        givenStatisticFieldTargetsFollowRecords("field1");
        // 两条跟进记录关联到同一个客户: 批量删除时这是常态
        when(extStatisticMapper.selectFieldValue(anyString(), anyString(), eq(RELATED_FIELD_ID),
                anyString(), anyBoolean())).thenReturn(HOST_DATA_ID);

        StatisticDeleteScope scope = service.captureRelatedHosts(TARGET_FORM_KEY,
                List.of("record1", "record2"), ORG_ID);

        assertEquals(1, scope.hosts().size());
        assertEquals(HOST_DATA_ID, scope.hosts().getFirst().hostDataId());
    }

    @Test
    void captureSkipsTargetWithoutRelationValue() {
        givenStatisticFieldTargetsFollowRecords("field1");
        when(extStatisticMapper.selectFieldValue(anyString(), anyString(), eq(RELATED_FIELD_ID),
                anyString(), anyBoolean())).thenReturn(null);

        StatisticDeleteScope scope = service.captureRelatedHosts(TARGET_FORM_KEY, List.of("record1"), ORG_ID);

        assertTrue(scope.hosts().isEmpty());
    }

    @Test
    void refreshSkipsHostThatNoLongerExists() {
        givenStatisticFieldTargetsFollowRecords("field1");
        // 宿主和它关联的数据在同一次级联删除里一起没了
        when(extStatisticMapper.selectExistingDataIds(anyString(), anyList())).thenReturn(List.of());

        service.refreshAfterRelatedDelete(scopeOf(hostRecord("field1")));

        // 连聚合都不该发生, 否则会给一条不存在的记录写出孤儿值行
        verify(targetSqlMapper, never()).selectStatisticAggregate(any());
        verify(self, never()).writeStatisticValue(any(), any(), any());
    }

    @Test
    void refreshRecalculatesOnlyExistingHosts() {
        givenStatisticFieldTargetsFollowRecords("field1");
        when(extStatisticMapper.selectExistingDataIds(anyString(), anyList()))
                .thenReturn(List.of(HOST_DATA_ID));
        when(targetSqlMapper.selectStatisticAggregate(any())).thenReturn(BigDecimal.valueOf(2));

        HostRecord gone = new HostRecord(HOST_FORM_KEY, "customer", statisticField("field1"), "customer2");
        service.refreshAfterRelatedDelete(
                new StatisticDeleteScope(TARGET_FORM_KEY, ORG_ID, List.of(hostRecord("field1"), gone)));

        verify(targetSqlMapper, times(1)).selectStatisticAggregate(any());
        // 写回的是这一步聚合出来的值, 不是别处读来的
        verify(self, times(1)).writeStatisticValue(any(), eq(HOST_DATA_ID), eq(BigDecimal.valueOf(2)));
    }

    @Test
    void refreshKeepsGoingWhenOneFieldFails() {
        // 两个统计字段都挂在客户上, 但统计的是同一批跟进记录
        HostRecord first = hostRecord("field1");
        HostRecord second = new HostRecord(HOST_FORM_KEY, "customer", statisticField("field2"), HOST_DATA_ID);
        // 分组按 host 的遍历顺序走, 所以第一次调用一定是 field1 那次
        when(extStatisticMapper.selectExistingDataIds(anyString(), anyList()))
                .thenReturn(List.of(HOST_DATA_ID));
        when(targetSqlMapper.selectStatisticAggregate(any()))
                .thenThrow(new IllegalStateException("模拟聚合失败"))
                .thenReturn(BigDecimal.ONE);

        service.refreshAfterRelatedDelete(
                new StatisticDeleteScope(TARGET_FORM_KEY, ORG_ID, List.of(first, second)));

        verify(targetSqlMapper, times(2)).selectStatisticAggregate(any());
        // field1 挂了不影响 field2 写回
        verify(self, times(1)).writeStatisticValue(any(), eq(HOST_DATA_ID), eq(BigDecimal.ONE));
    }

    /**
     * 目标表单的字段配置: 关联字段解析不出来会被当成「字段已删」直接跳过, 所以每个用例都要先给上。
     */
    private void givenTargetFormRelatedField() {
        DatasourceField relatedField = new DatasourceField();
        relatedField.setId(RELATED_FIELD_ID);
        relatedField.setType(FieldType.DATA_SOURCE.name());
        ModuleFormConfigDTO config = new ModuleFormConfigDTO();
        config.setFields(List.of(relatedField));
        when(moduleFormCacheService.getConfig(TARGET_FORM_KEY, ORG_ID)).thenReturn(config);
    }

    /**
     * 让反查命中「客户上的一个统计字段, 统计目标是跟进记录」。
     */
    private void givenStatisticFieldTargetsFollowRecords(String fieldId) {
        when(extStatisticMapper.selectStatisticFields(anyString(), anyString(), any()))
                .thenReturn(List.of(statisticFieldSource(fieldId)));
    }

    private StatisticFieldSourceDTO statisticFieldSource(String fieldId) {
        StatisticFieldSourceDTO source = new StatisticFieldSourceDTO();
        source.setHostFormKey(HOST_FORM_KEY);
        source.setFieldId(fieldId);
        // 走一遍序列化: 生产上 prop 是库里存的字段 JSON, 反查时按同样的方式解析回 BaseField
        source.setProp(JSON.toJSONString(statisticField(fieldId)));
        return source;
    }

    private StatisticField statisticField(String fieldId) {
        StatisticField field = new StatisticField();
        field.setId(fieldId);
        field.setType(FieldType.STATISTIC.name());
        field.setTargetFormId(TARGET_FORM_KEY);
        field.setRelatedFieldId(RELATED_FIELD_ID);
        field.setStatisticType(StatisticType.COUNT.name());
        return field;
    }

    private HostRecord hostRecord(String fieldId) {
        return new HostRecord(HOST_FORM_KEY, "customer", statisticField(fieldId), HOST_DATA_ID);
    }

    private StatisticDeleteScope scopeOf(HostRecord host) {
        return new StatisticDeleteScope(TARGET_FORM_KEY, ORG_ID, List.of(host));
    }
}
