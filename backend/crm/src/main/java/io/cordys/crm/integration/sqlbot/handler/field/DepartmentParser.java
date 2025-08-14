package io.cordys.crm.integration.sqlbot.handler.field;


import io.cordys.crm.system.dto.field.DepartmentField;
import io.cordys.crm.integration.sqlbot.handler.field.api.TextFieldParser;

import java.text.MessageFormat;

public class DepartmentParser extends TextFieldParser<DepartmentField> {

    public static final String DEPARTMENT_OPTION_FIELD_SQL_TEMPLATE = """
            (
               SELECT sd.name
               FROM {0} f
               join sys_department sd on sd.id = f.field_value
               WHERE f.resource_id = c.id AND f.field_id = ''{1}''
               LIMIT 1
            ) AS ''{2}''
            """;

    @Override
    public String parseSql(String fieldValueTable, DepartmentField field) {
        return MessageFormat.format(DEPARTMENT_OPTION_FIELD_SQL_TEMPLATE,
                fieldValueTable,
                field.getId(),
                field.getId()
        );
    }
}
