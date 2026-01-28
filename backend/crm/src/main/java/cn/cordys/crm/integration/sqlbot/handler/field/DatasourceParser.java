package cn.cordys.crm.integration.sqlbot.handler.field;

import cn.cordys.crm.integration.sqlbot.handler.field.api.TextFieldParser;
import cn.cordys.crm.system.constants.FieldSourceType;
import cn.cordys.crm.system.dto.field.DatasourceField;
import java.text.MessageFormat;
import org.apache.commons.lang3.Strings;

public class DatasourceParser extends TextFieldParser<DatasourceField> {

  public static final String MEMBER_OPTION_FIELD_SQL_TEMPLATE =
      """
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
    if (Strings.CI.equals(datasourceTableName, FieldSourceType.QUOTATION.name())) {
      datasourceTableName = "opportunity_quotation";
    }
    return MessageFormat.format(
        MEMBER_OPTION_FIELD_SQL_TEMPLATE,
        fieldValueTable,
        datasourceTableName,
        field.getId(),
        field.getId());
  }
}
