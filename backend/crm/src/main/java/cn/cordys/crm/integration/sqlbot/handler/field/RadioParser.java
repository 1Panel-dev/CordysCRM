package cn.cordys.crm.integration.sqlbot.handler.field;


import cn.cordys.crm.integration.sqlbot.dto.FieldDTO;
import cn.cordys.crm.integration.sqlbot.handler.field.api.OptionFieldParser;
import cn.cordys.crm.system.dto.field.RadioField;

public class RadioParser extends OptionFieldParser<RadioField> {

    @Override
    public String parseSql(String filedValueTableName, RadioField field) {
        return parseOptionFieldSql(filedValueTableName, field, field.getOptions());
    }

    @Override
    public FieldDTO parseToSQLBotField(RadioField field) {
        return parse2SQLBotField(field, field.getOptions());
    }
}
