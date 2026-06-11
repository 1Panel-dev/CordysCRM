-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

-- modify sys_module_field_blob prop column to longtext
ALTER TABLE contract MODIFY COLUMN start_time bigint NULL;
ALTER TABLE contract MODIFY COLUMN end_time bigint NULL;

CREATE TABLE custom_form(
    `id` VARCHAR(32) NOT NULL   COMMENT 'id' ,
    `name` VARCHAR(255) NOT NULL   COMMENT '名称' ,
    `enable` BIT(1) NOT NULL  DEFAULT 0 COMMENT '是否启用' ,
    `organization_id` VARCHAR(32) NOT NULL   COMMENT '组织id' ,
    `create_time` BIGINT NOT NULL   COMMENT '创建时间' ,
    `update_time` BIGINT NOT NULL   COMMENT '更新时间' ,
    `create_user` VARCHAR(32) NOT NULL   COMMENT '创建人' ,
    `update_user` VARCHAR(32) NOT NULL   COMMENT '更新人' ,
    PRIMARY KEY (id)
)  COMMENT = '自定义表单'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;

CREATE TABLE custom_form_admin(
    `id` VARCHAR(32) NOT NULL   COMMENT 'id' ,
    `custom_form_id` VARCHAR(32) NOT NULL   COMMENT '自定义表单ID' ,
    `user_id` VARCHAR(32) NOT NULL   COMMENT '用户ID' ,
    PRIMARY KEY (id)
)  COMMENT = '自定义表单管理员'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_custom_form_id ON custom_form_admin(custom_form_id ASC);


CREATE TABLE custom_form_role(
    `id` VARCHAR(32) NOT NULL   COMMENT 'id' ,
    `name` VARCHAR(255) NOT NULL   COMMENT '名称' ,
    `custom_form_id` VARCHAR(32) NOT NULL   COMMENT '自定义表单ID' ,
    `internal_key` VARCHAR(50) NOT NULL   COMMENT '内置角色key' ,
    `create_time` BIGINT NOT NULL   COMMENT '创建时间' ,
    `update_time` BIGINT NOT NULL   COMMENT '更新时间' ,
    `create_user` VARCHAR(32) NOT NULL   COMMENT '创建人' ,
    `update_user` VARCHAR(32) NOT NULL   COMMENT '更新人' ,
    PRIMARY KEY (id)
)  COMMENT = '自定义表单角色'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_custom_form_id ON custom_form_role(custom_form_id ASC);

CREATE TABLE custom_form_role_user(
    `id` VARCHAR(32) NOT NULL   COMMENT 'id' ,
    `role_id` VARCHAR(32) NOT NULL   COMMENT '角色id' ,
    `user_id` VARCHAR(32) NOT NULL   COMMENT '用户id' ,
    `create_time` BIGINT NOT NULL   COMMENT '创建时间' ,
    `update_time` BIGINT NOT NULL   COMMENT '更新时间' ,
    `create_user` VARCHAR(32) NOT NULL   COMMENT '创建人' ,
    `update_user` VARCHAR(32) NOT NULL   COMMENT '更新人' ,
    PRIMARY KEY (id)
)  COMMENT = '自定义表单角色关联用户'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_role_id ON custom_form_role_user(role_id ASC);
CREATE INDEX idx_create_time ON custom_form_role_user(create_time DESC);


CREATE TABLE custom_form_data(
    `id` VARCHAR(32) NOT NULL   COMMENT 'id' ,
    `custom_form_id` VARCHAR(32)    COMMENT '自定义表单ID' ,
    `name` VARCHAR(255) NOT NULL   COMMENT '名称' ,
    `owner` VARCHAR(32) NOT NULL   COMMENT '负责人' ,
    `organization_id` VARCHAR(32) NOT NULL   COMMENT '组织id' ,
    `create_time` BIGINT NOT NULL   COMMENT '创建时间' ,
    `update_time` BIGINT NOT NULL   COMMENT '更新时间' ,
    `create_user` VARCHAR(32) NOT NULL   COMMENT '创建人' ,
    `update_user` VARCHAR(32) NOT NULL   COMMENT '更新人' ,
    PRIMARY KEY (id)
)  COMMENT = '自定义表单数据'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_custom_form_id ON custom_form_data(custom_form_id ASC);

CREATE TABLE custom_form_data_field(
    `id` VARCHAR(32) NOT NULL   COMMENT 'id' ,
    `resource_id` VARCHAR(32) NOT NULL   COMMENT '自定义表单数据id' ,
    `field_id` VARCHAR(32) NOT NULL   COMMENT '自定义属性id' ,
    `field_value` VARCHAR(255) NOT NULL   COMMENT '自定义属性值' ,
    PRIMARY KEY (id)
)  COMMENT = '自定义表单数据自定义属性'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_resource_id ON custom_form_data_field(resource_id ASC);

CREATE TABLE custom_form_data_field_blob(
    `id` VARCHAR(32) NOT NULL   COMMENT 'id' ,
    `resource_id` VARCHAR(32) NOT NULL   COMMENT '自定义表单数据id' ,
    `field_id` VARCHAR(32) NOT NULL   COMMENT '自定义属性id' ,
    `field_value` TEXT NOT NULL   COMMENT '自定义属性值' ,
    PRIMARY KEY (id)
)  COMMENT = '自定义表单数据自定义属性大文本'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_resource_id ON custom_form_data_field_blob(resource_id ASC);


SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'business_title'
              AND COLUMN_NAME = 'company_number'
        ),
        'SELECT 1',
        'ALTER TABLE business_title ADD COLUMN company_number BIGINT NOT NULL AUTO_INCREMENT UNIQUE COMMENT ''公司编号'''
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


ALTER TABLE approval_node_approver MODIFY COLUMN field_permissions text COMMENT '字段权限配置（JSON格式）';
ALTER TABLE approval_node_approver MODIFY COLUMN pass_post_config text COMMENT '审批通过后配置（JSON格式）';
ALTER TABLE approval_node_approver MODIFY COLUMN reject_post_config text COMMENT '审批驳回后配置（JSON格式）';

ALTER TABLE sys_operation_log ADD COLUMN request_source VARCHAR(32) DEFAULT 'WEB' COMMENT '请求来源';

-- set innodb lock wait timeout to default
SET SESSION innodb_lock_wait_timeout = DEFAULT;
