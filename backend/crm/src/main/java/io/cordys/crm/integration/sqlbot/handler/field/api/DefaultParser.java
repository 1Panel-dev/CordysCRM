package io.cordys.crm.integration.sqlbot.handler.field.api;


import io.cordys.crm.system.dto.field.base.BaseField;
import io.cordys.crm.integration.sqlbot.dto.FieldDTO;

import java.text.MessageFormat;

public class DefaultParser implements ModuleFieldParser<BaseField> {

    public static final String DEFAULT_FIELD_SQL_TEMPLATE = """
            ''选项不存在'' AS ''{0}''
            """;

    @Override
    public String parseSql(String filedValueTableName, BaseField field) {
        return MessageFormat.format(DEFAULT_FIELD_SQL_TEMPLATE,
                field.getId()
        );
    }

    @Override
    public FieldDTO parse2SQLBotField(BaseField field) {
        FieldDTO fieldDTO = new FieldDTO();
        fieldDTO.setName(field.getId());
        fieldDTO.setType("text");
        fieldDTO.setComment(field.getName());
        return fieldDTO;
    }
}
