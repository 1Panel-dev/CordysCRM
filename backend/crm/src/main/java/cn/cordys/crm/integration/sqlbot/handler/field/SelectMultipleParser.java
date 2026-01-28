package cn.cordys.crm.integration.sqlbot.handler.field;

import cn.cordys.crm.integration.sqlbot.dto.FieldDTO;
import cn.cordys.crm.integration.sqlbot.handler.field.api.MultipleOptionFieldParser;
import cn.cordys.crm.system.dto.field.SelectMultipleField;

public class SelectMultipleParser extends MultipleOptionFieldParser<SelectMultipleField> {

  @Override
  public String parseSql(String filedValueTableName, SelectMultipleField field) {
    return parseOptionFieldSql(filedValueTableName, field, field.getOptions());
  }

  @Override
  public FieldDTO parseToSQLBotField(SelectMultipleField field) {
    return parse2SQLBotField(field, field.getOptions());
  }
}
