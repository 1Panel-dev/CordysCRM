-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

CREATE TABLE agent_model(
    `id` VARCHAR(32) NOT NULL   COMMENT 'ID' ,
    `display_name` VARCHAR(255) NOT NULL   COMMENT '模型展示名称' ,
    `model_name` VARCHAR(255) NOT NULL   COMMENT '模型名称' ,
    `provider` VARCHAR(50) NOT NULL  DEFAULT OpenAI COMMENT '模型供应商' ,
    `api_url` VARCHAR(100)    COMMENT 'API请求地址' ,
    `api_key` VARCHAR(100) NOT NULL   COMMENT 'API Key' ,
    `enable` TINYINT(1) NOT NULL  DEFAULT 1 COMMENT '启用状态' ,
    `user_daily_limit` BIGINT   DEFAULT -1 COMMENT '用户每日调用限制' ,
    `global_daily_limit` BIGINT   DEFAULT -1 COMMENT '全局每日调用限制' ,
    `model_params` TEXT(255)    COMMENT '模型参数' ,
    `organization_id` VARCHAR(32) NOT NULL   COMMENT '组织ID' ,
    `create_time` BIGINT NOT NULL   COMMENT '创建时间' ,
    `update_time` BIGINT NOT NULL   COMMENT '更新时间' ,
    `create_user` VARCHAR(32) NOT NULL   COMMENT '创建人' ,
    `update_user` VARCHAR(32) NOT NULL   COMMENT '更新人' ,
    PRIMARY KEY (id)
)  COMMENT = '模型'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_provider ON agent_model(provider ASC);
CREATE INDEX idx_org_id ON agent_model(organization_id ASC);
CREATE INDEX idx_enable ON agent_model(enable ASC);

-- set innodb lock wait timeout to default
SET SESSION innodb_lock_wait_timeout = DEFAULT;