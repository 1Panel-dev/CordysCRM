-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

-- approval_flow 增加 delete_execute 字段
ALTER TABLE approval_flow ADD COLUMN delete_execute tinyint(1) DEFAULT 0 COMMENT '删除时执行';

-- approval_node 增加 execute_time 字段
ALTER TABLE approval_node ADD COLUMN execute_time varchar(30) DEFAULT NULL COMMENT '执行时机：CREATE/UPDATE/DELETE';

-- approval_instance 增加 execute_time 字段
ALTER TABLE approval_instance ADD COLUMN execute_time varchar(30) DEFAULT NULL COMMENT '执行时机：CREATE/UPDATE/DELETE';

-- 记录审批过程中的中间数据
CREATE TABLE approval_resource_data(
   `id` VARCHAR(32) NOT NULL   COMMENT 'ID' ,
   `form_type` VARCHAR(50) NOT NULL   COMMENT '表单类型;表单类型：QUOTATION(报价)、CONTRACT(合同)、INVOICE(发票)、ORDER(订单)' ,
   `resource_id` VARCHAR(32) NOT NULL   COMMENT '资源ID' ,
   `execute_time` VARCHAR(30) NOT NULL   COMMENT '执行时机：CREATE/UPDATE/DELETE' ,
   `update_fields` VARCHAR(2000) NOT NULL   COMMENT '执行时机为UPDATE时，有修改的字段列表' ,
   PRIMARY KEY (id)
)  COMMENT = '审批过程中的中间数据'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_resource_id ON approval_resource_data(resource_id ASC);

CREATE TABLE stage_advanced_config
(
    `id`              VARCHAR(32) NOT NULL COMMENT 'id',
    `origin_id`       VARCHAR(32) NOT NULL COMMENT '源id',
    `target_id`       VARCHAR(32) NOT NULL COMMENT '目标id',
    `enable`          TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否允许流转',
    `field_config`    TEXT(255) COMMENT '字段配置',
    `type`            VARCHAR(20) COMMENT '类型',
    `organization_id` VARCHAR(32) NOT NULL COMMENT '组织id',
    `create_time`     BIGINT      NOT NULL COMMENT '创建时间',
    `update_time`     BIGINT      NOT NULL COMMENT '更新时间',
    `create_user`     VARCHAR(32) NOT NULL COMMENT '创建人',
    `update_user`     VARCHAR(32) NOT NULL COMMENT '更新人',
    PRIMARY KEY (id)
) COMMENT = '状态流高级配置'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_type ON stage_advanced_config (type ASC);


ALTER TABLE sales_order_stage_config ADD COLUMN circulation_type VARCHAR(50) DEFAULT 'NORMAL' COMMENT '流转类型(NORMAL-普通，ADVANCED-高级)';
ALTER TABLE contract_stage_config ADD COLUMN circulation_type VARCHAR(50) DEFAULT 'NORMAL' COMMENT '流转类型(NORMAL-普通，ADVANCED-高级)';

-- set innodb lock wait timeout to default
SET SESSION innodb_lock_wait_timeout = DEFAULT;
