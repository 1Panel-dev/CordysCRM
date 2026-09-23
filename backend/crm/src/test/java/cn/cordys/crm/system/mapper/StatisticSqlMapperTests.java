package cn.cordys.crm.system.mapper;

import cn.cordys.common.dto.condition.CombineSearch;
import cn.cordys.common.dto.condition.FilterDBCondition;
import cn.cordys.common.statistic.StatisticAggregateRequest;
import cn.cordys.common.statistic.StatisticCursorRequest;
import cn.cordys.common.statistic.StatisticSqlMapper;
import cn.cordys.crm.base.BaseTest;
import cn.cordys.crm.system.service.StatisticSqlMapperRegistry;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 各表单统计聚合语句的「执行」冒烟测试: 在真实数据库上把 28 条语句各跑一遍。
 *
 * <p>与 {@code StatisticSqlRenderTests} 的分工: 那边只把 SQL 拼出来看形状(不需要库, 快);
 * 这边证明拼出来的列名、表名、别名在库里真实存在 —— 条件片段引用了没 join 的 {@code combine_0}、
 * 没建的 {@code stat}、写死别名的 {@code sou}, 都只有真正执行才会报错。</p>
 *
 * <p>遍历用的是 {@link StatisticSqlMapperRegistry#all()}, 跑的就是生产的分发路径, 不是另抄一份表单清单。</p>
 *
 * <p>全部用不存在的组织ID与数据ID, 每条语句都命中空集: 断言的是「能执行出结果」而不是具体数值,
 * 既不依赖也不产生任何测试数据。</p>
 */
class StatisticSqlMapperTests extends BaseTest {

    /**
     * 不存在的组织ID。
     *
     * <p>组织隔离是每条语句的第一道谓词, 用它把结果集钉死成空, 断言就与库里有没有数据无关了。</p>
     */
    private static final String NO_SUCH_ORG = "no-such-org";

    /**
     * 不存在的记录ID / 字段ID。
     */
    private static final String NO_SUCH_ID = "no-such-id";

    /**
     * 14 张表单都有的业务列, 用它验证「业务字段走主表列」这一支在每张表上都能执行。
     *
     * <p>刻意不用 {@code owner} 之类: 产品与价格没有负责人列, 挑一个「大多数表有」的列会让这两张表
     * 的失败看起来像语句写错了。</p>
     */
    private static final String UNIVERSAL_BUSINESS_KEY = "id";

    /**
     * 分发表里应有的表单数, 与 {@code StatisticFieldService.FORM_KEY_TABLE} 一致。
     */
    private static final int FORM_COUNT = 14;

    @Resource
    private StatisticSqlMapperRegistry statisticSqlMapperRegistry;

    /**
     * 关联字段与被统计字段都是业务字段: 两个谓词都落在主表自己的列上。
     *
     * <p>空集下 SUM 返回 null —— 不是 0。这个区别是上层「空结果」判定的依据
     * (见 {@code StatisticFieldService#writeStatisticValue}), 顺手在这里钉住。</p>
     */
    @Test
    void aggregateWithBusinessRelationAndBusinessValue() {
        int visited = 0;
        for (Map.Entry<String, StatisticSqlMapper> entry : statisticSqlMapperRegistry.all().entrySet()) {
            StatisticAggregateRequest request = request("SUM");
            request.setRelatedBusinessKey(UNIVERSAL_BUSINESS_KEY);
            request.setStatisticBusinessKey(UNIVERSAL_BUSINESS_KEY);

            assertNull(entry.getValue().selectStatisticAggregate(request),
                    entry.getKey() + ": 空集下 SUM 应返回 null");
            visited++;
        }
        assertEquals(FORM_COUNT, visited, "遍历到的表单数与分发表不一致");
    }

    /**
     * 关联字段与被统计字段都是自定义字段: 字段值表被 join 两次(一次找关联行, 一次取值)。
     */
    @Test
    void aggregateWithCustomRelationAndCustomValue() {
        int visited = 0;
        for (Map.Entry<String, StatisticSqlMapper> entry : statisticSqlMapperRegistry.all().entrySet()) {
            // relatedBusinessKey / statisticBusinessKey 都留空, 两支 join 才会都建起来
            StatisticAggregateRequest request = request("SUM");
            request.setRelatedFieldId(NO_SUCH_ID);
            request.setStatisticFieldId(NO_SUCH_ID);

            assertNull(entry.getValue().selectStatisticAggregate(request),
                    entry.getKey() + ": 没有关联数据时聚合应为空");
            visited++;
        }
        assertEquals(FORM_COUNT, visited, "遍历到的表单数与分发表不一致");
    }

    /**
     * COUNT 没有命中数据时返回 0 而不是 null, 上层据此判定空结果。
     */
    @Test
    void aggregateCountReturnsZeroWithoutData() {
        int visited = 0;
        for (Map.Entry<String, StatisticSqlMapper> entry : statisticSqlMapperRegistry.all().entrySet()) {
            StatisticAggregateRequest request = request("COUNT");
            request.setRelatedBusinessKey(UNIVERSAL_BUSINESS_KEY);

            BigDecimal value = entry.getValue().selectStatisticAggregate(request);
            assertNotNull(value, entry.getKey() + ": 空集下 COUNT 应返回 0 而不是 null");
            assertEquals(0, value.compareTo(BigDecimal.ZERO), entry.getKey() + ": 空集下 COUNT 应为 0");
            visited++;
        }
        assertEquals(FORM_COUNT, visited, "遍历到的表单数与分发表不一致");
    }

    /**
     * 统计范围带部门条件。
     *
     * <p>这是本次改造的关键用例: 改造前统计走一套通用渲染, 这条语句会拼出 {@code t.department_id}
     * 而语句里根本没有 {@code t} 这个别名, 直接 {@code Unknown column} 失败;
     * 现在部门条件与列表页共用 {@code fieldConditionJoin}, 由各表单自己的片段建 join。</p>
     *
     * <p>跟进记录 / 跟进计划把部门表别名写死成 {@code sou}, 产品 / 价格没有部门这一维
     * (会落到字段值表分支), 这三种形态都要能执行。</p>
     */
    @Test
    void aggregateWithDepartmentCondition() {
        int visited = 0;
        for (Map.Entry<String, StatisticSqlMapper> entry : statisticSqlMapperRegistry.all().entrySet()) {
            StatisticAggregateRequest request = request("SUM");
            request.setRelatedBusinessKey(UNIVERSAL_BUSINESS_KEY);
            request.setScopeCondition(combine(condition("departmentId", "EQUALS", NO_SUCH_ID)));

            assertNull(entry.getValue().selectStatisticAggregate(request),
                    entry.getKey() + ": 部门条件应能正常执行");
            visited++;
        }
        assertEquals(FORM_COUNT, visited, "遍历到的表单数与分发表不一致");
    }

    /**
     * 两个条件用 OR 连接: 条件片段要按 searchMode 选择连接词, 且两个条件各占一张字段值表副本。
     */
    @Test
    void aggregateWithTwoConditionsAndOr() {
        int visited = 0;
        for (Map.Entry<String, StatisticSqlMapper> entry : statisticSqlMapperRegistry.all().entrySet()) {
            StatisticAggregateRequest request = request("SUM");
            request.setRelatedBusinessKey(UNIVERSAL_BUSINESS_KEY);
            CombineSearch combineSearch = combine(
                    condition("createUser", "EQUALS", NO_SUCH_ID),
                    condition("createUser", "EQUALS", NO_SUCH_ID));
            combineSearch.setSearchMode("OR");
            request.setScopeCondition(combineSearch);

            assertNull(entry.getValue().selectStatisticAggregate(request),
                    entry.getKey() + ": 两个 OR 条件应能正常执行");
            visited++;
        }
        assertEquals(FORM_COUNT, visited, "遍历到的表单数与分发表不一致");
    }

    /**
     * 自定义大字段条件: 走的是字段值表的 blob 副本, 而不是普通字段值表。
     */
    @Test
    void aggregateWithBlobCustomFieldCondition() {
        int visited = 0;
        for (Map.Entry<String, StatisticSqlMapper> entry : statisticSqlMapperRegistry.all().entrySet()) {
            StatisticAggregateRequest request = request("SUM");
            request.setRelatedBusinessKey(UNIVERSAL_BUSINESS_KEY);
            FilterDBCondition condition = condition(NO_SUCH_ID, "CONTAINS", NO_SUCH_ID);
            condition.setCustomField(true);
            condition.setBlob(true);
            request.setScopeCondition(combine(condition));

            assertNull(entry.getValue().selectStatisticAggregate(request),
                    entry.getKey() + ": 大字段条件应能正常执行");
            visited++;
        }
        assertEquals(FORM_COUNT, visited, "遍历到的表单数与分发表不一致");
    }

    /**
     * 宿主侧游标语句带部门条件: 更新范围 = 「符合条件」时走的就是这条。
     */
    @Test
    void hostDataIdsWithDepartmentCondition() {
        int visited = 0;
        for (Map.Entry<String, StatisticSqlMapper> entry : statisticSqlMapperRegistry.all().entrySet()) {
            StatisticCursorRequest request = cursorRequest(NO_SUCH_ORG, null);
            request.setScopeCondition(combine(condition("departmentId", "EQUALS", NO_SUCH_ID)));

            List<String> ids = entry.getValue().selectStatisticHostDataIds(request);
            assertNotNull(ids, entry.getKey() + ": 游标语句应返回空列表而不是 null");
            assertTrue(ids.isEmpty(), entry.getKey() + ": 不存在的组织不该有数据");
            visited++;
        }
        assertEquals(FORM_COUNT, visited, "遍历到的表单数与分发表不一致");
    }

    /**
     * 宿主侧游标语句不带条件: 更新范围 = 「全部计算」, 条件片段整段短路。
     *
     * <p>这里用真实组织跑, 因为「无条件」正是唯一可能返回数据的形态; 断言不管数据多少都成立 ——
     * 重点是这条语句在没有任何条件时也能执行, 且页大小生效、游标指向的结果按主键递增
     * (翻页靠 {@code id > lastId}, 顺序错了会漏数据或死循环)。</p>
     */
    @Test
    void hostDataIdsWithoutCondition() {
        int visited = 0;
        for (Map.Entry<String, StatisticSqlMapper> entry : statisticSqlMapperRegistry.all().entrySet()) {
            StatisticCursorRequest request = cursorRequest(DEFAULT_ORGANIZATION_ID, null);
            request.setLimit(5);
            // 无条件时也要给一个非空的空条件, 片段里的判空才会短路
            request.setScopeCondition(new CombineSearch());

            List<String> ids = entry.getValue().selectStatisticHostDataIds(request);
            assertNotNull(ids, entry.getKey() + ": 游标语句应返回空列表而不是 null");
            assertTrue(ids.size() <= 5, entry.getKey() + ": 页大小没有生效");
            // 翻页靠 id > lastId, 同一页里出现重复主键会让游标原地打转
            assertEquals(ids.size(), new HashSet<>(ids).size(), entry.getKey() + ": 游标结果不该有重复主键");
            visited++;
        }
        assertEquals(FORM_COUNT, visited, "遍历到的表单数与分发表不一致");
    }

    /**
     * 一个统计类型为 SUM、条件为空的聚合请求。
     */
    private static StatisticAggregateRequest request(String statisticType) {
        StatisticAggregateRequest request = new StatisticAggregateRequest();
        request.setOrgId(NO_SUCH_ORG);
        request.setDataId(NO_SUCH_ID);
        request.setStatisticType(statisticType);
        request.setScopeCondition(new CombineSearch());
        return request;
    }

    private static StatisticCursorRequest cursorRequest(String orgId, String lastId) {
        StatisticCursorRequest request = new StatisticCursorRequest();
        request.setOrgId(orgId);
        request.setLastId(lastId);
        request.setLimit(500);
        return request;
    }

    private static CombineSearch combine(FilterDBCondition... conditions) {
        CombineSearch combineSearch = new CombineSearch();
        combineSearch.setConditions(List.of(conditions));
        return combineSearch;
    }

    private static FilterDBCondition condition(String name, String operator, Object value) {
        FilterDBCondition condition = new FilterDBCondition();
        condition.setName(name);
        condition.setOperator(operator);
        condition.setValue(value);
        return condition;
    }
}
