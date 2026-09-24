package cn.cordys.crm.system.service;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.statistic.StatisticSqlMapper;
import cn.cordys.common.util.JSON;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.system.constants.FieldType;
import cn.cordys.crm.system.constants.StatisticDataScope;
import cn.cordys.crm.system.constants.StatisticType;
import cn.cordys.crm.system.domain.ModuleField;
import cn.cordys.crm.system.dto.StatisticFieldSourceDTO;
import cn.cordys.crm.system.dto.field.DatasourceField;
import cn.cordys.crm.system.dto.field.StatisticField;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.mapper.ExtStatisticMapper;
import cn.cordys.crm.system.service.StatisticFieldService.HostRecord;
import cn.cordys.crm.system.service.StatisticFieldService.StatisticHostScope;
import cn.cordys.mybatis.BaseMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 统计字段「写入前捕获、写入后重算」这一对动作: 变更与删除两条路径共用。
 *
 * <p>不起 Spring 上下文, 依赖全部用 Mockito 顶掉 —— 这段逻辑的输入只有「关联字段读出什么值」
 * 与「宿主还在不在」两件事, 用假数据比连库更能把边界钉死(Docker 在本机不可用,
 * 真库那条路交给 {@code ExtStatisticMapperTests})。</p>
 *
 * <p>变更路径比删除路径多一层: 写入前后各捕一次, 取并集。所以这里的用例重点在两件事上 ——
 * 关联关系换人时旧的与新的都要算, 没换人时并集不能退化成算两遍。</p>
 */
class StatisticHostScopeTests {

    private static final String ORG_ID = "100001";
    /**
     * 宿主表单: 客户。
     */
    private static final String HOST_FORM_KEY = FormKey.CUSTOMER.getKey();
    /**
     * 目标数据所属表单, 也就是统计目标: 跟进记录。
     */
    private static final String TARGET_FORM_KEY = FormKey.FOLLOW_RECORD.getKey();
    private static final String RELATED_FIELD_ID = "relatedField";
    private static final String HOST_DATA_ID = "customer1";
    /**
     * 关联字段被改成别的宿主后, 新指向的那条宿主记录。
     */
    private static final String MOVED_HOST_DATA_ID = "customer2";
    private static final String TARGET_DATA_ID = "record1";

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

        StatisticHostScope scope = service.captureRelatedHosts(TARGET_FORM_KEY, List.of("record1"), ORG_ID);

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

        StatisticHostScope scope = service.captureRelatedHosts(TARGET_FORM_KEY, List.of("record1"), ORG_ID);

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

        StatisticHostScope scope = service.captureRelatedHosts(TARGET_FORM_KEY,
                List.of("record1", "record2"), ORG_ID);

