-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

CREATE TABLE sys_notification_config
(
    `id`              VARCHAR(32) NOT NULL COMMENT 'id',
    `organization_id` VARCHAR(50) NOT NULL COMMENT '组织id',
    `type`            VARCHAR(64) NOT NULL COMMENT '通知类型',
    `value`           TEXT COMMENT '通知配置值',
    PRIMARY KEY (id)
) COMMENT = '消息通知配置'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_organization_id ON sys_notification_config (organization_id ASC);
CREATE INDEX idx_type ON sys_notification_config (type ASC);

-- set innodb lock wait timeout to default
SET SESSION innodb_lock_wait_timeout = DEFAULT;