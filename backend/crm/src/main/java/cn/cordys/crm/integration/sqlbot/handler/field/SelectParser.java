package cn.cordys.crm.integration.sqlbot.handler.field;


import cn.cordys.crm.integration.sqlbot.dto.FieldDTO;
import cn.cordys.crm.integration.sqlbot.handler.field.api.OptionFieldParser;
import cn.cordys.crm.system.dto.field.SelectField;

public class SelectParser extends OptionFieldParser<SelectField> {

    @Override
    public String parseSql(String filedValueTableName, SelectField field) {
        return parseOptionFieldSql(filedValueTableName, field, field.getOptions());
    }

    @Override
    public FieldDTO parse2SQLBotField(SelectField field) {
        return parse2SQLBotField(field, field.getOptions());
    }
}
