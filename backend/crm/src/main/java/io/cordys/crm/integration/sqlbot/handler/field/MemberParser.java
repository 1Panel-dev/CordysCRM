package io.cordys.crm.integration.sqlbot.handler.field;


import io.cordys.crm.integration.sqlbot.handler.field.api.TextFieldParser;
import io.cordys.crm.system.dto.field.MemberField;

import java.text.MessageFormat;

public class MemberParser extends TextFieldParser<MemberField> {

    public static final String MEMBER_OPTION_FIELD_SQL_TEMPLATE = """
            (
               SELECT su.name
               FROM {0} f
               join sys_user su on su.id = f.field_value
               WHERE f.resource_id = c.id AND f.field_id = ''{1}''
               LIMIT 1
            ) AS ''{2}''
            """;

    @Override
    public String parseSql(String fieldValueTable, MemberField field) {
        return MessageFormat.format(MEMBER_OPTION_FIELD_SQL_TEMPLATE,
                fieldValueTable,
                field.getId(),
                field.getId()
        );
    }
}
