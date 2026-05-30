package cn.cordys.crm.integration.sqlbot.handler.field;


import cn.cordys.crm.integration.sqlbot.handler.field.api.TextFieldParser;
import cn.cordys.crm.system.dto.field.DateTimeField;

import java.text.MessageFormat;

public class DateTimeParser extends TextFieldParser<DateTimeField> {
    public static final String DATE_TIME_FIELD_SQL_TEMPLATE = """
            (
               SELECT DATE_FORMAT(FROM_UNIXTIME(f.field_value / 1000),''%Y-%m-%d %H:%i:%s'')
               FROM {0} f
               WHERE f.resource_id = c.id AND f.field_id = ''{1}''
               LIMIT 1
            ) AS ''{2}''
            """;

    @Override
    public String parseSql(String fieldValueTable, DateTimeField field) {
        return MessageFormat.format(DATE_TIME_FIELD_SQL_TEMPLATE,
                fieldValueTable,
                field.getId(),
                field.getId()
        );
    }
}
