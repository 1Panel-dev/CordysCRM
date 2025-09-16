-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

CREATE TABLE agent
(
    `id`              VARCHAR(32)  NOT NULL COMMENT 'id',
    `name`            VARCHAR(255) NOT NULL COMMENT '名称',
    `agent_module_id` VARCHAR(32)  NOT NULL COMMENT '文件id',
    `organization_id` VARCHAR(32)  NOT NULL COMMENT '组织id',
    `pos`             BIGINT       NOT NULL DEFAULT 0 COMMENT '同一节点下顺序',
    `scope_id`        TEXT         NOT NULL COMMENT '应用范围',
    `script`          TEXT         NOT NULL COMMENT '嵌入脚本',
    `description`     VARCHAR(1000) COMMENT '描述',
    `create_time`     BIGINT       NOT NULL COMMENT '创建时间',
    `update_time`     BIGINT       NOT NULL COMMENT '更新时间',
    `create_user`     VARCHAR(32)  NOT NULL COMMENT '创建人',
    `update_user`     VARCHAR(32)  NOT NULL COMMENT '更新人',
    PRIMARY KEY (id)
) COMMENT = '智能体'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_name ON agent (name ASC);
CREATE INDEX idx_agent_module_id ON agent (agent_module_id ASC);


CREATE TABLE agent_module
(
    `id`              VARCHAR(32)  NOT NULL COMMENT 'id',
    `organization_id` VARCHAR(32)  NOT NULL COMMENT '组织id',
    `name`            VARCHAR(255) NOT NULL COMMENT '名称',
    `parent_id`       VARCHAR(32)  NOT NULL DEFAULT 'NONE' COMMENT '父节点id',
    `pos`             BIGINT       NOT NULL DEFAULT 0 COMMENT '同一节点下顺序',
    `create_time`     BIGINT       NOT NULL COMMENT '创建时间',
    `update_time`     BIGINT       NOT NULL COMMENT '更新时间',
    `create_user`     VARCHAR(32)  NOT NULL COMMENT '创建人',
    `update_user`     VARCHAR(32)  NOT NULL COMMENT '更新人',
    PRIMARY KEY (id)
) COMMENT = '智能体模块'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_organization_id ON agent_module (organization_id ASC);
CREATE INDEX idx_name ON agent_module (name ASC);
CREATE INDEX idx_pos ON agent_module (pos ASC);
CREATE INDEX idx_parent_id ON agent_module (parent_id ASC);


CREATE TABLE agent_collection
(
    `id`          VARCHAR(32) NOT NULL COMMENT 'id',
    `user_id`     VARCHAR(32) NOT NULL COMMENT '用户id',
    `agent_id`    VARCHAR(32) NOT NULL COMMENT '智能体id',
    `create_time` BIGINT      NOT NULL COMMENT '创建时间',
    `update_time` BIGINT      NOT NULL COMMENT '更新时间',
    `create_user` VARCHAR(32) NOT NULL COMMENT '创建人',
    `update_user` VARCHAR(32) NOT NULL COMMENT '更新人',
    PRIMARY KEY (id)
) COMMENT = '智能体收藏'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_user_id ON agent_collection (user_id ASC);

CREATE TABLE sys_ui_settings
(
    `param_key`       VARCHAR(100) NOT NULL COMMENT 'key',
    `param_value`     VARCHAR(255) COMMENT 'value',
    `type`            VARCHAR(100) NOT NULL DEFAULT 'text' COMMENT 'type',
    `organization_id` VARCHAR(32)  NOT NULL COMMENT '组织ID',
    PRIMARY KEY (param_key)
) COMMENT = '界面设置'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_organization_id ON sys_ui_settings (organization_id ASC);

-- set innodb lock wait timeout to default
SET SESSION innodb_lock_wait_timeout = DEFAULT;



