package cn.cordys.crm.integration.sqlbot.handler.field;


import cn.cordys.crm.integration.sqlbot.handler.field.api.TextFieldParser;
import cn.cordys.crm.system.dto.field.DepartmentMultipleField;

import java.text.MessageFormat;

public class DepartmentMultipleParser extends TextFieldParser<DepartmentMultipleField> {

    public static final String MULTIPLE_DEPARTMENT_OPTION_FIELD_SQL_TEMPLATE = """
            (
               SELECT JSON_ARRAYAGG(sd.name)
               FROM {0} f
               join sys_department sd on f.field_value like concat(''%'', sd.id, ''%'')
               WHERE f.resource_id = c.id AND f.field_id = ''{1}''
               LIMIT 1
            ) AS ''{2}''
            """;

    @Override
    public String parseSql(String fieldValueTable, DepartmentMultipleField field) {
        return MessageFormat.format(MULTIPLE_DEPARTMENT_OPTION_FIELD_SQL_TEMPLATE,
                fieldValueTable + "_blob",
                field.getId(),
                field.getId()
        );
    }
}
