-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

ALTER TABLE custom_form_data ADD COLUMN approval_status VARCHAR(50) NOT NULL DEFAULT 'NONE' COMMENT '审批状态';

ALTER TABLE custom_form_data ADD COLUMN approved TINYINT(1) DEFAULT 0 NULL COMMENT '是否审批通过过';

-- 模型配置支持系统层级和个人层级
ALTER TABLE agent_model
    ADD COLUMN `scope` VARCHAR(10) NOT NULL DEFAULT 'SYSTEM' COMMENT '配置层级：SYSTEM 系统，USER 个人' AFTER `organization_id`,
    ADD COLUMN `user_id` VARCHAR(32) NULL COMMENT '个人模型所属用户ID' AFTER `scope`;

CREATE INDEX idx_agent_model_scope_user ON agent_model (organization_id, `scope`, user_id);

-- 记录用户在各组织中最后一次对话选中的模型
CREATE TABLE agent_user_model_preference
(
    `organization_id` VARCHAR(32) NOT NULL COMMENT '组织ID',
    `user_id`         VARCHAR(32) NOT NULL COMMENT '用户ID',
    `model_id`        VARCHAR(32) NOT NULL COMMENT '模型ID',
    `update_time`     BIGINT      NOT NULL COMMENT '更新时间',
    PRIMARY KEY (`organization_id`, `user_id`)
) COMMENT = '用户模型偏好'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

-- set innodb lock wait timeout to default
SET SESSION innodb_lock_wait_timeout = DEFAULT;
