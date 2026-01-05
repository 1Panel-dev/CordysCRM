-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

CREATE TABLE sys_message_task_config
(
    `id`              VARCHAR(32)  NOT NULL COMMENT 'id',
    `organization_id` VARCHAR(50)  NOT NULL COMMENT '组织id',
    `event`           VARCHAR(255) NOT NULL COMMENT '通知事件类型',
    `task_type`       VARCHAR(64)  NOT NULL COMMENT '任务类型',
    `value`           TEXT COMMENT '通知配置值',
    PRIMARY KEY (id)
) COMMENT = '消息通知配置'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_organization_id ON sys_message_task_config (organization_id ASC);
CREATE INDEX idx_task_type ON sys_message_task_config (task_type ASC);
CREATE INDEX idx_event ON sys_message_task_config (event ASC);


CREATE TABLE business_title
(
    `id`                    VARCHAR(32)  NOT NULL COMMENT 'id',
    `business_name`         VARCHAR(255) NOT NULL COMMENT '公司名称',
    `type`                  VARCHAR(50)  NOT NULL COMMENT '来源类型',
    `identification_number` VARCHAR(255) NOT NULL COMMENT '纳税人识别号',
    `opening_bank`          VARCHAR(255) NOT NULL COMMENT '开户银行',
    `bank_account`          VARCHAR(50)  NOT NULL COMMENT '银行账号',
    `registration_address`  VARCHAR(255) NOT NULL COMMENT '注册地址',
    `phone_number`          VARCHAR(50)  NOT NULL COMMENT '注册电话',
    `registered_capital`    DECIMAL(20)  NOT NULL COMMENT '注册资本',
    `customer_size`         VARCHAR(50)  NOT NULL COMMENT '客户规模',
    `registration_number`   VARCHAR(255) NOT NULL COMMENT '工商注册号',
    `approval_status`       VARCHAR(50) COMMENT '审核状态',
    `unapproved_reason`     VARCHAR(255) COMMENT '不通过原因',
    `organization_id`       VARCHAR(50)  NOT NULL COMMENT '组织id',
    `create_time`           BIGINT       NOT NULL COMMENT '创建时间',
    `update_time`           BIGINT       NOT NULL COMMENT '更新时间',
    `create_user`           VARCHAR(32)  NOT NULL COMMENT '创建人',
    `update_user`           VARCHAR(32)  NOT NULL COMMENT '更新人',
    PRIMARY KEY (id)
) COMMENT = '工商抬头'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;


CREATE INDEX idx_organization_id ON business_title (organization_id ASC);
CREATE INDEX idx_business_name ON business_title (business_name ASC);


CREATE TABLE contract_invoice
(
    `id`                VARCHAR(32) NOT NULL COMMENT 'id',
    `number`            VARCHAR(50) NOT NULL COMMENT '编号',
    `contract_id`       VARCHAR(32) NOT NULL COMMENT '合同id',
    `owner`             VARCHAR(32) NOT NULL COMMENT '负责人',
    `amount`            DECIMAL(20, 10) COMMENT '开票金额',
    `invoice_type`      VARCHAR(50) COMMENT '发票类型',
    `tax_rate`          DECIMAL(20, 10) COMMENT '税率',
    `review_status`     VARCHAR(50) COMMENT '审核状态',
    `business_title_id` VARCHAR(50) COMMENT '工商抬头ID',
    `organization_id`   VARCHAR(32) NOT NULL COMMENT '组织id',
    `create_time`       BIGINT      NOT NULL COMMENT '创建时间',
    `update_time`       BIGINT      NOT NULL COMMENT '更新时间',
    `create_user`       VARCHAR(32) NOT NULL COMMENT '创建人',
    `update_user`       VARCHAR(32) NOT NULL COMMENT '更新人',
    PRIMARY KEY (id)
) COMMENT = '发票'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_contract_id ON contract_invoice (contract_id ASC);

CREATE TABLE contract_invoice_field
(
    `id`          VARCHAR(32)  NOT NULL COMMENT 'id',
    `resource_id` VARCHAR(32)  NOT NULL COMMENT '合同id',
    `field_id`    VARCHAR(32)  NOT NULL COMMENT '自定义属性id',
    `field_value` VARCHAR(255) NOT NULL COMMENT '自定义属性值',
    PRIMARY KEY (id)
) COMMENT = '发票自定义属性'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_resource_id ON contract_invoice_field (resource_id ASC);

CREATE TABLE contract_invoice_field_blob
(
    `id`          VARCHAR(32) NOT NULL COMMENT 'id',
    `resource_id` VARCHAR(32) NOT NULL COMMENT '合同id',
    `field_id`    VARCHAR(32) NOT NULL COMMENT '自定义属性id',
    `field_value` TEXT        NOT NULL COMMENT '自定义属性值',
    PRIMARY KEY (id)
) COMMENT = '发票自定义属性大文本'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_resource_id ON contract_invoice_field_blob (resource_id ASC);

CREATE TABLE contract_invoice_snapshot
(
    `id`            VARCHAR(32) NOT NULL COMMENT 'id',
    `invoice_id`    VARCHAR(32) NOT NULL COMMENT '合同id',
    `invoice_prop`  TEXT(255) COMMENT '表单属性快照',
    `invoice_value` TEXT(255) COMMENT '表单值快照',
    PRIMARY KEY (id)
) COMMENT = '发票快照'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_invoice_id ON contract_invoice_snapshot (invoice_id ASC);


-- set innodb lock wait timeout to default
SET SESSION innodb_lock_wait_timeout = DEFAULT;