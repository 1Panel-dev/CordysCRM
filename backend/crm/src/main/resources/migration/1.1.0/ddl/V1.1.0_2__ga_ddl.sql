-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

alter table sys_message_task
    add we_com_enable bit default b'0' null comment '企业微信启用' after sys_enable;

CREATE TABLE dashboard_module
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
) COMMENT = '仪表板模块'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_organization_id ON dashboard_module (organization_id ASC);
CREATE INDEX idx_name ON dashboard_module (name ASC);
CREATE INDEX idx_pos ON dashboard_module (pos ASC);
CREATE INDEX idx_parent_id ON dashboard_module (parent_id ASC);


CREATE TABLE dashboard
(
    `id`                  VARCHAR(32)  NOT NULL COMMENT 'id',
    `name`                VARCHAR(255) NOT NULL COMMENT '名称',
    `resource_id`         VARCHAR(50)  NOT NULL COMMENT '三方资源id',
    `dashboard_module_id` VARCHAR(32)  NOT NULL COMMENT '模块id',
    `organization_id`     VARCHAR(32)  NOT NULL COMMENT '组织id',
    `pos`                 BIGINT       NOT NULL DEFAULT 0 COMMENT '同一节点下顺序',
    `scope_id`            TEXT         NOT NULL COMMENT '范围',
    `description`         VARCHAR(1000) COMMENT '描述',
    `create_time`         BIGINT       NOT NULL COMMENT '创建时间',
    `update_time`         BIGINT       NOT NULL COMMENT '更新时间',
    `create_user`         VARCHAR(32)  NOT NULL COMMENT '创建人',
    `update_user`         VARCHAR(32)  NOT NULL COMMENT '更新人',
    PRIMARY KEY (id)
) COMMENT = '仪表板'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_name ON dashboard (name ASC);
CREATE INDEX idx_dashboard_module_id ON dashboard (dashboard_module_id ASC);


ALTER TABLE opportunity
    ADD COLUMN actual_end_time BIGINT COMMENT '实际结束时间';
ALTER TABLE opportunity
    ADD COLUMN failure_reason VARCHAR(50) COMMENT '失败原因';


CREATE TABLE sys_license
(
    `id`           VARCHAR(32) NOT NULL COMMENT 'id',
    `license_code` LONGTEXT    NOT NULL COMMENT 'license_code',
    `create_time`  BIGINT      NOT NULL COMMENT '创建时间',
    `update_time`  BIGINT      NOT NULL COMMENT '更新时间',
    `create_user`  VARCHAR(32) NOT NULL COMMENT '创建人',
    `update_user`  VARCHAR(32) NOT NULL COMMENT '更新人',
    PRIMARY KEY (id)
) COMMENT = 'license'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;


CREATE TABLE dashboard_collection
(
    `id`           VARCHAR(32) NOT NULL COMMENT 'id',
    `user_id`      VARCHAR(32) NOT NULL COMMENT '用户id',
    `dashboard_id` VARCHAR(32) NOT NULL COMMENT '仪表板id',
    `create_time`  BIGINT      NOT NULL COMMENT '创建时间',
    `update_time`  BIGINT      NOT NULL COMMENT '更新时间',
    `create_user`  VARCHAR(32) NOT NULL COMMENT '创建人',
    `update_user`  VARCHAR(32) NOT NULL COMMENT '更新人',
    PRIMARY KEY (id, user_id, dashboard_id)
) COMMENT = '仪表板收藏'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_user_id ON dashboard_collection (user_id ASC);


-- set innodb lock wait timeout to default
SET SESSION innodb_lock_wait_timeout = DEFAULT;



