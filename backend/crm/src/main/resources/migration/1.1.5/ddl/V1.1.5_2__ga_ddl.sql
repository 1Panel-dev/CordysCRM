-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;


CREATE TABLE sys_user_search_config
(
    `id`               VARCHAR(32)  NOT NULL COMMENT 'id',
    `field_id`         VARCHAR(32)  NOT NULL COMMENT '搜索字段id',
    `type`             VARCHAR(20)  NOT NULL COMMENT '字段类型',
    `business_key`     VARCHAR(50) COMMENT '业务字段key',
    `data_source_type` VARCHAR(50) COMMENT '数据源对应类型',
    `user_id`          VARCHAR(32)  NOT NULL COMMENT '用户id',
    `module_type`      VARCHAR(50)  NOT NULL COMMENT '搜索模块(customer/clue/等)',
    `sort_setting`     VARCHAR(255) NOT NULL COMMENT '模块顺序设置["customer","clue"]',
    `result_display`   BIT(1)       NOT NULL DEFAULT 0 COMMENT '是否展示有搜索结果的列表',
    `organization_id`  VARCHAR(32)  NOT NULL COMMENT '组织id',
    `create_time`      BIGINT       NOT NULL COMMENT '创建时间',
    `update_time`      BIGINT       NOT NULL COMMENT '更新时间',
    `create_user`      VARCHAR(32)  NOT NULL COMMENT '创建人',
    `update_user`      VARCHAR(32)  NOT NULL COMMENT '更新人',
    PRIMARY KEY (id)
) COMMENT = '用户全局搜索配置'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;


CREATE INDEX idx_user_id ON sys_user_search_config (user_id ASC);
CREATE INDEX ids_module_type ON sys_user_search_config (module_type ASC);


CREATE TABLE sys_search_field_mask_config
(
    `id`               VARCHAR(32) NOT NULL COMMENT 'id',
    `field_id`         VARCHAR(32) NOT NULL COMMENT '搜索字段id',
    `type`             VARCHAR(20) NOT NULL COMMENT '类型',
    `business_key`     VARCHAR(50) COMMENT '业务字段key',
    `data_source_type` VARCHAR(50) COMMENT '数据源对应类型',
    `module_type`      VARCHAR(50) NOT NULL COMMENT '搜索模块(customer/clue/等)',
    `organization_id`  VARCHAR(32) COMMENT '组织id',
    `create_time`      BIGINT      NOT NULL COMMENT '创建时间',
    `update_time`      BIGINT      NOT NULL COMMENT '更新时间',
    `create_user`      VARCHAR(32) NOT NULL COMMENT '创建人',
    `update_user`      VARCHAR(32) NOT NULL COMMENT '更新人',
    PRIMARY KEY (id)
) COMMENT = '搜索字段脱敏配置'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;


CREATE INDEX idx_module_type ON sys_search_field_mask_config (module_type ASC);
CREATE INDEX idx_organization_id ON sys_search_field_mask_config (organization_id ASC);
CREATE INDEX idx_field_id ON sys_search_field_mask_config (field_id ASC);

ALTER TABLE opportunity MODIFY contact_id VARCHAR(32) COMMENT '联系人ID';

create index idx_organization_id on customer_contact (organization_id);
create index idx_phone on customer_contact (phone);

create index idx_phone on clue (phone);



-- set innodb lock wait timeout to default
SET SESSION innodb_lock_wait_timeout = DEFAULT;