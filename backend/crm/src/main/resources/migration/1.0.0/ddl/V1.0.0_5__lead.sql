-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

CREATE TABLE lead_pool
(
    `id`              VARCHAR(32)   NOT NULL COMMENT 'id',
    `name`            VARCHAR(255)  NOT NULL COMMENT '线索池名称',
    `scope_id`        VARCHAR(1000) NOT NULL COMMENT '成员id',
    `organization_id` VARCHAR(32)   NOT NULL COMMENT '组织架构id',
    `owner_id`        VARCHAR(1000) NOT NULL COMMENT '管理员id',
    `enable`          BIT(1)        NOT NULL DEFAULT 1 COMMENT '启用/禁用',
    `create_time`     BIGINT        NOT NULL COMMENT '创建时间',
    `update_time`     BIGINT        NOT NULL COMMENT '更新时间',
    `create_user`     VARCHAR(32)   NOT NULL COMMENT '创建人',
    `update_user`     VARCHAR(32)   NOT NULL COMMENT '更新人',
    PRIMARY KEY (id)
) COMMENT = '线索池'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_organization_id ON lead_pool (organization_id ASC);

CREATE TABLE lead_pool_pick_rule
(
    `id`                 VARCHAR(32) NOT NULL COMMENT 'ID',
    `pool_id`            VARCHAR(32) NOT NULL COMMENT '线索池ID',
    `limit_on_number`    BIT(1)      NOT NULL DEFAULT 1 COMMENT '是否限制领取数量',
    `pick_number`        INT COMMENT '领取数量',
    `limit_pre_owner`    BIT(1)      NOT NULL DEFAULT 1 COMMENT '是否限制前归属人领取',
    `pick_interval_days` INT COMMENT '领取间隔天数',
    `create_user`        VARCHAR(32) NOT NULL COMMENT '创建人',
    `create_time`        BIGINT      NOT NULL COMMENT '创建时间',
    `update_user`        VARCHAR(32) NOT NULL COMMENT '更新人',
    `update_time`        BIGINT      NOT NULL COMMENT '更新时间',
    PRIMARY KEY (id)
) COMMENT = '线索池领取规则'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_pool_id ON lead_pool_pick_rule (pool_id ASC);

CREATE TABLE lead_pool_recycle_rule
(
    `id`            VARCHAR(32) NOT NULL COMMENT 'ID',
    `pool_id`       VARCHAR(32) NOT NULL COMMENT '线索池ID',
    `expire_notice` BIT(1)      NOT NULL DEFAULT 1 COMMENT '到期提醒',
    `notice_days`   INT COMMENT '提前提醒天数',
    `auto`          BIT(1)      NOT NULL DEFAULT 0 COMMENT '自动回收',
    `operator`      VARCHAR(10) COMMENT '操作符',
    `condition`     TEXT COMMENT '回收条件',
    `create_time`   BIGINT      NOT NULL COMMENT '创建时间',
    `update_time`   BIGINT      NOT NULL COMMENT '更新时间',
    `create_user`   VARCHAR(32) NOT NULL COMMENT '创建人',
    `update_user`   VARCHAR(32) NOT NULL COMMENT '更新人',
    PRIMARY KEY (id)
) COMMENT = '线索池回收规则'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_pool_id ON lead_pool_recycle_rule (pool_id ASC);

CREATE TABLE lead_pool_relation
(
    `id`          VARCHAR(32) NOT NULL COMMENT 'id',
    `pool_id`     VARCHAR(32) NOT NULL COMMENT '线索池id',
    `lead_id`     VARCHAR(32) NOT NULL COMMENT '线索id',
    `create_time` BIGINT      NOT NULL COMMENT '创建时间',
    `update_time` BIGINT      NOT NULL COMMENT '更新时间',
    `create_user` VARCHAR(32) NOT NULL COMMENT '创建人',
    `update_user` VARCHAR(32) NOT NULL COMMENT '更新人',
    PRIMARY KEY (id)
) COMMENT = '线索池中的线索'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_lead_id ON lead_pool_relation (lead_id ASC);
CREATE INDEX idx_pool_id ON lead_pool_relation (pool_id ASC);

CREATE TABLE lead_capacity
(
    `id`              VARCHAR(32)   NOT NULL COMMENT 'id',
    `organization_id` VARCHAR(32)   NOT NULL COMMENT '组织架构ID',
    `scope_id`        VARCHAR(1000) NOT NULL COMMENT '范围ID',
    `capacity`        INT(255)      NOT NULL DEFAULT 0 COMMENT '库容;0:不限制',
    `create_time`     BIGINT        NOT NULL COMMENT '创建时间',
    `update_time`     BIGINT        NOT NULL COMMENT '更新时间',
    `create_user`     VARCHAR(32)   NOT NULL COMMENT '创建人',
    `update_user`     VARCHAR(32)   NOT NULL COMMENT '更新人',
    PRIMARY KEY (id)
) COMMENT = '线索池库容设置'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_organization_id ON lead_capacity (organization_id ASC);

-- set innodb lock wait timeout to default
SET SESSION innodb_lock_wait_timeout = DEFAULT;
