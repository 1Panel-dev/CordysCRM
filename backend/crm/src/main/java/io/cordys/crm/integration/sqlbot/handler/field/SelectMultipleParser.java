package io.cordys.crm.integration.sqlbot.handler.field;


import io.cordys.crm.integration.sqlbot.dto.FieldDTO;
import io.cordys.crm.integration.sqlbot.handler.field.api.MultipleOptionFieldParser;
import io.cordys.crm.system.dto.field.SelectMultipleField;

public class SelectMultipleParser extends MultipleOptionFieldParser<SelectMultipleField> {

    @Override
    public String parseSql(String filedValueTableName, SelectMultipleField field) {
        return parseOptionFieldSql(filedValueTableName, field, field.getOptions());
    }

    @Override
    public FieldDTO parse2SQLBotField(SelectMultipleField field) {
        return parse2SQLBotField(field, field.getOptions());
    }
}
