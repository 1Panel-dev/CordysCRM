package cn.cordys.crm.customer.mapper;

import cn.cordys.common.dto.DeptDataPermissionDTO;
import cn.cordys.common.dto.condition.FilterCondition;
import cn.cordys.common.mapper.CommonMapper;
import cn.cordys.crm.contract.mapper.ExtContractInvoiceMapper;
import cn.cordys.crm.contract.mapper.ExtContractMapper;
import cn.cordys.crm.contract.mapper.ExtContractPaymentPlanMapper;
import cn.cordys.crm.contract.mapper.ExtContractPaymentRecordMapper;
import cn.cordys.crm.contract.dto.request.ContractPaymentRecordPageRequest;
import cn.cordys.crm.customer.dto.request.CustomerContractPaymentPlanPageRequest;
import cn.cordys.crm.opportunity.mapper.ExtOpportunityMapper;
import cn.cordys.crm.order.mapper.ExtOrderMapper;
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

    @Test
    void customerPaymentStatisticsUseListFilters() {
        CustomerContractPaymentPlanPageRequest planRequest = new CustomerContractPaymentPlanPageRequest();
        planRequest.setCustomerId("customer-a");
        planRequest.setFilters(List.of(filter("planStatus", "COMPLETED")));
        assertTrue(statisticSql(ExtContractPaymentPlanMapper.class, "calculateCustomerPaymentPlanStatistic", planRequest)
                .contains("cpp.plan_status=?"));

        ContractPaymentRecordPageRequest recordRequest = new ContractPaymentRecordPageRequest();
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
