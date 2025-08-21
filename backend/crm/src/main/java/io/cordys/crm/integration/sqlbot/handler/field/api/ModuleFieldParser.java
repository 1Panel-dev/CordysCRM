package io.cordys.crm.integration.sqlbot.handler.field.api;


import io.cordys.crm.integration.sqlbot.dto.FieldDTO;
import io.cordys.crm.system.dto.field.base.BaseField;

/**
 * 将模块字段转换为待拼接的SQL语句
 */
public interface ModuleFieldParser<T extends BaseField> {
    /**
     * 编写sql模板
     * @param field 字段信息
     */
    String parseSql(String filedValueTableName, T field);
    /**
     * 将字段转换为SQLBot的FieldDTO对象
     * @param field 字段信息
     * @return 转换后的FieldDTO对象
     */
    FieldDTO parse2SQLBotField(T field);
}
