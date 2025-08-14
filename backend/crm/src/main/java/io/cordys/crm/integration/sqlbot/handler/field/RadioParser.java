package io.cordys.crm.integration.sqlbot.handler.field;


import io.cordys.crm.system.dto.field.RadioField;
import io.cordys.crm.integration.sqlbot.dto.FieldDTO;
import io.cordys.crm.integration.sqlbot.handler.field.api.OptionFieldParser;

public class RadioParser extends OptionFieldParser<RadioField> {

    @Override
    public String parseSql(String filedValueTableName, RadioField field) {
        return parseOptionFieldSql(filedValueTableName, field, field.getOptions());
    }

    @Override
    public FieldDTO parse2SQLBotField(RadioField field) {
        return parse2SQLBotField(field, field.getOptions());
    }
}
