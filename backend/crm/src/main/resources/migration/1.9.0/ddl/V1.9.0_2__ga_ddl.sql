-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

-- agent ddl start
CREATE TABLE agent_model
(
    `id`                 VARCHAR(32)   NOT NULL COMMENT 'ID',
    `display_name`       VARCHAR(255)  NOT NULL COMMENT '模型展示名称',
    `model_name`         VARCHAR(255)  NOT NULL COMMENT '模型名称',
    `provider`           VARCHAR(50)   NOT NULL DEFAULT 'OpenAI' COMMENT '模型供应商',
    `api_url`            VARCHAR(100) COMMENT 'API请求地址',
    `api_key`            VARCHAR(1000) NOT NULL COMMENT 'API Key',
    `enable`             TINYINT(1)    NOT NULL DEFAULT 1 COMMENT '启用状态',
    `user_daily_limit`   DECIMAL(20, 0)         DEFAULT -1 COMMENT '用户每日调用限制',
    `global_daily_limit` DECIMAL(20, 0)         DEFAULT -1 COMMENT '全局每日调用限制',
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

CREATE TABLE agent_model_strategy(
    `id` VARCHAR(32) NOT NULL   COMMENT 'id' ,
    `chat_models` VARCHAR(1000)    COMMENT '对话或通用模型' ,
    `task_models` VARCHAR(1000)    COMMENT '任务模型' ,
    `fallback` TINYINT(1) NOT NULL   COMMENT '是否自动降级' ,
    PRIMARY KEY (id)
)  COMMENT = '模型策略'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

CREATE TABLE agent_task
(
    `id`                  VARCHAR(32)  NOT NULL COMMENT 'ID',
    `name`                VARCHAR(255) NOT NULL COMMENT '任务名称',
    `trigger_type`        VARCHAR(32) NOT NULL   COMMENT '触发类型',
    `execution_condition` TEXT(255) COMMENT '执行条件',
    `execution_action`    TEXT(255) COMMENT '执行动作',
    `confirmation_level`  VARCHAR(20)  NOT NULL COMMENT '确认级别',
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
    `content`     LONGTEXT COMMENT '行动上下文',
    `user_id`     VARCHAR(32) NOT NULL COMMENT '建议用户',
    `organization_id` VARCHAR(32) NOT NULL   COMMENT '组织ID' ,
    `actions`     VARCHAR(255) COMMENT '行动操作项',
    `create_time` BIGINT      NOT NULL COMMENT '创建时间',
    `create_user` VARCHAR(32) NOT NULL COMMENT '创建人',
    `status` VARCHAR(10) NOT NULL   COMMENT '状态' ,
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
    `content`     LONGTEXT COMMENT '审核上下文',
    `user_id`     VARCHAR(32) NOT NULL COMMENT '审核用户',
    `organization_id` VARCHAR(32) NOT NULL   COMMENT '组织ID' ,
    `create_time` BIGINT      NOT NULL COMMENT '创建时间',
    `create_user` VARCHAR(32) NOT NULL COMMENT '创建人',
    `status` VARCHAR(10) NOT NULL   COMMENT '状态' ,
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
    `role` VARCHAR(50) NOT NULL  COMMENT '对话角色' ,
    `run_id` VARCHAR(32) NOT NULL   COMMENT '执行ID' ,
    `conversation_id` VARCHAR(32) NOT NULL   COMMENT '对话ID' ,
    `input_tokens` BIGINT    COMMENT '本次对话输入' ,
    `output_tokens` BIGINT(255)    COMMENT '本次对话输出' ,
    `total_tokens` BIGINT(255)    COMMENT '累计调用' ,
    `content` MEDIUMTEXT    COMMENT '消息内容' ,
    `organization_id` VARCHAR(32) NOT NULL   COMMENT '组织ID' ,
    `helpful` TINYINT(1)    COMMENT '点赞/点踩：1 点赞，0 点踩，NULL 未评价' ,
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
CREATE INDEX idx_run_id ON agent_message(run_id ASC);

CREATE TABLE agent_term_catalog(
    `id` VARCHAR(32) NOT NULL   COMMENT 'ID' ,
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
    `id` VARCHAR(32) NOT NULL   COMMENT 'ID' ,
    `task_id` VARCHAR(32) NOT NULL   COMMENT '任务ID' ,
    `task_name` VARCHAR(255) NOT NULL   COMMENT '任务名称快照' ,
    `organization_id` VARCHAR(32) NOT NULL   COMMENT '组织ID' ,
    `run_id` VARCHAR(100) NOT NULL   COMMENT '执行ID' ,
    `execute_time` BIGINT NOT NULL   COMMENT '执行时间' ,
    `execute_reason` VARCHAR(500) NOT NULL   COMMENT '触发原因' ,
    `status` VARCHAR(50) NOT NULL   COMMENT '状态' ,
    `result` VARCHAR(255)    COMMENT '结果' ,
    PRIMARY KEY (id)
)  COMMENT = '执行记录'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_task_id ON agent_task_execute_log(task_id ASC);

CREATE TABLE agent_model_usage(
    `id` VARCHAR(32) NOT NULL   COMMENT 'ID' ,
    `model_id` VARCHAR(32) NOT NULL   COMMENT '模型ID' ,
    `run_id` VARCHAR(32) NOT NULL   COMMENT '执行ID' ,
    `user_id` VARCHAR(32) NOT NULL   COMMENT '用户ID' ,
    `input_tokens` BIGINT    COMMENT '输入消耗' ,
    `output_tokens` BIGINT    COMMENT '输出消耗' ,
    `total_tokens` BIGINT    COMMENT '累计调用' ,
    `call_count` BIGINT    COMMENT '调用次数' ,
    `fallback_count` BIGINT    COMMENT '降级次数' ,
    `success_count` BIGINT    COMMENT '成功次数' ,
    `failure_count` BIGINT    COMMENT '失败次数' ,
    `total_latency_ms` BIGINT    COMMENT '总延迟毫秒' ,
    `organization_id` VARCHAR(32) NOT NULL   COMMENT '组织ID' ,
    `create_time` BIGINT NOT NULL   COMMENT '创建时间' ,
    PRIMARY KEY (id)
)  COMMENT = '模型用量'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_model_id ON agent_model_usage(model_id ASC);
CREATE INDEX idx_run_id ON agent_model_usage(run_id ASC);
CREATE INDEX idx_user_id ON agent_model_usage(user_id ASC);
CREATE INDEX idx_org_id ON agent_model_usage(organization_id ASC);

CREATE TABLE agent_trace(
    `id` VARCHAR(32) NOT NULL   COMMENT 'ID' ,
    `name` VARCHAR(255) NOT NULL   COMMENT '名称' ,
    `status` VARCHAR(10)    COMMENT '状态' ,
    `operator` VARCHAR(32) NOT NULL   COMMENT '操作人' ,
    `call_time` BIGINT NOT NULL   COMMENT '执行时间' ,
    `call_ip` VARCHAR(50)    COMMENT '执行IP' ,
    `run_id` VARCHAR(32) NOT NULL   COMMENT '执行ID' ,
    `organization_id` VARCHAR(32) NOT NULL   COMMENT '组织ID' ,
    PRIMARY KEY (id)
)  COMMENT = 'AI执行日志'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_operator ON agent_trace(operator ASC);
CREATE INDEX idx_org_id ON agent_trace(organization_id ASC);
CREATE INDEX idx_run_id ON agent_trace(run_id ASC);

CREATE TABLE agent_trace_event(
    `id` VARCHAR(32) NOT NULL   COMMENT 'ID' ,
    `prompt` BLOB    COMMENT '原始输入' ,
    `trace` BLOB    COMMENT '响应内容' ,
    PRIMARY KEY (id)
)  COMMENT = 'AI执行日志详情表'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

CREATE TABLE agent_mcp_config(
    `id` VARCHAR(32) NOT NULL   COMMENT 'ID' ,
    `name` VARCHAR(255) NOT NULL   COMMENT '配置名称' ,
    `description` VARCHAR(1000)    COMMENT '描述' ,
    `config_json` TEXT(255) NOT NULL   COMMENT 'JSON配置' ,
    `user_id` VARCHAR(32) NOT NULL   COMMENT '用户ID' ,
    `organization_id` VARCHAR(32) NOT NULL   COMMENT '组织ID' ,
    `create_time` BIGINT NOT NULL   COMMENT '创建时间' ,
    `update_time` BIGINT NOT NULL   COMMENT '更新时间' ,
    `create_user` VARCHAR(32) NOT NULL   COMMENT '创建人' ,
    `update_user` VARCHAR(32) NOT NULL   COMMENT '更新人' ,
    PRIMARY KEY (id)
)  COMMENT = 'MCP配置'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_org_id ON agent_mcp_config(organization_id ASC);
CREATE INDEX idx_user_id ON agent_mcp_config(user_id ASC);

-- agent ddl end

ALTER TABLE follow_up_plan
    ADD COLUMN comment_count BIGINT NOT NULL DEFAULT 0 COMMENT '评论总数，包含回复';
ALTER TABLE follow_up_record
    ADD COLUMN comment_count BIGINT NOT NULL DEFAULT 0 COMMENT '评论总数，包含回复';

CREATE TABLE follow_up_plan_comment
(
    `id`              VARCHAR(32)   NOT NULL COMMENT 'ID',
    `resource_id`     VARCHAR(32)   NOT NULL COMMENT '跟进计划ID',
    `parent_id`       VARCHAR(32)            COMMENT '顶层评论ID',
    `reply_to_user_id` VARCHAR(32)           COMMENT '被回复人用户ID',
    `content`         VARCHAR(512) NOT NULL COMMENT '评论内容',
    `organization_id` VARCHAR(32)   NOT NULL COMMENT '组织ID',
    `create_time`     BIGINT        NOT NULL COMMENT '创建时间',
    `update_time`     BIGINT        NOT NULL COMMENT '更新时间',
    `create_user`     VARCHAR(32)   NOT NULL COMMENT '创建人',
    `update_user`     VARCHAR(32)   NOT NULL COMMENT '更新人',
    PRIMARY KEY (`id`)
) COMMENT = '跟进计划评论'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_parent_id ON follow_up_plan_comment (parent_id);
CREATE INDEX idx_resource_id ON follow_up_plan_comment (resource_id);

CREATE TABLE follow_up_record_comment
(
    `id`              VARCHAR(32)   NOT NULL COMMENT 'ID',
    `resource_id`     VARCHAR(32)   NOT NULL COMMENT '跟进记录ID',
    `parent_id`       VARCHAR(32)            COMMENT '顶层评论ID',
    `reply_to_user_id` VARCHAR(32)           COMMENT '被回复人用户ID',
    `content`         VARCHAR(512) NOT NULL COMMENT '评论内容',
    `organization_id` VARCHAR(32)   NOT NULL COMMENT '组织ID',
    `create_time`     BIGINT        NOT NULL COMMENT '创建时间',
    `update_time`     BIGINT        NOT NULL COMMENT '更新时间',
    `create_user`     VARCHAR(32)   NOT NULL COMMENT '创建人',
    `update_user`     VARCHAR(32)   NOT NULL COMMENT '更新人',
    PRIMARY KEY (`id`)
) COMMENT = '跟进记录评论'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_parent_id ON follow_up_record_comment (parent_id);
CREATE INDEX idx_resource_id ON follow_up_record_comment (resource_id);

CREATE TABLE follow_up_plan_comment_mention
(
    `id`         VARCHAR(32) NOT NULL COMMENT 'ID',
    `comment_id` VARCHAR(32) NOT NULL COMMENT '跟进计划评论ID',
    `user_id`    VARCHAR(32) NOT NULL COMMENT '被@用户ID',
    PRIMARY KEY (`id`)
) COMMENT = '跟进计划评论@用户关系'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_comment_id ON follow_up_plan_comment_mention (comment_id);

CREATE TABLE follow_up_record_comment_mention
(
    `id`         VARCHAR(32) NOT NULL COMMENT 'ID',
    `comment_id` VARCHAR(32) NOT NULL COMMENT '跟进记录评论ID',
    `user_id`    VARCHAR(32) NOT NULL COMMENT '被@用户ID',
    PRIMARY KEY (`id`)
) COMMENT = '跟进记录评论@用户关系'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_comment_id ON follow_up_record_comment_mention (comment_id);


-- 设置非必填
ALTER TABLE follow_up_plan MODIFY COLUMN `method` varchar(32) NULL COMMENT '跟进方式';
ALTER TABLE follow_up_plan MODIFY COLUMN estimated_time bigint NULL COMMENT '预计开始时间';
ALTER TABLE follow_up_record MODIFY COLUMN follow_time bigint NULL COMMENT '跟进时间';
ALTER TABLE follow_up_record MODIFY COLUMN follow_method varchar(32) NULL COMMENT '跟进方式';


-- set innodb lock wait timeout to default
SET SESSION innodb_lock_wait_timeout = DEFAULT;
