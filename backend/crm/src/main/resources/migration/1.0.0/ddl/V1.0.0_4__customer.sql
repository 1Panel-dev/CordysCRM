-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

CREATE TABLE customer
(
    `id`                VARCHAR(32)  NOT NULL COMMENT 'id',
    `name`              VARCHAR(255) NOT NULL COMMENT '客户名称',
    `create_time`       BIGINT       NOT NULL COMMENT '创建时间',
    `update_time`       BIGINT       NOT NULL COMMENT '更新时间',
    `create_user`       VARCHAR(32)  NOT NULL COMMENT '创建人',
    `update_user`       VARCHAR(32)  NOT NULL COMMENT '更新人',
    `tags`              VARCHAR(1000) COMMENT '标签',
    `is_in_shared_pool` BIT(1)       NOT NULL DEFAULT 0 COMMENT '是否在公海池',
    `deal_status`       VARCHAR(255) NOT NULL COMMENT '最终成交状态',
    `organization_id`   VARCHAR(32)  NOT NULL COMMENT '组织id',
    PRIMARY KEY (id)
) COMMENT = '客户'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_organization_id ON customer (organization_id ASC);

CREATE TABLE customer_pool
(
    `id`              VARCHAR(32)   NOT NULL COMMENT 'id',
    `scope_id`        VARCHAR(1000) NOT NULL COMMENT '范围ID',
    `organization_id` VARCHAR(32)   NOT NULL COMMENT '组织ID',
    `name`            VARCHAR(255)  NOT NULL COMMENT '公海池名称',
    `owner_id`        VARCHAR(1000) NOT NULL COMMENT '管理员ID',
    `enable`          BIT(1)        NOT NULL DEFAULT 1 COMMENT '启用/禁用',
    `create_time`     BIGINT        NOT NULL COMMENT '创建时间',
    `update_time`     BIGINT        NOT NULL COMMENT '更新时间',
    `create_user`     VARCHAR(32)   NOT NULL COMMENT '创建人',
    `update_user`     VARCHAR(32)   NOT NULL COMMENT '更新人',
    PRIMARY KEY (id)
) COMMENT = '公海池'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_organization_id ON customer_pool (organization_id ASC);

CREATE TABLE customer_pool_pick_rule
(
    `id`                 VARCHAR(32) NOT NULL COMMENT 'ID',
    `pool_id`            VARCHAR(32) NOT NULL COMMENT '公海池ID',
    `limit_on_number`    BIT(1)      NOT NULL DEFAULT 1 COMMENT '是否限制领取数量',
    `pick_number`        INT COMMENT '领取数量',
    `limit_pre_owner`    BIT(1)      NOT NULL DEFAULT 1 COMMENT '是否限制前归属人领取',
    `pick_interval_days` INT COMMENT '领取间隔天数',
    `create_user`        VARCHAR(32) NOT NULL COMMENT '创建人',
    `create_time`        BIGINT      NOT NULL COMMENT '创建时间',
    `update_user`        VARCHAR(32) NOT NULL COMMENT '更新人',
    `update_time`        BIGINT      NOT NULL COMMENT '更新时间',
    PRIMARY KEY (id)
) COMMENT = '公海池领取规则'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_pool_id ON customer_pool_pick_rule (pool_id ASC);

CREATE TABLE customer_pool_recycle_rule
(
    `id`            VARCHAR(32) NOT NULL COMMENT 'ID',
    `pool_id`       VARCHAR(32) NOT NULL COMMENT '公海池ID',
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
) COMMENT = '公海池回收规则'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_pool_id ON customer_pool_recycle_rule (pool_id ASC);

CREATE TABLE customer_pool_relation
(
    `id`          VARCHAR(32) NOT NULL COMMENT 'id',
    `customer_id` VARCHAR(32) NOT NULL COMMENT '客户id',
    `pool_id`     VARCHAR(32) NOT NULL COMMENT '公海id',
    `create_time` BIGINT      NOT NULL COMMENT '创建时间',
    `update_time` BIGINT      NOT NULL COMMENT '更新时间',
    `create_user` VARCHAR(32) NOT NULL COMMENT '创建人',
    `update_user` VARCHAR(32) NOT NULL COMMENT '更新人',
    PRIMARY KEY (id)
) COMMENT = '公海客户'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_customer_id ON customer_pool_relation (customer_id ASC);
CREATE INDEX idx_pool_id ON customer_pool_relation (pool_id ASC);

CREATE TABLE customer_capacity
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
) COMMENT = '客户容量设置'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_organization_id ON customer_capacity (organization_id ASC);


-- set innodb lock wait timeout to default
SET SESSION innodb_lock_wait_timeout = DEFAULT;