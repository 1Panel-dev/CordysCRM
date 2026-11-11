package cn.cordys.crm.customer.mapper;

import cn.cordys.common.mapper.CommonMapper;
import cn.cordys.crm.contract.mapper.ExtContractPaymentRecordMapper;
import cn.cordys.crm.follow.mapper.ExtFollowUpPlanMapper;
import cn.cordys.crm.follow.mapper.ExtFollowUpRecordMapper;
import net.sf.jsqlparser.expression.Expression;
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
                ExtContractPaymentRecordMapper.class, ExtFollowUpRecordMapper.class, ExtFollowUpPlanMapper.class)) {
            String resource = mapper.getName().replace('.', '/') + ".xml";
            try (InputStream in = CustomerCollaborationIsolationSqlTest.class.getClassLoader()
                    .getResourceAsStream(resource)) {
                assertNotNull(in, "找不到 Mapper XML：" + resource);
                new XMLMapperBuilder(in, configuration, resource, configuration.getSqlFragments()).parse();
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "payment", "record", "plan" })
    void relatedQueriesIsolateOnlyEffectiveCustomerInformationMembers(String entry) throws Exception {
        String statement = switch (entry) {
        case "payment" -> ExtContractPaymentRecordMapper.class.getName() + ".sumCustomerRecordAmount";
        case "record" -> ExtFollowUpRecordMapper.class.getName() + ".selectList";
        default -> ExtFollowUpPlanMapper.class.getName() + ".selectList";
        };
        Map<String, Object> parameters = Map.of("customerId", "customer-a", "userId", "receiver-a", "orgId", "org-a",
                "resourceType", "CUSTOMER", "type", "CUSTOMER",
                "request", Map.of("sourceId", "customer-a", "myPlan", false));
        long before = System.currentTimeMillis();
        BoundSql boundSql = configuration.getMappedStatement(statement).getBoundSql(parameters);
        long after = System.currentTimeMillis();
        long now = assertInstanceOf(Long.class, boundSql.getAdditionalParameter("collaborationCurrentTime"));
        assertTrue(before <= now && now <= after);
        List<Object> values = boundSql.getParameterMappings().stream().map(mapping -> {
            String property = mapping.getProperty();
            return boundSql.hasAdditionalParameter(property) ? boundSql.getAdditionalParameter(property)
                    : configuration.newMetaObject(parameters).getValue(property);
        }).toList();
        int occurrences = entry.equals("payment") ? 2 : 1;
        assertEquals(occurrences, values.stream().filter(value -> Objects.equals(value, now)).count(),
                "同一查询的全部隔离条件应使用相同时间绑定");
        assertEquals(occurrences, values.stream().filter("COLLABORATION"::equals).count());
        assertTrue(values.containsAll(List.of("customer-a", "receiver-a", "org-a")));

        List<OrExpression> isolationConditions = new ArrayList<>();
        new TablesNamesFinder<Void>() {
            @Override
            public <S> Void visit(OrExpression expression, S context) {
                if (expression.getLeftExpression() instanceof EqualsTo equals
                        && equals.getLeftExpression() instanceof Column column
                        && column.getColumnName().equals("collaboration_type")) {
                    isolationConditions.add(expression);
                }
                return super.visit(expression, context);
            }
        }.getTables(CCJSqlParserUtil.parse(boundSql.getSql()));
        assertEquals(occurrences, isolationConditions.size());
        for (Expression condition : isolationConditions) {
            assertTrue(isolated(condition, "COLLABORATION", null, values));
            assertTrue(isolated(condition, "COLLABORATION", now + 1, values));
            assertFalse(isolated(condition, "READ_ONLY", now - 1, values));
            assertFalse(isolated(condition, "JOINT_FOLLOW", null, values));
            assertFalse(isolated(condition, "JOINT_FOLLOW", now + 1, values));
            assertTrue(isolated(condition, "JOINT_FOLLOW", now, values));
            assertTrue(isolated(condition, "JOINT_FOLLOW", now - 1, values));
        }

        String sql = boundSql.getSql().replaceAll("\\s+", "");
        if (entry.equals("payment")) {
            assertTrue(sql.contains("isolated_customer.organization_id=?"));
            assertTrue(sql.contains("isolated_customer.owner=?"));
            assertTrue(sql.contains("isolated_member.user_id!=?"));
            assertTrue(sql.contains("isolated_member.user_id=cpr.owner"));
            assertTrue(sql.contains("isolated_member.user_id=c.owner"));
        } else {
            String alias = entry.equals("record") ? "fur" : "fup";
            assertTrue(sql.contains("AND(" + alias + ".owner=?ORNOTEXISTS("));
            assertTrue(sql.contains("privacy_customer.organization_id=?"));
            assertTrue(sql.contains("cc.user_id!=COALESCE(privacy_customer.owner,'')"));
            assertTrue(sql.contains("cc.user_idIN(" + alias + ".owner,?)"));
        }
    }

    private static boolean isolated(Expression expression, String type, Long expires, List<Object> values) {
        if (expression instanceof ParenthesedExpressionList<?> parenthesed) {
            return isolated(parenthesed.getFirst(), type, expires, values);
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
