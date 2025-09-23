package cn.cordys.crm.integration.sqlbot.handler.field.api;


import cn.cordys.crm.integration.sqlbot.dto.FieldDTO;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.field.base.OptionProp;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.text.MessageFormat;
import java.util.List;
import java.util.stream.Collectors;

public abstract class OptionFieldParser<T extends BaseField> implements ModuleFieldParser<T> {

    public static final String SINGLE_OPTION_FIELD_SQL_TEMPLATE = """
            (
               SELECT option.label
               FROM {0} f
               left join (
                   {1}
               ) option on option.value = f.field_value
                WHERE f.resource_id = c.id AND f.field_id = ''{2}''
                LIMIT 1
            ) AS ''{3}''
            """;

    /**
     * 将选项列表转换为SQL查询语句
     * 例如：
     * SELECT '400电话' AS label, '1' AS value
     * UNION ALL SELECT '官网咨询', '2'
     *
     * @param options
     *
     * @return
     */
    protected static String parseOptionSql(List<OptionProp> options) {
        if (CollectionUtils.isEmpty(options)) {
            return null;
        }

        StringBuffer sqlBuffer = new StringBuffer();

        for (int i = 0; i < options.size(); i++) {
            OptionProp option = options.get(i);
            if (option == null || StringUtils.isAnyBlank(option.getValue(), option.getLabel())) {
                continue;
            }

            String label = option.getLabel().replace("'", "''");
            String value = option.getValue().replace("'", "''");

            if (i == 0) {
                sqlBuffer.append(String.format("SELECT '%s' AS label, '%s' AS value", label, value));
            } else {
                sqlBuffer.append(String.format(" UNION ALL SELECT '%s', '%s'", label, value));
            }
        }

        return sqlBuffer.toString();
    }

    protected String parseOptionFieldSql(String fieldValueTable, BaseField field, List<OptionProp> options) {
        if (CollectionUtils.isEmpty(options)) {
            return parseEmptyOptionSql(field);
        }
        return MessageFormat.format(SINGLE_OPTION_FIELD_SQL_TEMPLATE,
                fieldValueTable,
                parseOptionSql(options),
                field.getId(),
                field.getId()
        );
    }

    public FieldDTO parse2SQLBotField(BaseField field, List<OptionProp> options) {
        FieldDTO fieldDTO = new FieldDTO();
        fieldDTO.setName(field.getId());
        fieldDTO.setType("varchar(50)");
        fieldDTO.setComment(field.getName() + getOptionLabelStr(options));
        return fieldDTO;
    }

    protected String getOptionLabelStr(List<OptionProp> options) {
        return "，可选值：(" +
                options
                        .stream()
                        .map(OptionProp::getLabel)
                        .collect(Collectors.joining(","))
                + ")";
    }

    protected String parseEmptyOptionSql(BaseField field) {
        return "'' as " + field.getId();
    }
}
