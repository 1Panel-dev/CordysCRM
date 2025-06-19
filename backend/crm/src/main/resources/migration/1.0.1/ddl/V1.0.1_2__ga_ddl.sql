-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

CREATE TABLE export_task
(
    `id`              VARCHAR(32)  NOT NULL COMMENT 'id',
    `file_name`       VARCHAR(255) NOT NULL COMMENT '文件名',
    `resource_type`   VARCHAR(50)  NOT NULL COMMENT '资源类型',
    `file_id`         VARCHAR(32)  NOT NULL COMMENT '文件id',
    `status`          VARCHAR(50)  NOT NULL COMMENT '状态',
    `organization_id` VARCHAR(32)  NOT NULL COMMENT '组织id',
    `create_time`     BIGINT       NOT NULL COMMENT '创建时间',
    `update_time`     BIGINT       NOT NULL COMMENT '更新时间',
    `create_user`     VARCHAR(32)  NOT NULL COMMENT '创建人',
    `update_user`     VARCHAR(32)  NOT NULL COMMENT '更新人',
    PRIMARY KEY (id)
) COMMENT = '导出任务'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_organization_id ON export_task(organization_id ASC);
CREATE INDEX idx_status ON export_task(status ASC);
CREATE INDEX idx_resource_type ON export_task(resource_type ASC);
CREATE INDEX idx_create_time ON export_task(create_time ASC);
CREATE INDEX idx_create_user ON export_task(create_user ASC);

-- set innodb lock wait timeout to default
SET SESSION innodb_lock_wait_timeout = DEFAULT;



