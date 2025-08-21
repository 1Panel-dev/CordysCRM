package io.cordys.crm.integration.sqlbot.handler.field;


import io.cordys.crm.integration.sqlbot.dto.FieldDTO;
import io.cordys.crm.integration.sqlbot.handler.field.api.MultipleOptionFieldParser;
import io.cordys.crm.system.dto.field.CheckBoxField;

public class CheckboxParser extends MultipleOptionFieldParser<CheckBoxField> {

    @Override
    public String parseSql(String filedValueTableName, CheckBoxField field) {
        return parseOptionFieldSql(filedValueTableName, field, field.getOptions());
    }

    @Override
    public FieldDTO parse2SQLBotField(CheckBoxField field) {
        return parse2SQLBotField(field, field.getOptions());
    }
}