        assertEquals(1, scope.hosts().size());
        assertEquals(HOST_DATA_ID, scope.hosts().getFirst().hostDataId());
    }

    @Test
    void captureSkipsTargetWithoutRelationValue() {
        givenStatisticFieldTargetsFollowRecords("field1");
        when(extStatisticMapper.selectFieldValue(anyString(), anyString(), eq(RELATED_FIELD_ID),
                anyString(), anyBoolean())).thenReturn(null);

        StatisticHostScope scope = service.captureRelatedHosts(TARGET_FORM_KEY, List.of("record1"), ORG_ID);

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
                new StatisticHostScope(TARGET_FORM_KEY, ORG_ID, List.of(hostRecord("field1"), gone)));

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
                new StatisticHostScope(TARGET_FORM_KEY, ORG_ID, List.of(first, second)));

        verify(targetSqlMapper, times(2)).selectStatisticAggregate(any());
        // field1 挂了不影响 field2 写回
        verify(self, times(1)).writeStatisticValue(any(), eq(HOST_DATA_ID), eq(BigDecimal.ONE));
    }

    @Test
    void captureKeepsGoingWhenOneTargetReadFails() {
        givenStatisticFieldTargetsFollowRecords("field1");
        // 批量操作里某一条读不出来是常事(字段值被并发改掉、值行刚被删), 不能因此丢掉整批
        when(extStatisticMapper.selectFieldValue(anyString(), anyString(), eq(RELATED_FIELD_ID),
                eq("record1"), anyBoolean())).thenThrow(new IllegalStateException("模拟读关联值失败"));
        when(extStatisticMapper.selectFieldValue(anyString(), anyString(), eq(RELATED_FIELD_ID),
                eq("record2"), anyBoolean())).thenReturn(HOST_DATA_ID);

        StatisticHostScope scope = service.captureRelatedHosts(TARGET_FORM_KEY,
                List.of("record1", "record2"), ORG_ID);

        assertEquals(1, scope.hosts().size());
        assertEquals(HOST_DATA_ID, scope.hosts().getFirst().hostDataId());
    }

    @Test
    void captureForFieldChangeSkipsWhenNoStatisticFieldUsesThatField() {
        givenStatisticFieldTargetsFollowRecords("field1");

        // 批量编辑改的是「备注」这类普通字段, 没有任何统计字段拿它当关联字段
        StatisticHostScope scope = service.captureRelatedHostsForFieldChange(TARGET_FORM_KEY,
                "otherField", List.of(TARGET_DATA_ID), ORG_ID);

        assertTrue(scope.hosts().isEmpty());
        // 这条路径挂在每一次批量编辑上, 大多改的都不是关联字段, 必须在这里就断掉 ——
        // 否则一次 500 行的批量编辑会按行数做两轮关联值点查
        verify(moduleFormCacheService, never()).getConfig(any(), any());
        verify(extStatisticMapper, never()).selectFieldValue(any(), any(), any(), any(), anyBoolean());
    }

    @Test
    void captureForFieldChangeKeepsHostWhenFieldMatches() {
        givenStatisticFieldTargetsFollowRecords("field1");
        when(extStatisticMapper.selectFieldValue(anyString(), anyString(), eq(RELATED_FIELD_ID),
                eq(TARGET_DATA_ID), anyBoolean())).thenReturn(HOST_DATA_ID);

        // 改的正是这个统计字段的关联字段, 过滤不能把该捕的也滤掉
        StatisticHostScope scope = service.captureRelatedHostsForFieldChange(TARGET_FORM_KEY,
                RELATED_FIELD_ID, List.of(TARGET_DATA_ID), ORG_ID);

        assertEquals(1, scope.hosts().size());
        assertEquals(HOST_DATA_ID, scope.hosts().getFirst().hostDataId());
    }

    @Test
    void captureForFieldChangeKeepsHostWhenStatisticFieldChanges() {
        // 统计类型是 SUM, 被统计的是目标表单上的「金额」
        StatisticField field = statisticField("field1");
        field.setStatisticType(StatisticType.SUM.name());
        field.setStatisticFieldId("amountField");
        givenStatisticFieldTargetsFollowRecords(field);
        when(extStatisticMapper.selectFieldValue(anyString(), anyString(), eq(RELATED_FIELD_ID),
                eq(TARGET_DATA_ID), anyBoolean())).thenReturn(HOST_DATA_ID);

        // 批量改金额: 关联关系一个都没动, 但客户那边统计的和变了, 宿主照样得重算。
        // 只按关联字段过滤的话, 这里会返回空范围, 客户上的统计值永远停在旧数
        StatisticHostScope scope = service.captureRelatedHostsForFieldChange(TARGET_FORM_KEY,
                "amountField", List.of(TARGET_DATA_ID), ORG_ID);

        assertEquals(1, scope.hosts().size());
        assertEquals(HOST_DATA_ID, scope.hosts().getFirst().hostDataId());
    }

    @Test
    void captureForFieldChangeKeepsHostWhenScopeConditionFieldChanges() {
        // 统计范围 = 符合条件: 只统计金额大于 100 的跟进记录
        StatisticField field = statisticField("field1");
        field.setDataScope(StatisticDataScope.CONDITION.name());
        field.setCombineSearch(Map.of(
                "searchMode", "AND",
                "conditions", List.of(Map.of(
                        "leftFieldId", "amountField",
                        "leftFieldType", "NUMBER",
                        "operator", "GT",
                        "rightFieldCustomValue", "100"))));
        givenStatisticFieldTargetsFollowRecords(field);
        when(extStatisticMapper.selectFieldValue(anyString(), anyString(), eq(RELATED_FIELD_ID),
                eq(TARGET_DATA_ID), anyBoolean())).thenReturn(HOST_DATA_ID);

        // 批量改金额: 有些记录会因为这次改动进出统计范围, 条件字段同样要算作「影响统计值」
        StatisticHostScope scope = service.captureRelatedHostsForFieldChange(TARGET_FORM_KEY,
                "amountField", List.of(TARGET_DATA_ID), ORG_ID);

        assertEquals(1, scope.hosts().size());
        assertEquals(HOST_DATA_ID, scope.hosts().getFirst().hostDataId());
    }

    @Test
    void captureForFieldChangeSkipsWhenOnlyUnrelatedFieldChanges() {
        // 关联字段、被统计字段、统计范围条件字段都说清楚了, 剩下的改法一律不该触发重算
        StatisticField field = statisticField("field1");
        field.setStatisticType(StatisticType.SUM.name());
        field.setStatisticFieldId("amountField");
        field.setDataScope(StatisticDataScope.CONDITION.name());
        field.setCombineSearch(Map.of(
                "searchMode", "AND",
                "conditions", List.of(Map.of("leftFieldId", "amountField", "operator", "GT"))));
        givenStatisticFieldTargetsFollowRecords(field);

        StatisticHostScope scope = service.captureRelatedHostsForFieldChange(TARGET_FORM_KEY,
                "remarkField", List.of(TARGET_DATA_ID), ORG_ID);

        assertTrue(scope.hosts().isEmpty());
        verify(extStatisticMapper, never()).selectFieldValue(any(), any(), any(), any(), anyBoolean());
    }

    @Test
    void changeRefreshesBothOldAndNewHostWhenRelationMoved() {
        givenStatisticFieldTargetsFollowRecords("field1");
        // 第一次读是写入前(关联客户1), 之后是写入后(已经改成客户2了)
        when(extStatisticMapper.selectFieldValue(anyString(), anyString(), eq(RELATED_FIELD_ID),
                eq(TARGET_DATA_ID), anyBoolean())).thenReturn(HOST_DATA_ID, MOVED_HOST_DATA_ID);
        when(extStatisticMapper.selectExistingDataIds(anyString(), anyList()))
                .thenReturn(List.of(HOST_DATA_ID, MOVED_HOST_DATA_ID));
        when(targetSqlMapper.selectStatisticAggregate(any())).thenReturn(BigDecimal.ONE);

        StatisticHostScope scope = service.captureRelatedHosts(TARGET_FORM_KEY,
                List.of(TARGET_DATA_ID), ORG_ID);
        service.refreshAfterRelatedChange(scope, List.of(TARGET_DATA_ID));

        // 写入前捕到 A、写入后读到 B: A 少了一条、B 多了一条, 两边都要算
        verify(targetSqlMapper, times(2)).selectStatisticAggregate(any());
        verify(self, times(1)).writeStatisticValue(any(), eq(HOST_DATA_ID), any());
        verify(self, times(1)).writeStatisticValue(any(), eq(MOVED_HOST_DATA_ID), any());
    }

    @Test
    void changeRefreshesHostOnceWhenRelationUnchanged() {
        givenStatisticFieldTargetsFollowRecords("field1");
        // 单条编辑改的是别的字段(捕获时不做字段过滤), 关联客户前前后后都是同一个 ——
        // 两次捕获拿到的是同一批宿主
        when(extStatisticMapper.selectFieldValue(anyString(), anyString(), eq(RELATED_FIELD_ID),
                eq(TARGET_DATA_ID), anyBoolean())).thenReturn(HOST_DATA_ID);
        when(extStatisticMapper.selectExistingDataIds(anyString(), anyList()))
                .thenReturn(List.of(HOST_DATA_ID));
        when(targetSqlMapper.selectStatisticAggregate(any())).thenReturn(BigDecimal.ONE);

        StatisticHostScope scope = service.captureRelatedHosts(TARGET_FORM_KEY,
                List.of(TARGET_DATA_ID), ORG_ID);
        service.refreshAfterRelatedChange(scope, List.of(TARGET_DATA_ID));

        // 并集靠 HostRecord 的值相等去重, 不能退化成把同一条宿主算两遍
        verify(targetSqlMapper, times(1)).selectStatisticAggregate(any());
        verify(self, times(1)).writeStatisticValue(any(), eq(HOST_DATA_ID), any());
    }

    @Test
    void changeRefreshesOldHostWhenRelationCleared() {
        givenStatisticFieldTargetsFollowRecords("field1");
        // 写入前还关联着客户1, 写入后关联被清空了
        when(extStatisticMapper.selectFieldValue(anyString(), anyString(), eq(RELATED_FIELD_ID),
                eq(TARGET_DATA_ID), anyBoolean())).thenReturn(HOST_DATA_ID).thenReturn(null);
        when(extStatisticMapper.selectExistingDataIds(anyString(), anyList()))
                .thenReturn(List.of(HOST_DATA_ID));
        when(targetSqlMapper.selectStatisticAggregate(any())).thenReturn(null);

        StatisticHostScope scope = service.captureRelatedHosts(TARGET_FORM_KEY,
                List.of(TARGET_DATA_ID), ORG_ID);
        service.refreshAfterRelatedChange(scope, List.of(TARGET_DATA_ID));

        // 关联被清空时写入后再也读不到宿主, 只靠写入后那份这个客户会永远停在旧值上
        verify(targetSqlMapper, times(1)).selectStatisticAggregate(any());
        verify(self, times(1)).writeStatisticValue(any(), eq(HOST_DATA_ID), isNull());
    }

    @Test
    void changeSkipsOldHostThatNoLongerExists() {
        givenStatisticFieldTargetsFollowRecords("field1");
        when(extStatisticMapper.selectFieldValue(anyString(), anyString(), eq(RELATED_FIELD_ID),
                eq(TARGET_DATA_ID), anyBoolean())).thenReturn(HOST_DATA_ID, MOVED_HOST_DATA_ID);
        // 旧宿主在同一批操作里已经被删了
        when(extStatisticMapper.selectExistingDataIds(anyString(), anyList()))
                .thenReturn(List.of(MOVED_HOST_DATA_ID));
        when(targetSqlMapper.selectStatisticAggregate(any())).thenReturn(BigDecimal.ONE);

        StatisticHostScope scope = service.captureRelatedHosts(TARGET_FORM_KEY,
                List.of(TARGET_DATA_ID), ORG_ID);
        service.refreshAfterRelatedChange(scope, List.of(TARGET_DATA_ID));

        verify(targetSqlMapper, times(1)).selectStatisticAggregate(any());
        verify(self, times(1)).writeStatisticValue(any(), eq(MOVED_HOST_DATA_ID), any());
        verify(self, never()).writeStatisticValue(any(), eq(HOST_DATA_ID), any());
    }

    @Test
    void changeRefreshesOldHostWhenPostWriteCaptureFails() {
        givenStatisticFieldTargetsFollowRecords("field1");
        when(extStatisticMapper.selectFieldValue(anyString(), anyString(), eq(RELATED_FIELD_ID),
                eq(TARGET_DATA_ID), anyBoolean())).thenReturn(HOST_DATA_ID);
        when(extStatisticMapper.selectExistingDataIds(anyString(), anyList()))
                .thenReturn(List.of(HOST_DATA_ID));
        when(targetSqlMapper.selectStatisticAggregate(any())).thenReturn(BigDecimal.ONE);
        // 写入后那次捕获整段失败(表单配置读不出来): 捕获是 fail-safe 的, 给回空范围
        when(moduleFormCacheService.getConfig(TARGET_FORM_KEY, ORG_ID))
                .thenReturn(targetFormConfig())
                .thenThrow(new IllegalStateException("模拟读表单配置失败"));

        StatisticHostScope scope = service.captureRelatedHosts(TARGET_FORM_KEY,
                List.of(TARGET_DATA_ID), ORG_ID);
        service.refreshAfterRelatedChange(scope, List.of(TARGET_DATA_ID));

        // 退化成「旧宿主照刷, 新宿主这次漏掉」, 而不是整次重算被带下去
        verify(targetSqlMapper, times(1)).selectStatisticAggregate(any());
        verify(self, times(1)).writeStatisticValue(any(), eq(HOST_DATA_ID), any());
    }

    @Test
    void changeDoesNothingForNullScope() {
        // 调用方在早退分支上捕不到 scope 时会把 null 传进来, 不能在这里抛
        service.refreshAfterRelatedChange(null, List.of(TARGET_DATA_ID));

        verifyNoInteractions(extStatisticMapper, targetSqlMapper, self);
    }

    @Test
    void captureReturnsEmptyScopeWhenTargetFormConfigFails() {
        givenStatisticFieldTargetsFollowRecords("field1");
        when(moduleFormCacheService.getConfig(TARGET_FORM_KEY, ORG_ID))
                .thenThrow(new IllegalStateException("模拟读表单配置失败"));

        // 捕获跑在调用方的写库之前, 它失败绝不能把写入整条带下去
        StatisticHostScope scope = service.captureRelatedHosts(TARGET_FORM_KEY,
                List.of(TARGET_DATA_ID), ORG_ID);

        assertTrue(scope.hosts().isEmpty());
    }

    @Test
    void captureReturnsEmptyScopeWhenStatisticFieldLookupFails() {
        when(extStatisticMapper.selectStatisticFields(anyString(), anyString(), any()))
                .thenThrow(new IllegalStateException("模拟反查统计字段失败"));

        // 反查是捕获的第一步, 它挂掉同样只能让这一批统计值不更新
        StatisticHostScope scope = service.captureRelatedHosts(TARGET_FORM_KEY,
                List.of(TARGET_DATA_ID), ORG_ID);

        assertTrue(scope.hosts().isEmpty());
    }

    @Test
    void hostRecordEqualityDeduplicatesIndependentParses() {
        // 两次「写入前/写入后」的捕获各自从库里解析出字段对象, 不是同一个实例。
        // 并集去重全靠 HostRecord 的值相等, 这条断言是它的看门狗。
        HostRecord before = new HostRecord(HOST_FORM_KEY, "customer",
                JSON.parseObject(JSON.toJSONString(statisticField("field1")), StatisticField.class), HOST_DATA_ID);
        HostRecord after = new HostRecord(HOST_FORM_KEY, "customer",
                JSON.parseObject(JSON.toJSONString(statisticField("field1")), StatisticField.class), HOST_DATA_ID);

        // 用与 unionHosts 同一个集合: Set.of 遇到重复会直接抛, 这里要的是「装进去了但只剩一条」
        assertEquals(1, new LinkedHashSet<>(List.of(before, after)).size());
    }

    /**
     * 目标表单的字段配置: 关联字段解析不出来会被当成「字段已删」直接跳过, 所以每个用例都要先给上。
     */
    private void givenTargetFormRelatedField() {
        when(moduleFormCacheService.getConfig(TARGET_FORM_KEY, ORG_ID)).thenReturn(targetFormConfig());
    }

    private ModuleFormConfigDTO targetFormConfig() {
        DatasourceField relatedField = new DatasourceField();
        relatedField.setId(RELATED_FIELD_ID);
        relatedField.setType(FieldType.DATA_SOURCE.name());
        ModuleFormConfigDTO config = new ModuleFormConfigDTO();
        config.setFields(List.of(relatedField));
        return config;
    }

    /**
     * 让反查命中「客户上的一个统计字段, 统计目标是跟进记录」。
     */
    private void givenStatisticFieldTargetsFollowRecords(String fieldId) {
        givenStatisticFieldTargetsFollowRecords(statisticField(fieldId));
    }

    private void givenStatisticFieldTargetsFollowRecords(StatisticField field) {
        when(extStatisticMapper.selectStatisticFields(anyString(), anyString(), any()))
                .thenReturn(List.of(statisticFieldSource(field)));
    }

    private StatisticFieldSourceDTO statisticFieldSource(String fieldId) {
        return statisticFieldSource(statisticField(fieldId));
    }

    private StatisticFieldSourceDTO statisticFieldSource(StatisticField field) {
        StatisticFieldSourceDTO source = new StatisticFieldSourceDTO();
        source.setHostFormKey(HOST_FORM_KEY);
        source.setFieldId(field.getId());
        // 走一遍序列化: 生产上 prop 是库里存的字段 JSON, 反查时按同样的方式解析回 BaseField
        source.setProp(JSON.toJSONString(field));
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

    private StatisticHostScope scopeOf(HostRecord host) {
        return new StatisticHostScope(TARGET_FORM_KEY, ORG_ID, List.of(host));
    }
}
