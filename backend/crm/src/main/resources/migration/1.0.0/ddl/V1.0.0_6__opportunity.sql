-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

CREATE TABLE opportunity_rule(
    `id`            VARCHAR(32) NOT NULL   COMMENT 'id' ,
    `name`          VARCHAR(255) NOT NULL   COMMENT '规则名称' ,
    `organization_id` VARCHAR(32) NOT NULL   COMMENT '组织ID' ,
    `owner_id`      TEXT NOT NULL   COMMENT '管理员ID' ,
    `scope_id`      TEXT NOT NULL   COMMENT '范围ID' ,
    `enable`        BIT(1) NOT NULL  DEFAULT 1 COMMENT '启用/禁用' ,
    `auto`          BIT(1) NOT NULL  DEFAULT 0 COMMENT '自动回收' ,
    `operator`      VARCHAR(10)    COMMENT '操作符' ,
    `condition`     TEXT    COMMENT '回收条件' ,
    `expire_notice` BIT(1) NOT NULL  DEFAULT 1 COMMENT '到期提醒' ,
    `notice_days`   INT    COMMENT '提前提醒天数' ,
    `create_time`   BIGINT NOT NULL   COMMENT '创建时间' ,
    `update_time`   BIGINT NOT NULL   COMMENT '更新时间' ,
    `create_user`   VARCHAR(32) NOT NULL   COMMENT '创建人' ,
    `update_user`   VARCHAR(32) NOT NULL   COMMENT '更新人' ,
    PRIMARY KEY (id)
)  COMMENT = '商机关闭规则'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_organization_id ON opportunity_rule(organization_id ASC);


CREATE TABLE opportunity(
    `id` VARCHAR(32) NOT NULL   COMMENT 'id' ,
    `customer_id` VARCHAR(32) NOT NULL   COMMENT '客户id' ,
    `name` VARCHAR(255) NOT NULL   COMMENT '商机名称' ,
    `amount` DECIMAL(10,2) NOT NULL   COMMENT '金额' ,
    `possible` DECIMAL(10,4) NOT NULL   COMMENT '可能性' ,
    `products` VARCHAR(1000) NOT NULL   COMMENT '意向产品' ,
    `organization_id` VARCHAR(32) NOT NULL   COMMENT '组织id' ,
    `update_user` VARCHAR(32) NOT NULL   COMMENT '更新人' ,
    `create_time` BIGINT NOT NULL   COMMENT '创建时间' ,
    `update_time` BIGINT NOT NULL   COMMENT '更新时间' ,
    `create_user` VARCHAR(32) NOT NULL   COMMENT '创建人' ,
    PRIMARY KEY (id)
)  COMMENT = '商机'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_customer_id ON opportunity(customer_id ASC);
CREATE INDEX idx_organization_id ON opportunity(organization_id ASC);


CREATE TABLE opportunity_field(
    `id` VARCHAR(32) NOT NULL   COMMENT 'id' ,
    `resource_id` VARCHAR(32) NOT NULL   COMMENT '商机id' ,
    `field_id` VARCHAR(32) NOT NULL   COMMENT '自定义属性id' ,
    `field_value` VARCHAR(255) NOT NULL   COMMENT '自定义属性值' ,
    PRIMARY KEY (id)
)  COMMENT = '商机自定义属性'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_resource_id_field_id_field_value ON opportunity_field(resource_id, field_id, field_value);

CREATE TABLE opportunity_field_blob(
   `id` VARCHAR(32) NOT NULL   COMMENT 'id' ,
   `resource_id` VARCHAR(32) NOT NULL   COMMENT '客户id' ,
   `field_id` VARCHAR(32) NOT NULL   COMMENT '自定义属性id' ,
   `field_value` BLOB NOT NULL   COMMENT '自定义属性值' ,
   PRIMARY KEY (id)
)  COMMENT = '商机自定义属性大文本'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_resource_id ON opportunity_field_blob(resource_id);

CREATE TABLE follow_up_record(
    `id` VARCHAR(32) NOT NULL   COMMENT 'id' ,
    `customer_id` VARCHAR(32)    COMMENT '客户id' ,
    `opportunity_id` VARCHAR(32)    COMMENT '商机id' ,
    `type` VARCHAR(32) NOT NULL   COMMENT '类型' ,
    `lead_id` VARCHAR(32)    COMMENT '线索id' ,
    `organization_id` VARCHAR(32) NOT NULL   COMMENT '组织id' ,
    `owner` VARCHAR(32) NOT NULL   COMMENT '负责人' ,
    `contact_id` VARCHAR(32) NOT NULL   COMMENT '联系人id' ,
    `create_time` BIGINT NOT NULL   COMMENT '创建时间' ,
    `update_time` BIGINT NOT NULL   COMMENT '更新时间' ,
    `create_user` VARCHAR(32) NOT NULL   COMMENT '创建人' ,
    `update_user` VARCHAR(32) NOT NULL   COMMENT '更新人' ,
    PRIMARY KEY (id)
)  COMMENT = '跟进记录'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_customer_id ON follow_up_record(customer_id ASC);
CREATE INDEX idx_organization_id ON follow_up_record(organization_id ASC);
CREATE INDEX idx_opportunity_id ON follow_up_record(opportunity_id ASC);
CREATE INDEX idx_lead_id ON follow_up_record(lead_id ASC);
CREATE INDEX idx_owner ON follow_up_record(owner ASC);
CREATE INDEX idx_contact_id ON follow_up_record(contact_id ASC);


CREATE TABLE follow_up_field(
    `id` VARCHAR(32) NOT NULL   COMMENT 'id' ,
    `resource_id` VARCHAR(32) NOT NULL   COMMENT '跟进记录id' ,
    `field_id` VARCHAR(32) NOT NULL   COMMENT '自定义属性id' ,
    `field_value` VARCHAR(255) NOT NULL   COMMENT '自定义属性值' ,
    PRIMARY KEY (id)
)  COMMENT = '跟进记录自定义属性'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_resource_id_field_id_field_value ON follow_up_field(resource_id,field_id,field_value);

CREATE TABLE follow_up_field_blob(
    `id` VARCHAR(32) NOT NULL   COMMENT 'id' ,
    `resource_id` VARCHAR(32) NOT NULL   COMMENT '跟进记录id' ,
    `field_id` VARCHAR(32) NOT NULL   COMMENT '自定义属性id' ,
    `field_value` BLOB NOT NULL   COMMENT '自定义属性值' ,
    PRIMARY KEY (id)
)  COMMENT = '跟进记录自定义属性大文本'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_resource_id ON follow_up_field_blob(resource_id);

-- set innodb lock wait timeout to default
SET SESSION innodb_lock_wait_timeout = DEFAULT;
