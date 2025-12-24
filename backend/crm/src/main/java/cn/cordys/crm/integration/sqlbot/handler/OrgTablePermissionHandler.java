package cn.cordys.crm.integration.sqlbot.handler;

import cn.cordys.crm.integration.sqlbot.dto.FieldDTO;
import cn.cordys.crm.integration.sqlbot.dto.TableDTO;
import cn.cordys.crm.integration.sqlbot.dto.TableHandleParam;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import java.text.MessageFormat;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 处理组织下的表
 */
public class OrgTablePermissionHandler implements TablePermissionHandler {
    public static final String ORG_TABLE_SQL_TEMPLATE = """
            select {0}
            from {1} c
            where c.organization_id = ''{2}''
            """;

    public void handleTable(TableDTO table, TableHandleParam tableHandleParam) {
        String sql = getOrgTableSql(tableHandleParam, table.getFields());
        table.setSql(sql);
    }

    protected String getOrgTableSql(TableHandleParam tableHandleParam, List<FieldDTO> sqlBotFields) {
        return MessageFormat.format(ORG_TABLE_SQL_TEMPLATE,
                getSelectSystemFileSql(sqlBotFields),
                tableHandleParam.getTableInfo().getTableName(),
                tableHandleParam.getOrgId());
    }

    protected String getSelectSystemFileSql(List<FieldDTO> sqlBotFields) {
        StringBuilder selectSqlBuilder = new StringBuilder();
        for (int i = 0; i < sqlBotFields.size(); i++) {
            FieldDTO sqlBotField = sqlBotFields.get(i);
            String sql = getSelectSystemFileSql(sqlBotField);
            if (i != 0) {
                // 不是第一个字段，添加逗号
                selectSqlBuilder.append(",");
            }
            selectSqlBuilder.append("\n");
            selectSqlBuilder.append(sql);
        }
        return selectSqlBuilder.toString();
    }

    protected String getSelectSystemFileSql(FieldDTO sqlBotField) {
        return getDefaultFieldSql(sqlBotField, null);
    }

    protected String getSelectSystemFileSql(FieldDTO sqlBotField, String prefix) {
        return getDefaultFieldSql(sqlBotField, prefix);
    }

    protected String getDefaultFieldSql(FieldDTO sqlBotField) {
        return getDefaultFieldSql(sqlBotField, null);
    }

    protected String getDefaultFieldSql(FieldDTO sqlBotField, String prefix) {
        String fieldName = sqlBotField.getName();
        if (Strings.CS.endsWith(fieldName, "_time")) {
            // long 类型替换为 datetime 类型
            sqlBotField.setType("varchar(50)");
            sqlBotField.setComment(sqlBotField.getComment() + ",时间格式：%Y-%m-%d %H:%i:%s");
            return getTimeFieldSql(fieldName, prefix);
        } else if (Strings.CS.endsWith(fieldName, "_date")) {
            // long 类型替换为 date 类型
            sqlBotField.setType("varchar(50)");
            sqlBotField.setComment(sqlBotField.getComment() + ",日期格式：%Y-%m-%d");
            return getDateFieldSql(fieldName, prefix);
        } else if (Strings.CS.endsWith(fieldName, "_user") || Strings.CS.equalsAny(fieldName, "follower")) {
            return getUserFieldSql(fieldName, prefix);
        } else {
            if (StringUtils.isBlank(prefix)) {
                return "c." + fieldName;
            } else {
                return prefix + "." + fieldName;
            }
        }
    }

    private String getUserFieldSql(String fieldName, String prefix) {
        if (StringUtils.isBlank(prefix)) {
            return String.format("(select su.name from sys_user su where su.id = c.%s limit 1) as %s", fieldName, fieldName);
        } else {
            return String.format("(select su.name from sys_user su where su.id = %s.%s limit 1) as %s", prefix, fieldName, fieldName);
        }
    }

    private String getTimeFieldSql(String fieldName, String prefix) {
        if (StringUtils.isBlank(prefix)) {
            return MessageFormat.format("DATE_FORMAT(FROM_UNIXTIME(c.{0} / 1000),''%Y-%m-%d %H:%i:%s'') as {1}", fieldName, fieldName);
        } else {
            return MessageFormat.format("DATE_FORMAT(FROM_UNIXTIME({0}.{1} / 1000),''%Y-%m-%d %H:%i:%s'') as {2}", prefix, fieldName, fieldName);
        }
    }

    private String getDateFieldSql(String fieldName, String prefix) {
        if (StringUtils.isBlank(prefix)) {
            return MessageFormat.format("DATE_FORMAT(FROM_UNIXTIME(c.{0} / 1000),''%Y-%m-%d'') as {1}", fieldName, fieldName);
        } else {
            return MessageFormat.format("DATE_FORMAT(FROM_UNIXTIME({0}.{1} / 1000),''%Y-%m-%d'') as {2}", prefix, fieldName, fieldName);
        }
    }

    /**
     * 过滤系统字段
     *
     * @param fields
     * @param disabledFieldNames
     *
     * @return
     */
    protected List<FieldDTO> filterSystemFields(List<FieldDTO> fields, Set<String> disabledFieldNames) {
        return fields
                .stream()
                .filter(field -> !disabledFieldNames.contains(field.getName()))
                .collect(Collectors.toList());
    }

    /**
     * 获取选项字段查询的SQL
     * 示例：
     * CASE c.stage
     * WHEN 'SUCCESS' THEN '成功'
     * WHEN 'FAIL' THEN '失败'
     * ELSE ''
     * END AS stage_name
     *
     * @param stream
     * @param fieldName
     *
     * @return
     */
    protected <T> String getSystemOptionFileSql(Stream<T> stream, Function<T, String> getKeyFunc, Function<T, String> getNameFunc, String fieldName) {
        return getSystemOptionFileSql(stream, getKeyFunc, getNameFunc, fieldName, null);
    }

    /**
     * 获取选项字段查询的SQL
     * 示例：
     * CASE c.stage
     * WHEN 'SUCCESS' THEN '成功'
     * WHEN 'FAIL' THEN '失败'
     * ELSE ''
     * END AS stage_name
     *
     * @param stream
     * @param fieldName
     * @param prefix
     *
     * @return
     */
    protected <T> String getSystemOptionFileSql(Stream<T> stream, Function<T, String> getKeyFunc, Function<T, String> getNameFunc, String fieldName, String prefix) {
        StringBuilder sqlBuilder = new StringBuilder();
        if (StringUtils.isBlank(prefix)) {
            sqlBuilder.append("case c.").append(fieldName);
        } else {
            sqlBuilder.append("case ").append(prefix).append(".").append(fieldName);
        }
        stream.forEach(clueStatus -> sqlBuilder.append(String.format(" when '%s' then '%s'", getKeyFunc.apply(clueStatus), getNameFunc.apply(clueStatus))));
        sqlBuilder.append(" else '' end as ").append(fieldName);
        return sqlBuilder.toString();
    }

    protected FieldDTO getDepartmentNameField() {
        FieldDTO fieldDTO = new FieldDTO();
        fieldDTO.setType("varchar(255)");
        fieldDTO.setComment("部门名称");
        fieldDTO.setName("department_name");
        return fieldDTO;
    }
}
