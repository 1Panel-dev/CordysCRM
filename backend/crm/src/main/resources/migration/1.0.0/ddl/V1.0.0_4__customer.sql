-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

CREATE TABLE customer(
     `id` VARCHAR(32) NOT NULL   COMMENT 'id' ,
     `name` VARCHAR(255) NOT NULL   COMMENT '客户名称' ,
     `create_time` BIGINT NOT NULL   COMMENT '创建时间' ,
     `update_time` BIGINT NOT NULL   COMMENT '更新时间' ,
     `create_user` VARCHAR(32) NOT NULL   COMMENT '创建人' ,
     `update_user` VARCHAR(32) NOT NULL   COMMENT '更新人' ,
     `tags` VARCHAR(1000)    COMMENT '标签' ,
     `is_in_shared_pool` BIT(1) NOT NULL  DEFAULT 0 COMMENT '是否在公海池' ,
     `deal_status` VARCHAR(255) NOT NULL   COMMENT '最终成交状态' ,
     `organization_id` VARCHAR(32) NOT NULL   COMMENT '组织id' ,
     PRIMARY KEY (id)
)  COMMENT = '客户'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_organization_id ON customer(organization_id ASC);


-- set innodb lock wait timeout to default
SET SESSION innodb_lock_wait_timeout = DEFAULT;