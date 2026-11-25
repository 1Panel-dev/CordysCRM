package cn.cordys.crm.customer.mapper;

import cn.cordys.common.dto.DeptDataPermissionDTO;
import cn.cordys.common.mapper.CommonMapper;
import cn.cordys.crm.customer.dto.request.CustomerPageRequest;
import cn.cordys.crm.follow.mapper.ExtFollowUpPlanMapper;
import cn.cordys.crm.follow.mapper.ExtFollowUpRecordMapper;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.JdbcParameter;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.IsNullExpression;
import net.sf.jsqlparser.expression.operators.relational.MinorThanEquals;
import net.sf.jsqlparser.expression.operators.relational.ParenthesedExpressionList;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.util.TablesNamesFinder;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 解析真实关联查询，验证到期协作的隔离语义及组织、负责人和本人约束。
 */
class CustomerCollaborationIsolationSqlTest {
    private static Configuration configuration;

    @BeforeAll
    static void loadMapperXml() throws Exception {
        configuration = new Configuration();
        for (Class<?> mapper : List.of(CommonMapper.class, ExtCustomerMapper.class,
                ExtFollowUpRecordMapper.class, ExtFollowUpPlanMapper.class)) {
            String resource = mapper.getName().replace('.', '/') + ".xml";
            try (InputStream in = CustomerCollaborationIsolationSqlTest.class.getClassLoader()
                    .getResourceAsStream(resource)) {
                assertNotNull(in, "找不到 Mapper XML：" + resource);
                new XMLMapperBuilder(in, configuration, resource, configuration.getSqlFragments()).parse();
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "record", "plan" })
    void relatedFollowQueriesUseMergedOwnerScope(String entry) throws Exception {
        String mapper = entry.equals("record") ? ExtFollowUpRecordMapper.class.getName()
                : ExtFollowUpPlanMapper.class.getName();
        String alias = entry.equals("record") ? "fur" : "fup";
        DeptDataPermissionDTO scope = new DeptDataPermissionDTO();
        scope.setDeptIds(java.util.Set.of("dept-a"));
        Map<String, Object> request = Map.of("sourceId", "customer-a", "myPlan", false, "customerTabScope", scope);
        Map<String, Object> parameters = Map.of("userId", "reader-a", "orgId", "org-a", "resourceType", "CUSTOMER",
                "type", "CUSTOMER", "request", request);
        BoundSql boundSql = configuration.getMappedStatement(mapper + ".selectList").getBoundSql(parameters);
        CCJSqlParserUtil.parse(boundSql.getSql());
        String sql = boundSql.getSql().replaceAll("\\s+", "");
        assertTrue(sql.contains(alias + ".customer_id=?"));
        assertTrue(sql.contains(alias + ".owner=?OREXISTS("));
        assertTrue(sql.contains("scope_user.organization_id=?"));
        assertTrue(sql.contains("scope_user.department_idIN(?)"));

        scope.setDeptIds(java.util.Set.of());
        scope.setAll(true);
        scope.setExcludedOwnerIds(java.util.Set.of("outside-dept"));
        String unrestricted = configuration.getMappedStatement(mapper + ".selectList").getBoundSql(parameters)
                .getSql().replaceAll("\\s+", "");
        assertTrue(unrestricted.contains(alias + ".ownerNOTIN(?)"));
        assertFalse(unrestricted.contains("scope_user"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "READ_ONLY", "JOINT_FOLLOW" })
    void customerListReturnsEffectiveCollaborationType(String type) throws Exception {
        CustomerPageRequest request = new CustomerPageRequest();
        request.setTransition(false);
        DeptDataPermissionDTO scope = new DeptDataPermissionDTO();
        scope.setVisible(true);
        Map<String, Object> parameters = Map.of("request", request, "orgId", "org-a", "userId", "member-a",
                "dataPermission", scope);
        long before = System.currentTimeMillis();
        BoundSql boundSql = configuration.getMappedStatement(ExtCustomerMapper.class.getName() + ".list")
                .getBoundSql(parameters);
        long after = System.currentTimeMillis();
        long now = assertInstanceOf(Long.class, boundSql.getAdditionalParameter("collaborationCurrentTime"));
        assertTrue(before <= now && now <= after);
        List<Object> values = boundSql.getParameterMappings().stream().map(mapping -> {
            String property = mapping.getProperty();
            return boundSql.hasAdditionalParameter(property) ? boundSql.getAdditionalParameter(property)
                    : configuration.newMetaObject(parameters).getValue(property);
        }).toList();
        List<Function> effectiveTypes = new ArrayList<>();
        new TablesNamesFinder<Void>() {
            @Override
            public <S> Void visit(Function function, S context) {
                if ("IF".equalsIgnoreCase(function.getName()) && function.getParameters().size() == 3
                        && function.getParameters().get(2) instanceof Column column
                        && "collaboration_type".equals(column.getColumnName())) {
                    effectiveTypes.add(function);
                }
                return super.visit(function, context);
            }
        }.getTables(CCJSqlParserUtil.parse(boundSql.getSql()));
        assertEquals(1, effectiveTypes.size());
        List<?> branches = effectiveTypes.getFirst().getParameters();
        Expression expired = (Expression) branches.getFirst();
        assertFalse(isolated(expired, type, null, values));
        assertFalse(isolated(expired, type, now + 1, values));
        assertTrue(isolated(expired, type, now, values));
        assertTrue(isolated(expired, type, now - 1, values));
        assertEquals("COLLABORATION", value((Expression) branches.get(1), type, now, values));
        assertEquals(type, value((Expression) branches.get(2), type, now + 1, values));
    }

    @ParameterizedTest
    @ValueSource(strings = { "list", "chart" })
    void customerCollaborationViewKeepsMembershipAndExcludesCustomerOwner(String statement) throws Exception {
        DeptDataPermissionDTO scope = new DeptDataPermissionDTO();
        scope.setVisible(true);
        scope.setViewId("CUSTOMER_COLLABORATION");
        Map<String, Object> request = new HashMap<>();
        request.put("transition", false);
        Map<String, Object> axis = Map.of("businessField", true, "businessFieldName", "owner",
                "blob", false, "aggregateMethod", "COUNT");
        request.put("categoryAxisParam", axis);
        request.put("valueAxisParam", axis);
        Map<String, Object> parameters = Map.of("request", request, "orgId", "org-a", "userId", "reader-a",
                "dataPermission", scope);
        BoundSql bound = configuration.getMappedStatement(ExtCustomerMapper.class.getName() + "." + statement)
                .getBoundSql(parameters);
        CCJSqlParserUtil.parse(bound.getSql());
        String sql = bound.getSql().replaceAll("\\s+", "");

        assertTrue(sql.contains("joincustomer_collaborationcconc.id=cc.customer_id"));
        assertTrue(sql.contains("cc.user_id=?"), "协作关系解除后客户应立即离开该视图");
        assertTrue(sql.contains("c.organization_id=?"));
        assertTrue(sql.contains("c.in_shared_poolisfalse"));
        assertTrue(sql.contains("c.owner!=?"), "本人负责的客户归我的数据视图");
        assertFalse(sql.contains("cc.collaboration_typeIN"), "客户协作视图包含仅客户信息成员");
        assertFalse(sql.contains("andc.owner=?"));
    }

    private static boolean isolated(Expression expression, String type, Long expires, List<Object> values) {
        if (expression instanceof ParenthesedExpressionList<?> parenthesized) {
            return isolated(parenthesized.getFirst(), type, expires, values);
        }
        if (expression instanceof AndExpression and) {
            return isolated(and.getLeftExpression(), type, expires, values)
                    && isolated(and.getRightExpression(), type, expires, values);
        }
        if (expression instanceof OrExpression or) {
            return isolated(or.getLeftExpression(), type, expires, values)
                    || isolated(or.getRightExpression(), type, expires, values);
        }
        if (expression instanceof EqualsTo equals) {
            return Objects.equals(value(equals.getLeftExpression(), type, expires, values),
                    value(equals.getRightExpression(), type, expires, values));
        }
        if (expression instanceof IsNullExpression isNull) {
            return isNull.isNot() != (value(isNull.getLeftExpression(), type, expires, values) == null);
        }
        MinorThanEquals comparison = assertInstanceOf(MinorThanEquals.class, expression);
        Object left = value(comparison.getLeftExpression(), type, expires, values);
        return left != null && (Long) left <= (Long) value(comparison.getRightExpression(), type, expires, values);
    }

    private static Object value(Expression expression, String type, Long expires, List<Object> values) {
        if (expression instanceof JdbcParameter parameter) {
            return values.get(parameter.getIndex() - 1);
        }
        if (expression instanceof StringValue literal) {
            return literal.getValue();
        }
        Column column = assertInstanceOf(Column.class, expression);
        return switch (column.getColumnName()) {
        case "collaboration_type" -> type;
        case "expire_time" -> expires;
        default -> throw new AssertionError("隔离条件引用了意外列：" + column);
        };
    }
}
