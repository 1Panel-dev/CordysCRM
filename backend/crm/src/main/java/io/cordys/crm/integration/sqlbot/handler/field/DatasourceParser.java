package io.cordys.crm.integration.sqlbot.handler.field;


import io.cordys.crm.integration.sqlbot.handler.field.api.TextFieldParser;
import io.cordys.crm.system.constants.FieldSourceType;
import io.cordys.crm.system.dto.field.DatasourceField;
import org.apache.commons.lang3.Strings;

import java.text.MessageFormat;

public class DatasourceParser extends TextFieldParser<DatasourceField> {

    public static final String MEMBER_OPTION_FIELD_SQL_TEMPLATE = """
            (
               SELECT ds.name
               FROM {0} f
               join {1} ds on ds.id = f.field_value
               WHERE f.resource_id = c.id AND f.field_id = ''{2}''
               LIMIT 1
            ) AS ''{3}''
            """;

    @Override
    public String parseSql(String fieldValueTable, DatasourceField field) {
        String datasourceTableName = field.getDataSourceType().toLowerCase();
        if (Strings.CI.equals(datasourceTableName, FieldSourceType.CONTACT.name())) {
            datasourceTableName = "customer_contact";
        }
        return MessageFormat.format(MEMBER_OPTION_FIELD_SQL_TEMPLATE,
                fieldValueTable,
                datasourceTableName,
                field.getId(),
                field.getId()
        );
    }
}
