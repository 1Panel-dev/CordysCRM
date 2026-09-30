package cn.cordys.crm.analytics.mapper;

import cn.cordys.crm.analytics.service.AnalyticsSqlBuilder.Plan;
import org.apache.ibatis.scripting.defaults.DefaultParameterHandler;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.StatementCreatorUtils;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.PreparedStatement;
import java.sql.SQLDataException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 新增的只读聚合数据访问；原列表的 MyBatis 参数和 TypeHandler 原样绑定。 */
@Repository
public class AnalyticsQueryMapper {
    private final JdbcTemplate jdbc;

    public AnalyticsQueryMapper(DataSource dataSource) {
        this.jdbc = new JdbcTemplate(dataSource);
    }

    public List<Map<String, Object>> query(Plan plan) {
        return jdbc.execute(connection -> {
            var statement = connection.prepareStatement(plan.sql());
            try {
                statement.setQueryTimeout(10);
                var source = plan.access();
                new DefaultParameterHandler(source.statement(), source.boundSql().getParameterObject(), source.boundSql())
                        .setParameters(statement);
                int index = source.boundSql().getParameterMappings().size() + 1;
                if (plan.related() != null) {
                    var related = plan.related();
                    int offset = index - 1;
                    // 继续使用原 Mapper 的 TypeHandler，只平移关联子查询的 JDBC 参数下标。
                    var shifted = (PreparedStatement) Proxy.newProxyInstance(PreparedStatement.class.getClassLoader(),
                            new Class<?>[]{PreparedStatement.class}, (proxy, method, args) -> {
                                if (method.getName().startsWith("set") && args != null && args.length >= 2 && args[0] instanceof Integer i) {
                                    args[0] = i + offset;
                                }
                                try { return method.invoke(statement, args); }
                                catch (InvocationTargetException e) { throw e.getCause(); }
                            });
                    new DefaultParameterHandler(related.statement(), related.boundSql().getParameterObject(), related.boundSql())
                            .setParameters(shifted);
                    index += related.boundSql().getParameterMappings().size();
                }
                for (Object parameter : plan.parameters()) {
                    StatementCreatorUtils.setParameterValue(statement, index++, org.springframework.jdbc.core.SqlTypeValue.TYPE_UNKNOWN, parameter);
                }
                return statement;
            } catch (Exception e) {
                statement.close();
                throw e;
            }
        }, (PreparedStatementCallback<List<Map<String, Object>>>) statement -> {
            List<Map<String, Object>> rows = new ArrayList<>();
            var mapper = new ColumnMapRowMapper();
            try (var result = statement.executeQuery()) {
                while (result.next()) rows.add(mapper.mapRow(result, rows.size()));
            }
            // MySQL SELECT 的数值截断等错误可能只产生 warning；不把这些数字当精确结果。
            if (statement.getWarnings() != null) throw new SQLDataException("Analytics query produced SQL warnings");
            return rows;
        });
    }
}
