package io.cordys.crm.integration.sqlbot.handler;

import io.cordys.crm.system.constants.FieldType;
import io.cordys.crm.system.dto.field.base.BaseField;
import io.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import io.cordys.crm.integration.sqlbot.dto.FieldDTO;
import io.cordys.crm.integration.sqlbot.dto.TableDTO;
import io.cordys.crm.integration.sqlbot.dto.TableHandleParam;
import io.cordys.crm.integration.sqlbot.handler.field.api.ModuleFieldParser;
import io.cordys.crm.integration.sqlbot.handler.field.api.ModuleFieldParserFactory;
import org.apache.commons.lang3.StringUtils;

import java.text.MessageFormat;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 处理组织下的表
 */
public class ModuleFieldTablePermissionHandler extends OrgTablePermissionHandler {
    public static final String MODULE_FIELD_TABLE_SQL_TEMPLATE = """
            select
            {0}
            {1}
            from {2} c
            where c.organization_id = ''{3}''
            """;

    public void handleTable(TableDTO table, TableHandleParam tableHandleParam) {
        String sql = getOrgTableSql(tableHandleParam, table.getFields());
        table.setSql(sql);
    }

    public void handleTable(TableDTO table, TableHandleParam tableHandleParam, ModuleFormConfigDTO formConfig) {
        List<BaseField> moduleFields = filterFields(formConfig.getFields());
        List<FieldDTO> tableFields = table.getFields();
        String sql = getTableSql(tableFields, tableHandleParam, moduleFields);
        table.setSql(sql);

        List<FieldDTO> appendFields = parse2SQLBotFields(moduleFields);
        tableFields.addAll(appendFields);
    }

    protected String getTableSql(List<FieldDTO> sqlBotFields, TableHandleParam tableHandleParam, List<BaseField> moduleFields) {
        String fieldsSql = parseFieldsSql(tableHandleParam.getTableInfo().getTableName() + "_field", moduleFields);
        return MessageFormat.format(MODULE_FIELD_TABLE_SQL_TEMPLATE,
                getSelectSystemFileSql(sqlBotFields),
                fieldsSql,
                tableHandleParam.getTableInfo().getTableName(),
                tableHandleParam.getOrgId()
        );
    }

    /**
     * 解析模块字段SQL
     *
     * @param fields 字段列表
     * @return 拼接的SQL字符串
     */
    protected String parseFieldsSql(String filedValueTableName, List<BaseField> fields) {
        StringBuilder fieldSqlBuilder = new StringBuilder();
        for (int i = 0; i < fields.size(); i++) {
            BaseField field = fields.get(i);
            fieldSqlBuilder.append(",");

            ModuleFieldParser fieldParser = ModuleFieldParserFactory.getFieldParser(field.getType());
            // 拼接字段的SQL
            fieldSqlBuilder.append(fieldParser.parseSql(filedValueTableName, field));
        }
        return fieldSqlBuilder.toString();
    }

    /**
     * 将字段列表转换为SQLBot的FieldDTO对象列表
     *
     * @param fields 字段列表
     * @return 转换后的FieldDTO对象列表
     */
    protected List<FieldDTO> parse2SQLBotFields(List<BaseField> fields) {
        return fields.stream()
                .map(field -> {
                    ModuleFieldParser fieldParser = ModuleFieldParserFactory.getFieldParser(field.getType());
                    return fieldParser.parse2SQLBotField(field);
                }).collect(Collectors.toList());
    }

    protected List<BaseField> filterFields(List<BaseField> fields) {
        return fields
                .stream()
                .filter(field ->
                        // 分割线、图片、位置等字段不支持
                        // 不可见的字段不处理
                        // 业务字段不处理
                        !StringUtils.equalsAny(field.getType(), FieldType.DIVIDER.name(), FieldType.PICTURE.name(), FieldType.LOCATION.name(), FieldType.TEXTAREA.name())
                                && field.getReadable()
                                && StringUtils.isBlank(field.getBusinessKey())
                ).toList();
    }
}
