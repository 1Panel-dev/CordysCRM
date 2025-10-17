package cn.cordys.crm.integration.sqlbot.handler.field.api;


import cn.cordys.crm.integration.sqlbot.dto.FieldDTO;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.field.base.OptionProp;
import org.apache.commons.collections4.CollectionUtils;

import java.text.MessageFormat;
import java.util.List;

public abstract class MultipleOptionFieldParser<T extends BaseField> extends OptionFieldParser<T> {

    public static final String MULTIPLE_OPTION_FIELD_SQL_TEMPLATE = """
            (
               SELECT JSON_ARRAYAGG(option_t.label)
               FROM {0} f
               left join (
                   {1}
               ) option_t on f.field_value like concat(''%'', option_t.value, ''%'')
                WHERE option_t.label is not null and f.resource_id = c.id AND f.field_id = ''{2}''
                LIMIT 1
            ) AS ''{3}''
            """;

    @Override
    protected String parseOptionFieldSql(String fieldValueTable, BaseField field, List<OptionProp> options) {
        if (CollectionUtils.isEmpty(options)) {
            return parseEmptyOptionSql(field);
        }
        return MessageFormat.format(MULTIPLE_OPTION_FIELD_SQL_TEMPLATE,
                fieldValueTable + "_blob",
                parseOptionSql(options),
                field.getId(),
                field.getId()
        );
    }

    @Override
    public FieldDTO parse2SQLBotField(BaseField field, List<OptionProp> options) {
        FieldDTO fieldDTO = new FieldDTO();
        fieldDTO.setName(field.getId());
        fieldDTO.setType("text");
        fieldDTO.setComment(field.getName() + getOptionLabelStr(options));
        return fieldDTO;
    }
}
