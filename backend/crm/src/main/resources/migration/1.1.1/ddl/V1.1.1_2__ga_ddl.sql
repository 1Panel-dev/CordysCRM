-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

-- Add pool pick rule
ALTER TABLE customer_pool_pick_rule
    ADD limit_new BIT(1) NOT NULL DEFAULT 0 COMMENT '是否限制新数据' after pick_interval_days;
ALTER TABLE customer_pool_pick_rule
    ADD new_pick_interval INT COMMENT '新数据领取保护' after limit_new;

ALTER TABLE clue_pool_pick_rule
    ADD limit_new BIT(1) NOT NULL DEFAULT 0 COMMENT '是否限制新数据' after pick_interval_days;
ALTER TABLE clue_pool_pick_rule
    ADD new_pick_interval INT COMMENT '新数据领取保护' after limit_new;


CREATE TABLE sys_user_view
(
    `id`              VARCHAR(32)  NOT NULL COMMENT 'id',
    `user_id`         VARCHAR(32)  NOT NULL COMMENT '用户id',
    `name`            VARCHAR(255) NOT NULL COMMENT '视图名称',
    `fixed`           BIT(1)       NOT NULL DEFAULT 0 COMMENT '是否固定',
    `resource_type`   VARCHAR(50)  NOT NULL COMMENT '资源类型(客户/线索/商机)',
    `organization_id` VARCHAR(32)  NOT NULL COMMENT '组织id',
    `pos`             BIGINT       NOT NULL COMMENT '排序',
    `enable`          bit(1)       NOT NULL DEFAULT 1 COMMENT '状态',
    `search_mode`     VARCHAR(10)  NOT NULL DEFAULT 'AND' COMMENT '匹配模式(AND/OR)',
    `create_time`     BIGINT       NOT NULL COMMENT '创建时间',
    `update_time`     BIGINT       NOT NULL COMMENT '更新时间',
    `create_user`     VARCHAR(32)  NOT NULL COMMENT '创建人',
    `update_user`     VARCHAR(32)  NOT NULL COMMENT '更新人',
    PRIMARY KEY (id)
) COMMENT = '用户视图'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_user_id ON sys_user_view (user_id ASC);
CREATE INDEX idx_name ON sys_user_view (name ASC);
CREATE INDEX idx_resource_type ON sys_user_view (resource_type ASC);
CREATE INDEX idx_organization_id ON sys_user_view (organization_id ASC);


CREATE TABLE sys_user_view_condition
(
    `id`               VARCHAR(32)  NOT NULL COMMENT 'id',
    `sys_user_view_id` VARCHAR(32)  NOT NULL COMMENT '视图id',
    `name`             VARCHAR(255) NOT NULL COMMENT '参数名称',
    `value`            TEXT(255) COMMENT '参数值',
    `value_type`       TEXT(255) COMMENT '参数值类型',
    `type`             VARCHAR(20) COMMENT '类型',
    `multiple_value`   BIT(1)       NOT NULL DEFAULT 0 COMMENT '是否是多选值',
    `operator`         VARCHAR(20) COMMENT '操作符',
    `create_time`      BIGINT       NOT NULL COMMENT '创建时间',
    `update_time`      BIGINT       NOT NULL COMMENT '更新时间',
    `create_user`      VARCHAR(32)  NOT NULL COMMENT '创建人',
    `update_user`      VARCHAR(32)  NOT NULL COMMENT '更新人',
    PRIMARY KEY (id)
) COMMENT = '用户视图详情'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_sys_user_view_id ON sys_user_view_condition (sys_user_view_id ASC);

-- Dict config
CREATE TABLE sys_dict
(
    `id`              VARCHAR(32)  NOT NULL COMMENT 'id',
    `name`            VARCHAR(255) NOT NULL COMMENT '字典值',
    `module`          VARCHAR(20)  NOT NULL COMMENT '字典模块',
    `type`            VARCHAR(10)  NOT NULL DEFAULT 'TEXT' COMMENT '字典值类型',
    `pos`             BIGINT       NOT NULL COMMENT '自定义排序' ,
    `organization_id` VARCHAR(32)  NOT NULL COMMENT '组织ID',
    `create_time`     BIGINT       NOT NULL COMMENT '创建时间',
    `update_time`     BIGINT       NOT NULL COMMENT '更新时间',
    `create_user`     VARCHAR(32)  NOT NULL COMMENT '创建人',
    `update_user`     VARCHAR(32)  NOT NULL COMMENT '更新人',
    PRIMARY KEY (id)
) COMMENT = '系统字典表'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_org_type ON sys_dict (organization_id ASC, type ASC);
CREATE INDEX idx_org_name ON sys_dict (organization_id ASC, name ASC);

-- Add customer/lead pool reason
ALTER TABLE customer
    ADD `reason_id` VARCHAR(32) COMMENT '公海原因ID';
CREATE INDEX idx_reason_id ON customer (reason_id);
ALTER TABLE clue
    ADD `reason_id` VARCHAR(32) COMMENT '线索池原因ID';
CREATE INDEX idx_reason_id ON clue (reason_id);

-- Add dict config
CREATE TABLE sys_dict_config
(
    `module`          VARCHAR(20) NOT NULL COMMENT '字典类型',
    `organization_id` VARCHAR(32) NOT NULL COMMENT '组织ID',
    `enabled`         BIT(1)      NOT NULL DEFAULT 0 COMMENT '是否启用',
    PRIMARY KEY (module, organization_id)
) COMMENT = '系统字典配置表'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;


CREATE TABLE customer_pool_hidden_field(
    `pool_id` VARCHAR(32) NOT NULL   COMMENT '公海池ID' ,
    `field_id` VARCHAR(32) NOT NULL   COMMENT '字段ID' ,
    PRIMARY KEY (pool_id,field_id)
)  COMMENT = '公海池隐藏字段'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;

CREATE TABLE clue_pool_hidden_field(
   `pool_id` VARCHAR(32) NOT NULL   COMMENT '线索池ID' ,
   `field_id` VARCHAR(32) NOT NULL   COMMENT '字段ID' ,
   PRIMARY KEY (pool_id,field_id)
)  COMMENT = '线索池隐藏字段'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;

-- set innodb lock wait timeout to default
SET SESSION innodb_lock_wait_timeout = DEFAULT;



