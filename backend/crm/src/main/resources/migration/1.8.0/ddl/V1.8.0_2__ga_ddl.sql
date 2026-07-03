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

-- set innodb lock wait timeout to default
SET SESSION innodb_lock_wait_timeout = DEFAULT;
