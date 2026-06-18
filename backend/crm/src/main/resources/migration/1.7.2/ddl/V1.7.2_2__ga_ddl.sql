-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

-- approval_flow 增加 delete_execute 字段
ALTER TABLE approval_flow ADD COLUMN delete_execute tinyint(1) DEFAULT 0 COMMENT '删除时执行';

-- approval_node 增加 execute_time 字段
ALTER TABLE approval_node ADD COLUMN execute_time varchar(30) DEFAULT NULL COMMENT '执行时机：CREATE/UPDATE/DELETE';

-- approval_instance 增加 execute_time 字段
ALTER TABLE approval_instance ADD COLUMN execute_time varchar(30) DEFAULT NULL COMMENT '执行时机：CREATE/UPDATE/DELETE';

ALTER TABLE approval_instance ADD `comment` varchar(500) NULL COMMENT '变更说明';

ALTER TABLE contract ADD approved TINYINT(1) DEFAULT 0 NULL COMMENT '是否审批通过过';
ALTER TABLE opportunity_quotation ADD approved TINYINT(1) DEFAULT 0 NULL COMMENT '是否审批通过过';
ALTER TABLE contract_invoice ADD approved TINYINT(1) DEFAULT 0 NULL COMMENT '是否审批通过过';
ALTER TABLE sales_order ADD approved TINYINT(1) DEFAULT 0 NULL COMMENT '是否审批通过过';

-- approval_instance 增加 update_fields 字段
ALTER TABLE approval_instance ADD COLUMN update_fields VARCHAR(2000) DEFAULT NULL COMMENT '编辑时修改的字段列表';

CREATE TABLE stage_advanced_config
(
    `id`              VARCHAR(32) NOT NULL COMMENT 'id',
    `origin_id`       VARCHAR(32) NOT NULL COMMENT '源id',
    `target_id`       VARCHAR(32) NOT NULL COMMENT '目标id',
    `enable`          TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否允许流转',
    `field_config`    TEXT(255) COMMENT '字段配置',
    `module_type`     VARCHAR(20) COMMENT '模块类型(order-订单/contract-合同)',
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

CREATE INDEX idx_type ON stage_advanced_config (module_type ASC);


ALTER TABLE sales_order_stage_config ADD COLUMN circulation_type VARCHAR(50) DEFAULT 'NORMAL' COMMENT '流转类型(NORMAL-普通，ADVANCED-高级)';
ALTER TABLE contract_stage_config ADD COLUMN circulation_type VARCHAR(50) DEFAULT 'NORMAL' COMMENT '流转类型(NORMAL-普通，ADVANCED-高级)';

-- set innodb lock wait timeout to default
SET SESSION innodb_lock_wait_timeout = DEFAULT;
