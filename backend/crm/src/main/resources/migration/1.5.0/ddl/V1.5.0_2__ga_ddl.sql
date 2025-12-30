-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

CREATE TABLE sys_message_task_config
(
    `id`              VARCHAR(32)  NOT NULL COMMENT 'id',
    `organization_id` VARCHAR(50)  NOT NULL COMMENT '组织id',
    `event`           VARCHAR(255) NOT NULL COMMENT '通知事件类型',
    `task_type`       VARCHAR(64)  NOT NULL COMMENT '任务类型',
    `value`           TEXT COMMENT '通知配置值',
    PRIMARY KEY (id)
) COMMENT = '消息通知配置'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_organization_id ON sys_message_task_config (organization_id ASC);
CREATE INDEX idx_task_type ON sys_message_task_config (task_type ASC);
CREATE INDEX idx_event ON sys_message_task_config (event ASC);

-- set innodb lock wait timeout to default
SET SESSION innodb_lock_wait_timeout = DEFAULT;