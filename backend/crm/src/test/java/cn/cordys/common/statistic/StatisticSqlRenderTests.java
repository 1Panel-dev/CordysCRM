package cn.cordys.common.statistic;

import cn.cordys.common.dto.condition.CombineSearch;
import cn.cordys.common.dto.condition.FilterDBCondition;
import cn.cordys.crm.clue.mapper.ExtClueMapper;
import cn.cordys.crm.contract.mapper.ExtContractInvoiceMapper;
import cn.cordys.crm.contract.mapper.ExtContractMapper;
import cn.cordys.crm.contract.mapper.ExtContractPaymentPlanMapper;
import cn.cordys.crm.contract.mapper.ExtContractPaymentRecordMapper;
import cn.cordys.crm.customer.mapper.ExtCustomerContactMapper;
import cn.cordys.crm.customer.mapper.ExtCustomerMapper;
import cn.cordys.crm.follow.mapper.ExtFollowUpPlanMapper;
import cn.cordys.crm.follow.mapper.ExtFollowUpRecordMapper;
import cn.cordys.crm.opportunity.mapper.ExtOpportunityMapper;
import cn.cordys.crm.opportunity.mapper.ExtOpportunityQuotationMapper;
import cn.cordys.crm.order.mapper.ExtOrderMapper;
import cn.cordys.crm.product.mapper.ExtProductMapper;
import cn.cordys.crm.product.mapper.ExtProductPriceMapper;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 各表单统计聚合语句的「渲染」冒烟测试: 只把 SQL 拼出来看形状, 不连数据库。
 *
 * <p>聚合语句由「本表单的条件片段 + CommonMapper 的公用片段」拼成, 拼错(片段名写错、属性没传、
 * 别名对不上、引用了没 join 的表)在编译期和启动期都不报错 —— MyBatis 只在真正执行时才炸。
 * 这里把 14 张表单的两条语句都渲染一遍, 抓的就是这一类错误, 顺带验证
 * 「语句挂在具体 Mapper 的命名空间下」这个继承绑定前提(MyBatis 先按具体接口的全限定名找语句)。</p>
 *
 * <p>不需要 Docker: 比 {@code StatisticSqlMapperTests} 快得多, 但没有数据库, 证明不了列名真实存在,
 * 两者互补。</p>
 */
class StatisticSqlRenderTests {

    private static final String COMMON_MAPPER_XML = "cn/cordys/common/mapper/CommonMapper.xml";

    /**
     * 可作为统计目标的表单, 按表单Key排列。
     *
     * <p>这是本测试唯一需要手工维护的清单, 与 {@code StatisticSqlMapperRegistry} 的键集合一一对应:
     * 漏一个就少测一张表单, 而少测的那张正是最可能配错的一张。</p>
     */
    private static final List<Class<?>> MAPPERS = List.of(
            ExtClueMapper.class,
            ExtCustomerMapper.class,
            ExtCustomerContactMapper.class,
            ExtFollowUpRecordMapper.class,
            ExtFollowUpPlanMapper.class,
            ExtOpportunityMapper.class,
            ExtProductMapper.class,
            ExtProductPriceMapper.class,
            ExtOpportunityQuotationMapper.class,
            ExtContractMapper.class,
            ExtContractInvoiceMapper.class,
            ExtContractPaymentPlanMapper.class,
            ExtContractPaymentRecordMapper.class,
            ExtOrderMapper.class);

    /**
     * 部门条件在各表单上 join 出来的部门表别名, 用来核对条件片段引用的别名与 join 用的是同一个。
     */
    private static final Pattern DEPARTMENT_JOIN = Pattern.compile("join\\s+sys_organization_user\\s+(\\w+)", Pattern.CASE_INSENSITIVE);

    private static Configuration configuration;

    /**
     * 解析各 Mapper 的 XML。
     *
     * <p>先解析 CommonMapper, 再解析各资源 Mapper: {@code <include>} 是解析期展开的,
     * 被引用的片段必须先存在于配置里。</p>
     *
     * <p>{@code XMLMapperBuilder} 遇到 {@code namespace} 会顺手把接口注册进 MapperRegistry,
     * 与生产环境 {@code @MapperScan} 的效果一致, 所以这里不需要再手工注册一次。</p>
     */
    @BeforeAll
    static void loadMapperXml() throws Exception {
        configuration = new Configuration();
        parse(COMMON_MAPPER_XML);
        for (Class<?> mapper : MAPPERS) {
            parse(mapper.getName().replace('.', '/') + ".xml");
        }
    }

