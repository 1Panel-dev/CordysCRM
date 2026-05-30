package cn.cordys.crm.integration.sqlbot.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 数据库表结构数据传输对象
 * <p>
 * 用于在系统中传递数据库表的结构信息，包括表名、注释、规则定义、SQL定义以及字段列表。
 * 主要用于数据源服务中获取数据库模式信息时使用。
 * </p>
 */
@Data
public class TableDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * select * from (select * from test ) tx where
     * 表名称
     * <p>数据库中的实际表名</p>
     */
    private String name;

    /**
     * 表注释
     * <p>数据库表的描述信息</p>
     */
    private String comment;

    /**
     * 表规则
     * name = name like '%${name}%' and age = ${age} and time = '${time}'
     * <p>执行在 WHERE 条件中</p>
     */
    private String rule;

    /**
     * 自定义表SQL定义
     * select * from `test` where name='${name}'  and name like '%${name}%' and age = ${age} and time = '${time}
     * <p>类似虚拟一个数据集，编写自定义的SQL</p>
     */
    private String sql;

    /**
     * 表字段列表
     * <p>包含表中所有字段的详细信息</p>
     */
    private List<FieldDTO> fields;
}