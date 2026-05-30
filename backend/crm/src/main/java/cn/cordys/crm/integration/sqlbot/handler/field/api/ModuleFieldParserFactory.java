package cn.cordys.crm.integration.sqlbot.handler.field.api;

import cn.cordys.crm.integration.sqlbot.handler.field.*;
import cn.cordys.crm.system.constants.FieldType;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author: jianxing
 * @CreateTime: 2025-08-05  09:59
 */
public class ModuleFieldParserFactory {
    private static final Map<String, ModuleFieldParser<?>> MODULE_FIELD_PARSER_MAP = new HashMap<>();
    private static final DefaultParser DEFAULT_PARSER = new DefaultParser();

    static {
        registerFieldParser(FieldType.SELECT, new SelectParser());
        registerFieldParser(FieldType.SELECT_MULTIPLE, new SelectMultipleParser());
        registerFieldParser(FieldType.RADIO, new RadioParser());
        registerFieldParser(FieldType.CHECKBOX, new CheckboxParser());

        registerFieldParser(FieldType.INPUT, new InputParser());
        registerFieldParser(FieldType.INPUT_MULTIPLE, new InputMultipleParser());
        registerFieldParser(FieldType.TEXTAREA, new TextareaParser());
        registerFieldParser(FieldType.PHONE, new PhoneParser());
        registerFieldParser(FieldType.SERIAL_NUMBER, new SerialNumberParser());

        registerFieldParser(FieldType.DATE_TIME, new DateTimeParser());
        registerFieldParser(FieldType.MEMBER, new MemberParser());
        registerFieldParser(FieldType.MEMBER_MULTIPLE, new MemberMultipleParser());
        registerFieldParser(FieldType.DEPARTMENT, new DepartmentParser());
        registerFieldParser(FieldType.DEPARTMENT_MULTIPLE, new DepartmentMultipleParser());
        registerFieldParser(FieldType.DATA_SOURCE, new DatasourceParser());
        registerFieldParser(FieldType.DATA_SOURCE_MULTIPLE, new DatasourceMultipleParser());
    }

    private static void registerFieldParser(FieldType fieldType, ModuleFieldParser<?> parser) {
        MODULE_FIELD_PARSER_MAP.put(fieldType.name(), parser);
    }

    public static ModuleFieldParser<?> getFieldParser(String fieldType) {
        return MODULE_FIELD_PARSER_MAP.get(fieldType) != null ? MODULE_FIELD_PARSER_MAP.get(fieldType) : DEFAULT_PARSER;
    }
}