    private static void parse(String resource) throws Exception {
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("找不到 Mapper XML: " + resource);
            }
            new XMLMapperBuilder(in, configuration, resource, configuration.getSqlFragments()).parse();
        }
    }

    static Stream<Class<?>> mapperClasses() {
        return MAPPERS.stream();
    }

    /**
     * 两个语句都必须挂在具体 Mapper 自己的命名空间下。
     *
     * <p>接口继承是「声明共用」, 语句仍然各写各的。MyBatis 解析方法绑定时先按具体接口的全限定名找语句,
     * 找不到才回溯父接口 —— 所以只要这条断言成立, 调用就一定绑得上; 反过来, 漏写一条语句会在
     * 第一次刷新时抛 {@code Invalid bound statement}。</p>
     */
    @ParameterizedTest
    @MethodSource("mapperClasses")
    void statementsAreBoundUnderConcreteMapperNamespace(Class<?> mapper) {
        assertTrue(configuration.hasStatement(mapper.getName() + ".selectStatisticAggregate"),
                mapper.getSimpleName() + " 缺少 selectStatisticAggregate 语句");
        assertTrue(configuration.hasStatement(mapper.getName() + ".selectStatisticHostDataIds"),
                mapper.getSimpleName() + " 缺少 selectStatisticHostDataIds 语句");
    }

    /**
     * 聚合语句: 业务关联 + 业务取值时不该碰字段值表, 自定义关联 + 自定义取值时两个 join 都要有。
     *
     * <p>同时卡住「模板 ${} 没被替换掉」这一整类错误: 只要有一个属性没传对, 渲染结果里就会留下
     * 字面量 {@code ${...}}, 数据库执行时必然报错。</p>
     */
    @ParameterizedTest
    @MethodSource("mapperClasses")
    void aggregateRendersBothStorageShapes(Class<?> mapper) {
        StatisticAggregateRequest business = request("SUM", null, null);
        business.setRelatedBusinessKey("businessKey");
        business.setStatisticBusinessKey("businessKey");
        String businessSql = sql(aggregateId(mapper), business);
        assertNoUnresolvedProperty(businessSql);
        // 两个字段都是业务字段: 关系与取值都在主表列上, 多 join 一次字段值表只会 join 出空
        assertFalse(businessSql.contains("rel."), "不应出现关联字段值表的别名 rel: " + businessSql);
        assertFalse(businessSql.contains("stat."), "不应出现取值字段值表的别名 stat: " + businessSql);
        assertTrue(businessSql.contains("sum("), "SUM 应走 sum 聚合: " + businessSql);

        // 取值是自定义字段时 statisticBusinessKey 必须为空, 否则走的是主表列那一支
        StatisticAggregateRequest custom = request("AVG", "fieldId", null);
        String customSql = sql(aggregateId(mapper), custom);
        assertNoUnresolvedProperty(customSql);
        assertTrue(customSql.contains("rel."), "关联字段是自定义字段时应 join 字段值表: " + customSql);
        assertTrue(customSql.contains("stat."), "被统计字段是自定义字段时应 join 字段值表: " + customSql);
        assertTrue(customSql.contains("avg(ifnull("), "AVG 不跳过空值时用 ifnull 把 NULL 补成 0: " + customSql);
    }

    /**
     * COUNT 数的是关联数据条数, 必须 distinct, 且完全不取值。
     */
    @ParameterizedTest
    @MethodSource("mapperClasses")
    void aggregateRendersCountWithoutValue(Class<?> mapper) {
        StatisticAggregateRequest request = request("COUNT", null, null);
        request.setRelatedBusinessKey("businessKey");
        String sql = sql(aggregateId(mapper), request);
        assertNoUnresolvedProperty(sql);
        assertTrue(sql.contains("count(distinct"), "COUNT 必须去重: " + sql);
        assertFalse(sql.contains("stat."), "COUNT 不取值, 不该 join 取值用的字段值表: " + sql);
    }

    /**
     * 部门条件: 条件片段引用的别名必须与 {@code fieldConditionJoin} join 出来的别名一致。
     *
     * <p>这是本次改造的核心证明项。改造前统计走一套通用渲染, 部门条件只能直接抛异常拒绝
     * (拼出来会是 {@code t.department_id}, 而语句里根本没有 t 这个别名); 现在复用各表单列表页的
     * 条件片段, 部门条件与列表页走同一条路径。</p>
     *
     * <p>跟进记录 / 跟进计划的条件片段把部门表的别名写死成 {@code sou}, 这条断言同时守着它 ——
     * 别名只在 join 与引用两处保持一致才成立。</p>
     *
     * <p>产品 / 价格没有负责人, 表上根本没有部门这一维(条件片段里也没有这条分支), 渲染结果里
     * 不会出现部门表 —— 靠这一点自动区分, 不给这两张表硬套一个它们没有的概念。</p>
     */
    @ParameterizedTest
    @MethodSource("mapperClasses")
    void aggregateRendersDepartmentCondition(Class<?> mapper) {
        String sql = normalize(sql(aggregateId(mapper), departmentRequest()));
        assertNoUnresolvedProperty(sql);

        Matcher matcher = DEPARTMENT_JOIN.matcher(sql);
        if (!matcher.find()) {
            return;
        }
        String alias = matcher.group(1);
        assertTrue(sql.contains(alias + ".department_id"),
                mapper.getSimpleName() + ": 部门条件引用的别名应与 join 出的别名一致(" + alias + "): " + sql);
    }

    /**
     * 上一条用例在没有部门维度的表单上会直接返回, 这里的计数保证它不是「整张表单都跳过」而静默通过。
     */
    @Test
    void departmentConditionIsRenderedForEveryFormWithOwner() {
        int rendered = 0;
        for (Class<?> mapper : MAPPERS) {
            if (DEPARTMENT_JOIN.matcher(normalize(sql(aggregateId(mapper), departmentRequest()))).find()) {
                rendered++;
            }
        }
        // 14 张表单里只有产品与价格没有负责人
        assertTrue(rendered == 12, "应有 12 张表单支持部门条件, 实际 " + rendered);
    }

    /**
     * 业务字段条件的名字必须以驼峰原样送进条件片段。
     *
     * <p>各表单的条件片段是按驼峰名匹配的({@code <when test="condition.name == 'createUser'">}),
     * 而它们的物理列名是下划线。改造前这里还有一道「驼峰转下划线」, 转了之后所有 {@code <when>} 都命中不了,
     * 条件会静默落到按字段值表查的 {@code <otherwise>} 分支上 —— 拼出来的 SQL 照样能跑, 只是结果全错。
     * 这条断言就是那道转换的墓碑。</p>
     *
     * <p>{@code createUser} 是 14 张表单上都有的业务字段, 且都映射到本表单主表的 {@code create_user} 列,
     * 所以「渲染结果里有 {@code create_user}」等价于「驼峰名命中了 when 分支」—— 落到 otherwise 时
     * 条件片段拼的是字段值表上的 {@code field_value}, 不会出现这个列名。</p>
     */
    @ParameterizedTest
    @MethodSource("mapperClasses")
    void businessFieldConditionKeepsCamelCaseName(Class<?> mapper) {
        StatisticAggregateRequest request = request("SUM", null, null);
        request.setRelatedBusinessKey("businessKey");
        request.setStatisticBusinessKey("businessKey");
        request.setScopeCondition(combine(condition("createUser", "EQUALS", "v", false, false)));

        String sql = normalize(sql(aggregateId(mapper), request));
        assertNoUnresolvedProperty(sql);
        assertTrue(sql.contains("create_user"),
                mapper.getSimpleName() + ": 驼峰条件名没命中 when 分支, 条件会落到字段值表分支: " + sql);
    }

    /**
     * 多个条件各占一张字段值表副本, 连接方式由 searchMode 决定, 且副本别名要按下标错开。
     */
    @ParameterizedTest
    @MethodSource("mapperClasses")
    void aggregateRendersMultipleConditionsWithSearchMode(Class<?> mapper) {
        StatisticAggregateRequest request = request("SUM", null, null);
        request.setRelatedBusinessKey("businessKey");
        request.setStatisticBusinessKey("businessKey");
        CombineSearch combineSearch = combine(
                condition("custom-field-a", "EQUALS", "v", true, false),
                condition("custom-field-b", "CONTAINS", "v", true, true));
        combineSearch.setSearchMode("OR");
        request.setScopeCondition(combineSearch);

        String sql = normalize(sql(aggregateId(mapper), request));
        assertNoUnresolvedProperty(sql);
        // 片段里的 join 关键字大小写不一致(left join / LEFT JOIN 都有), 断言前统一转小写
        String lower = sql.toLowerCase();
        assertTrue(lower.contains("join ") && sql.contains("_0"), "第一个条件应有自己的字段值表副本: " + sql);
        assertTrue(sql.contains("_1"), "第二个条件应有自己的字段值表副本: " + sql);
        assertTrue(sql.contains(" OR "), "searchMode=OR 应拼出 OR: " + sql);
    }

    /**
     * 宿主侧游标语句: 条件谓词在前, {@code order by} / {@code limit} 收在最后。
     *
     * <p>顺序错了就是语法错; 而且游标分页靠 {@code id > lastId}, 首页没有 lastId 时那一句要整段消失。</p>
     */
    @ParameterizedTest
    @MethodSource("mapperClasses")
    void hostDataIdsRendersCursorPage(Class<?> mapper) {
        StatisticCursorRequest firstPage = new StatisticCursorRequest();
        firstPage.setOrgId("100001");
        firstPage.setLimit(500);
        firstPage.setScopeCondition(combine(condition("custom-field-a", "EQUALS", "v", true, false)));

        String firstSql = normalize(sql(hostDataIdsId(mapper), firstPage));
        assertNoUnresolvedProperty(firstSql);
        assertTrue(firstSql.startsWith("select "), "应以 select 开头: " + firstSql);
        assertTrue(firstSql.contains(" order by "), "缺少 order by: " + firstSql);
        assertTrue(firstSql.contains(" limit ?"), "缺少 limit: " + firstSql);
        assertFalse(firstSql.contains(" > ?"), "首页不该有游标条件: " + firstSql);
        assertTrue(firstSql.indexOf("organization_id") < firstSql.indexOf("order by"),
                "组织隔离应在 order by 之前: " + firstSql);

        firstPage.setLastId("last-id");
        firstPage.setScopeCondition(new CombineSearch());
        String nextSql = normalize(sql(hostDataIdsId(mapper), firstPage));
        assertNoUnresolvedProperty(nextSql);
        assertTrue(nextSql.contains(" > ?"), "有 lastId 时应带上游标条件: " + nextSql);
        // 无条件时不该出现任何条件片段的痕迹
        assertFalse(nextSql.contains("combine_"), "无条件时不该 join 条件用的表: " + nextSql);
    }

    @Test
    void mapperListCoversEveryForm() {
        // 表单Key各对应一个 Mapper, 数量对上说明没漏; 真正的键值对应由 StatisticSqlMapperRegistry 的测试覆盖
        assertTrue(MAPPERS.size() == 14, "统计目标表单数量应为 14, 实际 " + MAPPERS.size());
    }

    private static StatisticAggregateRequest departmentRequest() {
        StatisticAggregateRequest request = request("SUM", null, null);
        request.setRelatedBusinessKey("businessKey");
        request.setStatisticBusinessKey("businessKey");
        request.setScopeCondition(combine(condition("departmentId", "EQUALS", List.of("dept"), false, false)));
        return request;
    }

    /**
     * 折叠空白: 片段拼出来的 SQL 缩进五花八门(每个 {@code <if>} 都带一层缩进),
     * 按原样断言会很脆, 折叠成单个空格后再看结构。
     */
    private static String normalize(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }

    private static String aggregateId(Class<?> mapper) {
        return mapper.getName() + ".selectStatisticAggregate";
    }

    private static String hostDataIdsId(Class<?> mapper) {
        return mapper.getName() + ".selectStatisticHostDataIds";
    }

    private static String sql(String statementId, Object request) {
        MappedStatement statement = configuration.getMappedStatement(statementId);
        // 真实调用会被 @Param 包成 ParamMap, 这里补上同样的结构
        return statement.getBoundSql(Map.of("request", request)).getSql();
    }

    private static void assertNoUnresolvedProperty(String sql) {
        assertFalse(sql.contains("${"), "SQL 里残留了没被替换的属性: " + sql);
    }

    private static StatisticAggregateRequest request(String statisticType, String statisticFieldId, String statisticBusinessKey) {
        StatisticAggregateRequest request = new StatisticAggregateRequest();
        request.setOrgId("100001");
        request.setDataId("data-id");
        request.setRelatedFieldId("related-field-id");
        request.setStatisticFieldId(statisticFieldId);
        request.setStatisticBusinessKey(statisticBusinessKey);
        request.setStatisticType(statisticType);
        request.setScopeCondition(new CombineSearch());
        return request;
    }

    private static CombineSearch combine(FilterDBCondition... conditions) {
        CombineSearch combineSearch = new CombineSearch();
        combineSearch.setConditions(List.of(conditions));
        return combineSearch;
    }

    private static FilterDBCondition condition(String name, String operator, Object value, boolean customField, boolean blob) {
        FilterDBCondition condition = new FilterDBCondition();
        condition.setName(name);
        condition.setOperator(operator);
        condition.setValue(value);
        condition.setCustomField(customField);
        condition.setBlob(blob);
        return condition;
    }
}
