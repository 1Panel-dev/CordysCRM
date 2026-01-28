package cn.cordys.crm.integration.sqlbot.handler.field;

import cn.cordys.crm.integration.sqlbot.handler.field.api.TextFieldParser;
import cn.cordys.crm.system.dto.field.MemberMultipleField;
import java.text.MessageFormat;

public class MemberMultipleParser extends TextFieldParser<MemberMultipleField> {

  public static final String MULTIPLE_MEMBER_OPTION_FIELD_SQL_TEMPLATE =
      """
      (
         SELECT JSON_ARRAYAGG(su.name)
         FROM {0} f
         join sys_user su on f.field_value like concat(''%'', su.id, ''%'')
         WHERE f.resource_id = c.id AND f.field_id = ''{1}''
         LIMIT 1
      ) AS ''{2}''
      """;

  @Override
  public String parseSql(String fieldValueTable, MemberMultipleField field) {
    return MessageFormat.format(
        MULTIPLE_MEMBER_OPTION_FIELD_SQL_TEMPLATE,
        fieldValueTable + "_blob",
        field.getId(),
        field.getId());
  }
}
