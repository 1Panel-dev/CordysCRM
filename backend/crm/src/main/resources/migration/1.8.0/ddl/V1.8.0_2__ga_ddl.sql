-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

CREATE TABLE agent_model
(
    `id`                 VARCHAR(32)   NOT NULL COMMENT 'ID',
    `display_name`       VARCHAR(255)  NOT NULL COMMENT '模型展示名称',
    `model_name`         VARCHAR(255)  NOT NULL COMMENT '模型名称',
    `provider`           VARCHAR(50)   NOT NULL DEFAULT 'OpenAI' COMMENT '模型供应商',
    `api_url`            VARCHAR(100) COMMENT 'API请求地址',
    `api_key`            VARCHAR(1000) NOT NULL COMMENT 'API Key',
    `enable`             TINYINT(1)    NOT NULL DEFAULT 1 COMMENT '启用状态',
    `user_daily_limit`   BIGINT                 DEFAULT -1 COMMENT '用户每日调用限制',
    `global_daily_limit` BIGINT                 DEFAULT -1 COMMENT '全局每日调用限制',
    `model_params`       TEXT(255) COMMENT '模型参数',
    `organization_id`    VARCHAR(32)   NOT NULL COMMENT '组织ID',
    `create_time`        BIGINT        NOT NULL COMMENT '创建时间',
    `update_time`        BIGINT        NOT NULL COMMENT '更新时间',
    `create_user`        VARCHAR(32)   NOT NULL COMMENT '创建人',
    `update_user`        VARCHAR(32)   NOT NULL COMMENT '更新人',
    PRIMARY KEY (id)
) COMMENT = '模型'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_provider ON agent_model (provider ASC);
CREATE INDEX idx_org_id ON agent_model (organization_id ASC);
CREATE INDEX idx_enable ON agent_model (enable ASC);

CREATE TABLE agent_task
(
    `id`                  VARCHAR(32)  NOT NULL COMMENT 'ID',
    `name`                VARCHAR(255) NOT NULL COMMENT '任务名称',
    `trigger_type`        VARCHAR(32) NOT NULL   COMMENT '触发类型',
    `execution_condition` TEXT(255) COMMENT '执行条件',
    `execution_action`    TEXT(255) COMMENT '执行动作',
    `confirmation_level`  VARCHAR(20)  NOT NULL COMMENT '确认级别',
    `applicable_roles`    VARCHAR(1000) COMMENT '适用角色',
    `applicable_model`    VARCHAR(32) COMMENT '适用模型',
    `enable`              TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '启用状态',
    `organization_id`     VARCHAR(32)  NOT NULL COMMENT '组织ID',
    `create_time`         BIGINT       NOT NULL COMMENT '创建时间',
    `update_time`         BIGINT       NOT NULL COMMENT '更新时间',
    `create_user`         VARCHAR(32)  NOT NULL COMMENT '创建人',
    `update_user`         VARCHAR(32)  NOT NULL COMMENT '更新人',
    PRIMARY KEY (id)
) COMMENT = '任务'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

CREATE TABLE agent_action_suggestion
(
    `id`          VARCHAR(32) NOT NULL COMMENT 'ID',
    `priority`    TINYINT COMMENT '优先级',
    `topic`       VARCHAR(255) COMMENT '行动主题',
    `summary`     VARCHAR(500) COMMENT '行动概括',
    `content`     BLOB COMMENT '行动上下文',
    `user_id`     VARCHAR(32) NOT NULL COMMENT '建议用户',
    `organization_id` VARCHAR(32) NOT NULL   COMMENT '组织ID' ,
    `actions`     VARCHAR(255) COMMENT '行动操作项',
    `create_time` BIGINT      NOT NULL COMMENT '创建时间',
    `create_user` VARCHAR(32) NOT NULL COMMENT '创建人',
    PRIMARY KEY (id)
) COMMENT = '行动建议'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_user_id ON agent_action_suggestion (user_id ASC);

CREATE TABLE agent_action_approve
(
    `id`          VARCHAR(32) NOT NULL COMMENT 'ID',
    `type`        VARCHAR(255) COMMENT '审核类型',
    `topic`       VARCHAR(255) COMMENT '审核主题',
    `summary`     VARCHAR(500) COMMENT '审核概括',
    `content`     BLOB COMMENT '审核上下文',
    `user_id`     VARCHAR(32) NOT NULL COMMENT '审核用户',
    `organization_id` VARCHAR(32) NOT NULL   COMMENT '组织ID' ,
    `create_time` BIGINT      NOT NULL COMMENT '创建时间',
    `create_user` VARCHAR(32) NOT NULL COMMENT '创建人',
    PRIMARY KEY (id)
) COMMENT = '行动审核'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_user_id ON agent_action_approve (user_id ASC);

