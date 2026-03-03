-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

CREATE TABLE sales_order(
  `id` VARCHAR(32) NOT NULL   COMMENT 'id' ,
  `number` VARCHAR(50) NOT NULL   COMMENT '编号' ,
  `name` VARCHAR(255) NOT NULL   COMMENT '订单名称' ,
  `customer_id` VARCHAR(32) NOT NULL   COMMENT '客户id' ,
  `contract_id` VARCHAR(32) NOT NULL   COMMENT '合同id' ,
  `owner` VARCHAR(32) NOT NULL   COMMENT '订单负责人' ,
  `amount` DECIMAL(20,10)    COMMENT '金额' ,
  `status` VARCHAR(50) NOT NULL   COMMENT '订单状态' ,
  `organization_id` VARCHAR(32) NOT NULL   COMMENT '组织id' ,
  `create_time` BIGINT NOT NULL   COMMENT '创建时间' ,
  `update_time` BIGINT NOT NULL   COMMENT '更新时间' ,
  `create_user` VARCHAR(32) NOT NULL   COMMENT '创建人' ,
  `update_user` VARCHAR(32) NOT NULL   COMMENT '更新人' ,
  PRIMARY KEY (id)
)  COMMENT = '订单'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_customer_id ON sales_order(customer_id ASC);
CREATE INDEX idx_contract_id ON sales_order(contract_id ASC);
CREATE INDEX idx_owner ON sales_order(owner ASC);
CREATE INDEX idx_name ON sales_order(name ASC);


CREATE TABLE sales_order_field(
    `id` VARCHAR(32) NOT NULL   COMMENT 'id' ,
    `resource_id` VARCHAR(32) NOT NULL   COMMENT '订单id' ,
    `field_id` VARCHAR(32) NOT NULL   COMMENT '自定义属性id' ,
    `field_value` VARCHAR(255) NOT NULL   COMMENT '自定义属性值' ,
    `ref_sub_id` VARCHAR(32)    COMMENT '引用子表格ID;关联的子表格字段ID' ,
    `row_id` VARCHAR(32)    COMMENT '子表格行实例ID;行实例数据ID' ,
    PRIMARY KEY (id)
)  COMMENT = '订单自定义属性'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_resource_id ON sales_order_field(resource_id ASC);


CREATE TABLE sales_order_field_blob(
   `id` VARCHAR(32) NOT NULL   COMMENT 'id' ,
   `resource_id` VARCHAR(32) NOT NULL   COMMENT '订单id' ,
   `field_id` VARCHAR(32) NOT NULL   COMMENT '自定义属性id' ,
   `field_value` TEXT NOT NULL   COMMENT '自定义属性值' ,
   `ref_sub_id` VARCHAR(32)    COMMENT '引用子表格ID;关联的子表格字段ID' ,
   `row_id` VARCHAR(32)    COMMENT '子表格行实例ID;行实例数据ID' ,
   PRIMARY KEY (id)
)  COMMENT = '订单自定义属性大文本'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_resource_id ON sales_order_field_blob(resource_id ASC);

CREATE TABLE sales_order_snapshot(
   `id` VARCHAR(32) NOT NULL   COMMENT 'id' ,
   `order_id` VARCHAR(32) NOT NULL   COMMENT '订单id' ,
   `order_prop` LONGTEXT    COMMENT '订单属性快照' ,
   `order_value` TEXT    COMMENT '订单值快照' ,
   PRIMARY KEY (id)
)  COMMENT = '订单快照'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_order_id ON sales_order_snapshot(order_id ASC);


-- set innodb lock wait timeout to default
SET SESSION innodb_lock_wait_timeout = DEFAULT;