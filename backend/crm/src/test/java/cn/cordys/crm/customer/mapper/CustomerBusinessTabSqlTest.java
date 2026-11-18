package cn.cordys.crm.customer.mapper;

import cn.cordys.common.dto.DeptDataPermissionDTO;
import cn.cordys.common.dto.condition.FilterCondition;
import cn.cordys.common.mapper.CommonMapper;
import cn.cordys.crm.contract.mapper.ExtContractInvoiceMapper;
import cn.cordys.crm.contract.mapper.ExtContractMapper;
import cn.cordys.crm.contract.mapper.ExtContractPaymentPlanMapper;
import cn.cordys.crm.contract.mapper.ExtContractPaymentRecordMapper;
import cn.cordys.crm.customer.dto.request.CustomerContractPaymentPlanStatisticRequest;
import cn.cordys.crm.customer.dto.request.CustomerContractPaymentRecordStatisticRequest;
import cn.cordys.crm.opportunity.mapper.ExtOpportunityMapper;
import cn.cordys.crm.order.mapper.ExtOrderMapper;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomerBusinessTabSqlTest {
    private static Configuration configuration;

    @BeforeAll
    static void loadRealBusinessQueries() throws Exception {
        configuration = new Configuration();
        for (Class<?> mapper : List.of(CommonMapper.class, ExtCustomerMapper.class, ExtOpportunityMapper.class,
                ExtContractMapper.class, ExtContractPaymentPlanMapper.class, ExtContractPaymentRecordMapper.class,
                ExtContractInvoiceMapper.class, ExtOrderMapper.class)) {
            String resource = mapper.getName().replace('.', '/') + ".xml";
            try (InputStream in = CustomerBusinessTabSqlTest.class.getClassLoader().getResourceAsStream(resource)) {
                assertNotNull(in, "找不到 Mapper XML：" + resource);
                new XMLMapperBuilder(in, configuration, resource, configuration.getSqlFragments()).parse();
            }
        }
    }

    static Stream<Arguments> businessQueriesAndScopes() {
        return Stream.of(
                new Object[] { ExtOpportunityMapper.class, "list", "o" },
                new Object[] { ExtContractMapper.class, "list", "ct" },
                new Object[] { ExtContractPaymentPlanMapper.class, "list", "cpp" },
                new Object[] { ExtContractPaymentRecordMapper.class, "list", "cpr" },
                new Object[] { ExtContractInvoiceMapper.class, "list", "ci" },
                new Object[] { ExtOrderMapper.class, "list", "so" },
                new Object[] { ExtContractMapper.class, "calculateContractStatisticByCustomerId", "ct" },
                new Object[] { ExtContractPaymentPlanMapper.class, "calculateCustomerPaymentPlanStatistic", "cpp" },
                new Object[] { ExtContractPaymentRecordMapper.class, "sumCustomerRecordAmount", "cpr" },
                new Object[] { ExtContractInvoiceMapper.class, "calculateCustomerInvoiceAmount", "ci" },
                new Object[] { ExtOrderMapper.class, "searchStatistic", "so" })
                .flatMap(query -> Stream.of("FULL", "SELF", "ROLE")
                        .map(scope -> Arguments.of(query[0], query[1], query[2], scope)));
    }

    @ParameterizedTest
    @MethodSource("businessQueriesAndScopes")
    void customerTabQueriesUseOwnerBasedScope(Class<?> mapper, String statement, String alias,
            String access) {
        DeptDataPermissionDTO scope = new DeptDataPermissionDTO();
        scope.setAll(!"SELF".equals(access));
        scope.setSelf("SELF".equals(access));
        if ("FULL".equals(access)) {
            scope.setExcludedOwnerIds(Set.of("private-owner"));
        }
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("customerId", "customer-a");
        parameters.put("userId", "reader-a");
        parameters.put("orgId", "org-a");
        parameters.put("currentUser", "reader-a");
        parameters.put("currentOrg", "org-a");
        parameters.put("source", false);
        parameters.put("approved", false);
        parameters.put("dataPermission", scope);
        parameters.put("request", new HashMap<>(Map.of("customerId", "customer-a")));

        BoundSql bound = configuration.getMappedStatement(mapper.getName() + "." + statement)
                .getBoundSql(parameters);
        String sql = bound.getSql().replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
        List<Object> values = bound.getParameterMappings().stream().map(mapping -> {
            String property = mapping.getProperty();
            return bound.hasAdditionalParameter(property) ? bound.getAdditionalParameter(property)
                    : configuration.newMetaObject(parameters).getValue(property);
        }).toList();

        assertEquals("FULL".equals(access), sql.contains(alias + ".ownernotin(?)"));
        assertEquals("SELF".equals(access), sql.contains("and" + alias + ".owner=?"));
        assertEquals("FULL".equals(access), values.contains("private-owner"));
        assertTrue(values.contains("org-a"));
        assertFalse(sql.contains("create_usernotin("), "创建人不能替代业务数据负责人");
        if ("sumCustomerRecordAmount".equals(statement)) {
            assertFalse(sql.contains("c.ownernotin("), "回款记录不能再按父合同负责人剔除");
            assertFalse(sql.contains("andc.owner=?"));
        }
    }

    static Stream<Arguments> collaborationViewQueries() {
        return Stream.of(
                new Object[] { ExtOpportunityMapper.class, "o" },
                new Object[] { ExtContractMapper.class, "ct" },
                new Object[] { ExtOrderMapper.class, "so" })
                .flatMap(query -> (query[0] == ExtOpportunityMapper.class
                        ? Stream.of("list", "searchStatistic", "chart") : Stream.of("list", "searchStatistic"))
                        .flatMap(statement -> Stream.of("COLLABORATION_VISIBLE", "ALL")
                                .map(view -> Arguments.of(query[0], statement, query[1], view))));
    }

    @ParameterizedTest
    @MethodSource("collaborationViewQueries")
    void collaborationViewUsesCurrentCustomerMembershipAndExcludesOwnAndIsolatedData(Class<?> mapper,
            String statement, String alias, String view) throws Exception {
        DeptDataPermissionDTO scope = new DeptDataPermissionDTO();
        scope.setViewId(view);
        scope.setVisible("COLLABORATION_VISIBLE".equals(view));
        scope.setAll(!scope.getVisible());
        Map<String, Object> parameters = new HashMap<>();
        Map<String, Object> request = new HashMap<>();
        if ("chart".equals(statement)) {
            Map<String, Object> axis = Map.of("businessField", true, "businessFieldName", "owner",
                    "blob", false, "aggregateMethod", "COUNT");
            request.put("categoryAxisParam", axis);
            request.put("valueAxisParam", axis);
        }
        parameters.put("request", request);
        parameters.put("userId", "reader-a");
        parameters.put("orgId", "org-a");
        parameters.put("source", false);
        parameters.put("approved", false);
        parameters.put("dataPermission", scope);
        long before = System.currentTimeMillis();
        BoundSql bound = configuration.getMappedStatement(mapper.getName() + "." + statement)
                .getBoundSql(parameters);
        long after = System.currentTimeMillis();
        CCJSqlParserUtil.parse(bound.getSql());
        String sql = bound.getSql().replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
        if (!scope.getVisible()) {
            assertFalse(sql.contains("collaboration_member"), "普通列表仍按原角色范围查询");
            assertFalse(sql.contains(alias + ".owner!=?"));
            return;
        }

        assertTrue(sql.contains("and" + alias + ".owner!=?"), "协作视图必须排除本人负责的数据");
        assertTrue(sql.contains("collaboration_customer.id=" + alias + ".customer_id"));
        assertTrue(sql.contains("collaboration_customer.organization_id=?"));
        assertTrue(sql.contains("collaboration_customer.in_shared_poolisfalse"));
        assertTrue(sql.contains("joincustomer_collaborationcollaboration_member"));
        assertTrue(sql.contains("collaboration_member.user_id=?"));
        assertTrue(sql.contains("collaboration_member.collaboration_typein('read_only','joint_follow')"));
        assertTrue(sql.contains("and(collaboration_member.expire_timeisnullorcollaboration_member.expire_time>?)"));
        assertTrue(sql.contains("notexists("), "仅客户信息成员负责的数据也必须隔离");
        assertTrue(sql.contains("isolated_member.user_id=" + alias + ".owner"));
        assertTrue(sql.contains("coalesce(collaboration_customer.owner,'')"), "客户负责人不能被成员隔离规则剔除");
        assertTrue(sql.contains("isolated_member.collaboration_type=?"));
        assertTrue(sql.contains("isolated_member.expire_time<=?"));
        assertFalse(sql.contains("and" + alias + ".owner=?"));

        long now = (Long) bound.getAdditionalParameter("collaborationCurrentTime");
        assertTrue(before <= now && now <= after);
        List<Object> values = bound.getParameterMappings().stream().map(mapping -> {
            String property = mapping.getProperty();
            return bound.hasAdditionalParameter(property) ? bound.getAdditionalParameter(property)
                    : configuration.newMetaObject(parameters).getValue(property);
        }).toList();
        assertTrue(values.containsAll(List.of("reader-a", "org-a", "COLLABORATION")));
        assertEquals(2, values.stream().filter(value -> Long.valueOf(now).equals(value)).count(),
                "访问者与记录负责人的到期判断应使用同一时间");
    }

    @Test
    void customerPaymentStatisticsUseListFilters() {
        CustomerContractPaymentPlanStatisticRequest planRequest = new CustomerContractPaymentPlanStatisticRequest();
        planRequest.setCustomerId("customer-a");
        planRequest.setContractId("contract-a");
        planRequest.setFilters(List.of(filter("planStatus", "COMPLETED")));
        String planSql = statisticSql(ExtContractPaymentPlanMapper.class, "calculateCustomerPaymentPlanStatistic",
                planRequest);
        assertTrue(planSql.contains("cpp.contract_id=?"));
        assertTrue(planSql.contains("cpp.plan_status=?"));

        CustomerContractPaymentRecordStatisticRequest recordRequest = new CustomerContractPaymentRecordStatisticRequest();
        recordRequest.setCustomerId("customer-a");
        recordRequest.setFilters(List.of(filter("recordAmount", "100")));
        assertTrue(statisticSql(ExtContractPaymentRecordMapper.class, "sumCustomerRecordAmount", recordRequest)
                .contains("cpr.record_amount=?"));
    }

    private String statisticSql(Class<?> mapper, String statement, Object request) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("request", request);
        parameters.put("userId", "reader-a");
        parameters.put("orgId", "org-a");
        return configuration.getMappedStatement(mapper.getName() + "." + statement).getBoundSql(parameters)
                .getSql().replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }

    private FilterCondition filter(String name, String value) {
        FilterCondition filter = new FilterCondition();
        filter.setName(name);
        filter.setOperator(FilterCondition.CombineConditionOperator.EQUALS.name());
        filter.setValue(value);
        return filter;
    }
}
