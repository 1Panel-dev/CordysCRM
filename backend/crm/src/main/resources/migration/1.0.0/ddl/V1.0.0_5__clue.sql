-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

CREATE TABLE clue_pool
(
    `id`              VARCHAR(32)  NOT NULL COMMENT 'id',
    `name`            VARCHAR(255) NOT NULL COMMENT '线索池名称',
    `scope_id`        TEXT         NOT NULL COMMENT '成员id',
    `organization_id` VARCHAR(32)  NOT NULL COMMENT '组织架构id',
    `owner_id`        TEXT         NOT NULL COMMENT '管理员id',
    `enable`          BIT(1)       NOT NULL DEFAULT 1 COMMENT '启用/禁用',
    `auto`            BIT(1)       NOT NULL DEFAULT 0 COMMENT '自动回收',
    `create_time`     BIGINT       NOT NULL COMMENT '创建时间',
    `update_time`     BIGINT       NOT NULL COMMENT '更新时间',
    `create_user`     VARCHAR(32)  NOT NULL COMMENT '创建人',
    `update_user`     VARCHAR(32)  NOT NULL COMMENT '更新人',
    PRIMARY KEY (id)
) COMMENT = '线索池'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_organization_id ON clue_pool (organization_id ASC);

CREATE TABLE clue_pool_pick_rule
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

CREATE INDEX idx_pool_id ON clue_pool_pick_rule (pool_id ASC);

CREATE TABLE clue_pool_recycle_rule
(
    `id`            VARCHAR(32) NOT NULL COMMENT 'ID',
    `pool_id`       VARCHAR(32) NOT NULL COMMENT '线索池ID',
    `expire_notice` BIT(1)      NOT NULL DEFAULT 1 COMMENT '到期提醒',
    `notice_days`   INT COMMENT '提前提醒天数',
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

CREATE INDEX idx_pool_id ON clue_pool_recycle_rule (pool_id ASC);

CREATE TABLE clue_pool_relation
(
    `id`                VARCHAR(32) NOT NULL COMMENT 'id',
    `clue_id`           VARCHAR(32) NOT NULL COMMENT '线索id',
    `pool_id`           VARCHAR(32) NOT NULL COMMENT '线索池id',
    `last_pick_user_id` VARCHAR(32) NOT NULL COMMENT '上一次领取人',
    `last_pick_time`    BIGINT(255) NOT NULL COMMENT '上一次领取时间',
    `picked`            BIT(1)      NOT NULL COMMENT '是否领取',
    `create_time`       BIGINT      NOT NULL COMMENT '创建时间',
    `update_time`       BIGINT      NOT NULL COMMENT '更新时间',
    `create_user`       VARCHAR(32) NOT NULL COMMENT '创建人',
    `update_user`       VARCHAR(32) NOT NULL COMMENT '更新人',
    PRIMARY KEY (id)
) COMMENT = '线索池中的线索'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_clue_id ON clue_pool_relation (clue_id ASC);
CREATE INDEX idx_pool_id ON clue_pool_relation (pool_id ASC);

CREATE TABLE clue_capacity
(
    `id`              VARCHAR(32) NOT NULL COMMENT 'id',
    `organization_id` VARCHAR(32) NOT NULL COMMENT '组织架构ID',
    `scope_id`        TEXT        NOT NULL COMMENT '范围ID',
    `capacity`        INT         NOT NULL DEFAULT 0 COMMENT '库容;0:不限制',
    `create_time`     BIGINT      NOT NULL COMMENT '创建时间',
    `update_time`     BIGINT      NOT NULL COMMENT '更新时间',
    `create_user`     VARCHAR(32) NOT NULL COMMENT '创建人',
    `update_user`     VARCHAR(32) NOT NULL COMMENT '更新人',
    PRIMARY KEY (id)
) COMMENT = '线索池库容设置'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_organization_id ON clue_capacity (organization_id ASC);

CREATE TABLE clue(
     `id` VARCHAR(32) NOT NULL   COMMENT 'id' ,
     `name` VARCHAR(255) NOT NULL   COMMENT '客户名称' ,
     `owner` VARCHAR(32)    COMMENT '负责人' ,
     `status` VARCHAR(30) NOT NULL   COMMENT '线索状态' ,
     `collection_time` BIGINT    COMMENT '领取时间' ,
     `contact` VARCHAR(255)    COMMENT '联系人名称' ,
     `phone` VARCHAR(20)    COMMENT '联系人电话' ,
     `organization_id` VARCHAR(32) NOT NULL   COMMENT '组织id' ,
     `create_time` BIGINT NOT NULL   COMMENT '创建时间' ,
     `update_time` BIGINT NOT NULL   COMMENT '更新时间' ,
     `create_user` VARCHAR(32) NOT NULL   COMMENT '创建人' ,
     `update_user` VARCHAR(32) NOT NULL   COMMENT '更新人' ,
     `transition_type` VARCHAR(30)   DEFAULT 'NONE' COMMENT '转移成客户或者线索' ,
     `transition_id` VARCHAR(32)    COMMENT '客户id或者线索id' ,
     `in_shared_pool` BIT(1) NOT NULL  DEFAULT 0 COMMENT '是否在线索池' ,
     PRIMARY KEY (id)
)  COMMENT = '线索'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_organization_id ON `clue`(organization_id ASC);

CREATE TABLE clue_field(
   `id` VARCHAR(32) NOT NULL   COMMENT 'id' ,
   `resource_id` VARCHAR(32) NOT NULL   COMMENT '线索id' ,
   `field_id` VARCHAR(32) NOT NULL   COMMENT '自定义属性id' ,
   `field_value` VARCHAR(255) NOT NULL   COMMENT '自定义属性值' ,
   PRIMARY KEY (id)
)  COMMENT = '线索自定义属性'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_resource_id_field_id_field_value ON clue_field(resource_id,field_id,field_value);


CREATE TABLE clue_field_blob(
    `id` VARCHAR(32) NOT NULL   COMMENT 'id' ,
    `resource_id` VARCHAR(32) NOT NULL   COMMENT '客户联系人id' ,
    `field_id` VARCHAR(32) NOT NULL   COMMENT '自定义属性id' ,
    `field_value` BLOB NOT NULL   COMMENT '自定义属性值' ,
    PRIMARY KEY (id)
)  COMMENT = '线索自定义属性大文本'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;

CREATE INDEX resource_id ON clue_field_blob(resource_id ASC);


-- set innodb lock wait timeout to default
SET SESSION innodb_lock_wait_timeout = DEFAULT;
