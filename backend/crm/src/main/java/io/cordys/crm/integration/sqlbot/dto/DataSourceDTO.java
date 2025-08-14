package io.cordys.crm.integration.sqlbot.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 数据源数据传输对象
 * <p>
 * 用于在系统中传递数据库连接配置及结构信息，包括数据库类型、连接参数、认证信息以及数据库表结构。
 * 主要用于数据源服务中表示一个完整的数据库连接及其包含的表结构。
 * </p>
 */
@Data
public class DataSourceDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
    /**
     * 数据源名称
     * <p>标识数据源的唯一名称</p>
     */
    private String name;

    /**
     * 数据源类型
     * <p>数据库类型，如MySQL、Oracle等</p>
     */
    private String type;

    /**
     * 主机地址
     * <p>数据库服务器的主机名或IP地址</p>
     */
    private String host;

    /**
     * 端口号
     * <p>数据库服务端口，如MySQL默认3306</p>
     */
    private int port;

    /**
     * 数据库名称
     * <p>要连接的具体数据库实例名称</p>
     */
    private String dataBase;

    /**
     * 额外参数
     * <p>JDBC URL中的额外连接参数</p>
     */
    private String extraParams;

    /**
     * 用户名
     * <p>数据库连接用户名</p>
     */
    private String user;

    /**
     * 密码
     * <p>数据库连接密码</p>
     */
    private String password;

    /**
     * 架构名称
     * <p>数据库架构名称，用于特定数据库系统</p>
     */
    private String schema;

    /**
     * 注释说明
     * <p>数据源的描述信息</p>
     */
    private String comment;

    /**
     * 数据库表列表
     * <p>数据源中包含的所有表结构信息</p>
     */
    private List<TableDTO> tables;
}