CREATE TABLE agent_conversation(
    `id` VARCHAR(32) NOT NULL   COMMENT 'id' ,
    `title` VARCHAR(255) NOT NULL   COMMENT '对话标题' ,
    `user_id` VARCHAR(32) NOT NULL   COMMENT '用户ID' ,
    `organization_id` VARCHAR(32) NOT NULL   COMMENT '组织ID' ,
    `create_time` BIGINT NOT NULL   COMMENT '创建时间' ,
    `update_time` BIGINT NOT NULL   COMMENT '更新时间' ,
    `create_user` VARCHAR(32) NOT NULL   COMMENT '创建人' ,
    `update_user` VARCHAR(32) NOT NULL   COMMENT '更新人' ,
    PRIMARY KEY (id)
)  COMMENT = '会话'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_user_id ON agent_conversation(user_id ASC);

CREATE TABLE agent_message(
    `id` VARCHAR(32) NOT NULL   COMMENT 'ID' ,
    `role` VARCHAR(50) NOT NULL  DEFAULT 'user/assistant' COMMENT '对话角色' ,
    `conversation_id` VARCHAR(32) NOT NULL   COMMENT '对话ID' ,
    `model_name` VARCHAR(255)    COMMENT '模型名称' ,
    `prompt_tokens` BIGINT    COMMENT '本次调用输入' ,
    `completion_tokens` BIGINT(255)    COMMENT '本次调用输出' ,
    `total_tokens` BIGINT(255)    COMMENT '累计调用' ,
    `content` MEDIUMTEXT    COMMENT '消息内容' ,
    `organization_id` VARCHAR(32) NOT NULL   COMMENT '组织ID' ,
    `create_time` BIGINT NOT NULL   COMMENT '创建时间' ,
    `update_time` BIGINT NOT NULL   COMMENT '更新时间' ,
    `create_user` VARCHAR(32) NOT NULL   COMMENT '创建人' ,
    `update_user` VARCHAR(32) NOT NULL   COMMENT '更新人' ,
    PRIMARY KEY (id)
)  COMMENT = '消息'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_conversation_id ON agent_message(conversation_id ASC);

CREATE TABLE agent_term_catalog(
    `id` VARCHAR(32) NOT NULL   COMMENT 'id' ,
    `name` VARCHAR(255) NOT NULL   COMMENT '分类名称' ,
    `organization_id` VARCHAR(32) NOT NULL   COMMENT '组织ID' ,
    `create_time` BIGINT NOT NULL   COMMENT '创建时间' ,
    `update_time` BIGINT NOT NULL   COMMENT '更新时间' ,
    `create_user` VARCHAR(32) NOT NULL   COMMENT '创建人' ,
    `update_user` VARCHAR(32) NOT NULL   COMMENT '更新人' ,
    PRIMARY KEY (id)
)  COMMENT = '术语分类'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

CREATE TABLE agent_term(
    `id` VARCHAR(32) NOT NULL   COMMENT 'ID' ,
    `catalog_id` VARCHAR(32) NOT NULL   COMMENT '分类ID' ,
    `standard_term` VARCHAR(255) NOT NULL   COMMENT '标准术语' ,
    `also_called` VARCHAR(255)    COMMENT '同义词' ,
    `avoid_these` VARCHAR(255)    COMMENT '禁用词' ,
    `use_case` VARCHAR(255)    COMMENT '试用场景' ,
    `system_reference` VARCHAR(255)    COMMENT '系统映射' ,
    `enable` TINYINT(1) NOT NULL   COMMENT '状态' ,
    `organization_id` VARCHAR(32) NOT NULL   COMMENT '组织ID' ,
    `create_time` BIGINT NOT NULL   COMMENT '创建时间' ,
    `update_time` BIGINT NOT NULL   COMMENT '更新时间' ,
    `create_user` VARCHAR(32) NOT NULL   COMMENT '创建人' ,
    `update_user` VARCHAR(32) NOT NULL   COMMENT '更新人' ,
    PRIMARY KEY (id)
)  COMMENT = '术语配置'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_catalog_id ON agent_term(catalog_id ASC);

CREATE TABLE agent_term_discovery(
    `id` VARCHAR(32) NOT NULL   COMMENT 'ID' ,
    `free_term` VARCHAR(255) NOT NULL   COMMENT '未定义术语' ,
    `source` VARCHAR(50)    COMMENT '发现来源' ,
    `reference` VARCHAR(255)    COMMENT '映射' ,
    `create_time` BIGINT NOT NULL   COMMENT '创建时间' ,
    PRIMARY KEY (id)
)  COMMENT = '术语发现'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

CREATE TABLE agent_task_execute_log(
    `id` VARCHAR(32) NOT NULL   COMMENT 'id' ,
    `task_id` VARCHAR(32) NOT NULL   COMMENT '任务ID' ,
    `execute_time` BIGINT NOT NULL   COMMENT '执行时间' ,
    `execute_reason` VARCHAR(500) NOT NULL   COMMENT '触发原因' ,
    `result` VARCHAR(255) NOT NULL   COMMENT '结果' ,
    `confirm_user` VARCHAR(32)    COMMENT '确认用户' ,
    PRIMARY KEY (id)
)  COMMENT = '执行记录'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_task_id ON agent_task_execute_log(task_id ASC);
CREATE INDEX idx_confirm_user ON agent_task_execute_log(confirm_user ASC);

-- set innodb lock wait timeout to default
SET SESSION innodb_lock_wait_timeout = DEFAULT;