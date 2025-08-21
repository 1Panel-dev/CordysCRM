package io.cordys.crm.integration.sqlbot.handler.field.api;


import io.cordys.crm.integration.sqlbot.dto.FieldDTO;
import io.cordys.crm.system.constants.FieldType;
import io.cordys.crm.system.dto.field.base.BaseField;
import org.apache.commons.lang3.Strings;

import java.text.MessageFormat;

public abstract class TextFieldParser<T extends BaseField> implements ModuleFieldParser<T> {

    public static final String TEXT_FIELD_SQL_TEMPLATE = """
            (
               SELECT f.field_value
               FROM {0} f
               WHERE f.resource_id = c.id AND f.field_id = ''{1}''
               LIMIT 1
            ) AS ''{2}''
            """;

    @Override
    public String parseSql(String fieldValueTable, T field) {
        return MessageFormat.format(TEXT_FIELD_SQL_TEMPLATE,
                fieldValueTable,
                field.getId(),
                field.getId()
        );
    }

    @Override
    public FieldDTO parse2SQLBotField(BaseField field) {
        FieldDTO fieldDTO = new FieldDTO();
        fieldDTO.setName(field.getId());
        if (field.multiple() || Strings.CS.equals(field.getType(), FieldType.TEXTAREA.name())) {
            fieldDTO.setType("text");
        } else {
            fieldDTO.setType("varchar(255)");
        }
        fieldDTO.setComment(field.getName());
        return fieldDTO;
    }
}
