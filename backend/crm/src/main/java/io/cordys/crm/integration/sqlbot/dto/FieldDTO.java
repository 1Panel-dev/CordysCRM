package io.cordys.crm.integration.sqlbot.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 数据库字段数据传输对象
 * <p>
 * 用于在系统中传递数据库表字段的结构信息，包括字段名称、注释说明和数据类型。
 * 主要用于表示数据库表结构中的单个字段属性。
 * </p>
 */
@Data
public class FieldDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 字段名称
     * <p>数据库中的实际字段名</p>
     */
    private String name;

    /**
     * 字段注释
     * <p>数据库字段的描述信息</p>
     */
    private String comment;

    /**
     * 字段数据类型
     * <p>数据库中定义的字段类型，如VARCHAR、INT等</p>
     */
    private String type;
}