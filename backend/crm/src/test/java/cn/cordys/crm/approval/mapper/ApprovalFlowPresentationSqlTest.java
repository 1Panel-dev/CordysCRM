package cn.cordys.crm.approval.mapper;

import cn.cordys.common.dto.DeptDataPermissionDTO;
import cn.cordys.common.dto.condition.CombineSearch;
import cn.cordys.common.dto.condition.FilterDBCondition;
import cn.cordys.common.mapper.CommonMapper;
import cn.cordys.crm.clue.dto.request.CluePageRequest;
import cn.cordys.crm.clue.mapper.ExtClueMapper;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 校验当前业务状态查询的组织、表单及池内外隔离，不依赖数据库环境。
 */
class ApprovalFlowPresentationSqlTest {

    private static Configuration configuration;

    @BeforeAll
    static void loadMapperXml() throws Exception {
        configuration = new Configuration();
        for (Class<?> mapper : List.of(CommonMapper.class, ExtApprovalInstanceMapper.class, ExtClueMapper.class)) {
            String resource = mapper.getName().replace('.', '/') + ".xml";
            try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource)) {
                assertNotNull(in, "找不到 Mapper XML：" + resource);
                new XMLMapperBuilder(in, configuration, resource, configuration.getSqlFragments()).parse();
            }
        }
    }

    @Test
    void clueApprovingQueryReusesTheFilteredListScope() {
        FilterDBCondition viewFilter = new FilterDBCondition();
        viewFilter.setName("clue-source");
        viewFilter.setCustomField(true);
        viewFilter.setOperator("EQUALS");
        viewFilter.setValue("referral");
        CombineSearch viewSearch = new CombineSearch();
        viewSearch.setConditions(List.of(viewFilter));

        CluePageRequest request = new CluePageRequest();
        request.setViewId("view-a");
        request.setViewCombineSearch(viewSearch);
        request.setKeyword("keyword-a");
        DeptDataPermissionDTO dataPermission = new DeptDataPermissionDTO();
        dataPermission.setViewId("DEPARTMENT");
        dataPermission.setDeptIds(Set.of("department-a"));

        Map<String, Object> parameters = Map.of("request", request, "orgId", "org-a",
                "userId", "user-a", "dataPermission", dataPermission, "source", false);
        String mapper = ExtClueMapper.class.getName();
        BoundSql listSql = configuration.getMappedStatement(mapper + ".list").getBoundSql(parameters);
        BoundSql existsSql = configuration.getMappedStatement(mapper + ".existsApprovingInList").getBoundSql(parameters);
        String list = listSql.getSql().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
        String listScope = list.substring(list.indexOf("from `clue` c"), list.lastIndexOf("order by")).trim();

        assertEquals("select exists ( select 1 " + listScope + " and c.approval_status = 'approving' )",
                existsSql.getSql().replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT));
        assertEquals(List.of("clue-source", "department-a", "org-a", "keyword-a", "keyword-a", "referral"),
                existsSql.getParameterMappings().stream().map(mapping -> {
                    String property = mapping.getProperty();
                    return existsSql.hasAdditionalParameter(property) ? existsSql.getAdditionalParameter(property)
                            : configuration.newMetaObject(parameters).getValue(property);
                }).toList());
    }

    @ParameterizedTest
    @ValueSource(strings = { "opportunity", "opportunity_quotation", "contract",
            "contract_invoice", "sales_order" })
    void builtInFormsOnlyCheckCurrentApprovingResourcesInTheOrganization(String table) {
        BoundSql sql = boundSql(table, "form-a", "pool-a");

        assertEquals("SELECT EXISTS ( SELECT 1 FROM " + table
                        + " WHERE organization_id = ? AND approval_status = 'APPROVING' )",
                sql.getSql().replaceAll("\\s+", " ").trim());
        assertEquals(List.of("organizationId"),
                sql.getParameterMappings().stream().map(mapping -> mapping.getProperty()).toList());
        assertEquals("org-a", ((Map<?, ?>) sql.getParameterObject()).get("organizationId"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "clue", "customer" })
    void cluesAndCustomersExcludeSharedPoolResources(String table) {
        BoundSql sql = boundSql(table, table, "pool-a");

        assertEquals("SELECT EXISTS ( SELECT 1 FROM " + table
                        + " WHERE organization_id = ? AND approval_status = 'APPROVING'"
                        + " AND in_shared_pool IS FALSE )",
                sql.getSql().replaceAll("\\s+", " ").trim());
        assertEquals(List.of("organizationId"),
                sql.getParameterMappings().stream().map(mapping -> mapping.getProperty()).toList());
        assertEquals("org-a", ((Map<?, ?>) sql.getParameterObject()).get("organizationId"));
    }

    @ParameterizedTest
    @CsvSource({ "clue, cluePool", "customer, customerOpenSea" })
    void poolFormsOnlyCheckSharedPoolResources(String table, String formKey) {
        BoundSql sql = boundSql(table, formKey, null);

        assertEquals("SELECT EXISTS ( SELECT 1 FROM " + table
                        + " WHERE organization_id = ? AND approval_status = 'APPROVING'"
                        + " AND in_shared_pool IS TRUE )",
                sql.getSql().replaceAll("\\s+", " ").trim());
        assertEquals(List.of("organizationId"),
                sql.getParameterMappings().stream().map(mapping -> mapping.getProperty()).toList());
        assertEquals("org-a", ((Map<?, ?>) sql.getParameterObject()).get("organizationId"));
    }

    @ParameterizedTest
    @CsvSource({ "clue, cluePool, pool-a", "clue, cluePool, pool-b",
            "customer, customerOpenSea, pool-a", "customer, customerOpenSea, pool-b" })
    void poolFormsBindTheSelectedPoolId(String table, String formKey, String poolId) {
        BoundSql sql = boundSql(table, formKey, poolId);

        assertEquals("SELECT EXISTS ( SELECT 1 FROM " + table
                        + " WHERE organization_id = ? AND approval_status = 'APPROVING'"
                        + " AND in_shared_pool IS TRUE AND pool_id = ? )",
                sql.getSql().replaceAll("\\s+", " ").trim());
        assertEquals(List.of("organizationId", "poolId"),
                sql.getParameterMappings().stream().map(mapping -> mapping.getProperty()).toList());
        Map<?, ?> parameters = (Map<?, ?>) sql.getParameterObject();
        assertEquals(List.of("org-a", poolId),
                sql.getParameterMappings().stream().map(mapping -> parameters.get(mapping.getProperty())).toList());
    }

    @ParameterizedTest
    @ValueSource(strings = { "form-a", "form-'b" })
    void customFormsAlsoBindTheSpecificFormId(String formKey) {
        BoundSql sql = boundSql("custom_form_data", formKey, "pool-a");

        assertEquals("SELECT EXISTS ( SELECT 1 FROM custom_form_data WHERE organization_id = ?"
                        + " AND approval_status = 'APPROVING' AND custom_form_id = ? )",
                sql.getSql().replaceAll("\\s+", " ").trim());
        assertEquals(List.of("organizationId", "formKey"),
                sql.getParameterMappings().stream().map(mapping -> mapping.getProperty()).toList());
        Map<?, ?> parameters = (Map<?, ?>) sql.getParameterObject();
        assertEquals(List.of("org-a", formKey),
                sql.getParameterMappings().stream().map(mapping -> parameters.get(mapping.getProperty())).toList());
    }

    private BoundSql boundSql(String table, String formKey, String poolId) {
        Map<String, Object> parameters = new HashMap<>(Map.of("sourceTable", table,
                "organizationId", "org-a", "formKey", formKey));
        parameters.put("poolId", poolId);
        return configuration.getMappedStatement(ExtApprovalInstanceMapper.class.getName() + ".existsApprovingResource")
                .getBoundSql(parameters);
    }
}
