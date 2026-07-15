-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

ALTER TABLE clue_pool_hidden_field MODIFY COLUMN field_id varchar(255) NOT NULL COMMENT '字段ID';


-- 审批编辑快照表：记录编辑触发审批流前的资源数据，用于驳回/撤回时回退
CREATE TABLE approval_resource_snapshot
(
    `id`              VARCHAR(32)  NOT NULL COMMENT 'id',
    `form_key`        VARCHAR(64)  NOT NULL COMMENT '表单类型',
    `resource_id`     VARCHAR(32)  NOT NULL COMMENT '资源ID',
    `snapshot_data`   LONGTEXT     NOT NULL COMMENT '编辑前资源数据快照(JSON)',
    `create_time`     BIGINT       NOT NULL COMMENT '创建时间',
    `create_user`     VARCHAR(32)  DEFAULT NULL COMMENT '创建人',
    `update_time`     BIGINT       DEFAULT NULL COMMENT '更新时间',
    `update_user`     VARCHAR(32)  DEFAULT NULL COMMENT '更新人',
    PRIMARY KEY (id),
    INDEX idx_resource_id (resource_id)
) COMMENT = '审批编辑快照表'
  ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;

CREATE TABLE agent_task(
    `id` VARCHAR(32) NOT NULL   COMMENT 'ID' ,
    `name` VARCHAR(255) NOT NULL   COMMENT '任务名称' ,
    `execution_condition` TEXT(255)    COMMENT '执行条件' ,
    `execution_action` TEXT(255)    COMMENT '执行动作' ,
    `confirmation_level` VARCHAR(20) NOT NULL   COMMENT '确认级别' ,
    `applicable_roles` VARCHAR(1000)    COMMENT '适用角色' ,
    `applicable_model` VARCHAR(32)    COMMENT '适用模型' ,
    `enable` TINYINT(1) NOT NULL  DEFAULT 1 COMMENT '启用状态' ,
    `organization_id` VARCHAR(32) NOT NULL   COMMENT '组织ID' ,
    `create_time` BIGINT NOT NULL   COMMENT '创建时间' ,
    `update_time` BIGINT NOT NULL   COMMENT '更新时间' ,
    `create_user` VARCHAR(32) NOT NULL   COMMENT '创建人' ,
    `update_user` VARCHAR(32) NOT NULL   COMMENT '更新人' ,
    PRIMARY KEY (id)
)  COMMENT = '任务'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;

CREATE TABLE agent_action_suggestion(
    `id` VARCHAR(32) NOT NULL   COMMENT 'ID' ,
    `priority` TINYINT    COMMENT '优先级' ,
    `topic` VARCHAR(255)    COMMENT '行动主题' ,
    `summary` VARCHAR(500)    COMMENT '行动概括' ,
    `content` BLOB    COMMENT '行动上下文' ,
    `user_id` VARCHAR(32) NOT NULL   COMMENT '建议用户' ,
    `actions` VARCHAR(255)    COMMENT '行动操作项' ,
    `create_time` BIGINT NOT NULL   COMMENT '创建时间' ,
    `create_user` VARCHAR(32) NOT NULL   COMMENT '创建人' ,
    PRIMARY KEY (id)
)  COMMENT = '行动建议'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_user_id ON agent_action_suggestion(user_id ASC);

CREATE TABLE agent_action_approve(
    `id` VARCHAR(32) NOT NULL   COMMENT 'ID' ,
    `type` VARCHAR(255)    COMMENT '审核类型' ,
    `topic` VARCHAR(255)    COMMENT '审核主题' ,
    `summary` VARCHAR(500)    COMMENT '审核概括' ,
    `content` BLOB    COMMENT '审核上下文' ,
    `user_id` VARCHAR(32) NOT NULL   COMMENT '审核用户' ,
    `create_time` BIGINT NOT NULL   COMMENT '创建时间' ,
    `create_user` VARCHAR(32) NOT NULL   COMMENT '创建人' ,
    PRIMARY KEY (id)
)  COMMENT = '行动审核'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_user_id ON agent_action_approve(user_id ASC);

-- set innodb lock wait timeout to default
SET SESSION innodb_lock_wait_timeout = DEFAULT;
