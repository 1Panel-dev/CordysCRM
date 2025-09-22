-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

CREATE TABLE sys_navigation
(
    `id`              VARCHAR(32) NOT NULL COMMENT 'id',
    `organization_id` VARCHAR(32) NOT NULL COMMENT '组织id',
    `navigation_key`  VARCHAR(20) NOT NULL COMMENT '导航栏key',
    `enable`          BIT(1)      NOT NULL DEFAULT 1 COMMENT '启用/禁用',
    `pos`             BIGINT      NOT NULL COMMENT '自定义排序',
    `create_user`     VARCHAR(32) NOT NULL COMMENT '创建人',
    `create_time`     BIGINT      NOT NULL COMMENT '创建时间',
    `update_user`     VARCHAR(32) NOT NULL COMMENT '更新人',
    `update_time`     BIGINT      NOT NULL COMMENT '更新时间',
    PRIMARY KEY (id)
) COMMENT = '系统导航设置'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_organization_id ON sys_navigation(organization_id ASC);

-- set innodb lock wait timeout to default
SET SESSION innodb_lock_wait_timeout = DEFAULT;



