-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

CREATE TABLE opportunity_quotation
(
    `id`              VARCHAR(32)  NOT NULL COMMENT 'id',
    `name`            VARCHAR(255) NOT NULL COMMENT '名称',
    `opportunity_id`  VARCHAR(32)  NOT NULL COMMENT '商机id',
    `amount`          DECIMAL(14, 2) NOT NULL COMMENT '累计金额',
    `approval_status` VARCHAR(50)  NOT NULL COMMENT '审核状态',
    `organization_id` VARCHAR(32)  NOT NULL COMMENT '组织ID',
    `create_time`     BIGINT       NOT NULL COMMENT '创建时间',
    `update_time`     BIGINT       NOT NULL COMMENT '更新时间',
    `create_user`     VARCHAR(32)  NOT NULL COMMENT '创建人',
    `update_user`     VARCHAR(32)  NOT NULL COMMENT '更新人',
    PRIMARY KEY (id)
) COMMENT = '商机报价单'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_opportunity_id ON opportunity_quotation (opportunity_id ASC);
CREATE INDEX idx_organization_id ON opportunity_quotation (organization_id ASC);
CREATE INDEX idx_approval_status ON opportunity_quotation (approval_status);

CREATE TABLE opportunity_quotation_field
(
    `id`          VARCHAR(32)  NOT NULL COMMENT 'id',
    `resource_id` VARCHAR(32)  NOT NULL COMMENT '报价单id',
    `field_id`    VARCHAR(32)  NOT NULL COMMENT '自定义属性id',
    `field_value` VARCHAR(255) NOT NULL COMMENT '自定义属性值',
    `ref_sub_id`  VARCHAR(32) COMMENT '引用子表格ID;关联的子表格字段ID',
    `row_id`      VARCHAR(32) COMMENT '子表格行实例ID;行实例数据ID',
    PRIMARY KEY (id)
) COMMENT = '商机报价单自定义属性'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_resource_id ON opportunity_quotation_field (resource_id ASC);

CREATE TABLE opportunity_quotation_field_blob
(
    `id`          VARCHAR(32) NOT NULL COMMENT 'id',
    `resource_id` VARCHAR(32) NOT NULL COMMENT '报价单id',
    `field_id`    VARCHAR(32) NOT NULL COMMENT '自定义属性id',
    `field_value` TEXT        NOT NULL COMMENT '自定义属性值',
    `ref_sub_id`  VARCHAR(32) COMMENT '父引用ID;关联的子表格字段ID',
    `row_id`      VARCHAR(32) COMMENT '行实例ID;行实例数据ID',
    PRIMARY KEY (id)
) COMMENT = '商机报价单自定义属性大文本'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_resource_id ON opportunity_quotation_field_blob (resource_id ASC);

CREATE TABLE opportunity_quotation_approval
(
    `id`              VARCHAR(32) NOT NULL COMMENT 'id',
    `quotation_id`    VARCHAR(32) NOT NULL COMMENT '商机报价单id',
    `approval_status` VARCHAR(50) NOT NULL COMMENT '审核状态',
    `create_time`     BIGINT      NOT NULL COMMENT '创建时间',
    `update_time`     BIGINT      NOT NULL COMMENT '更新时间',
    `create_user`     VARCHAR(32) NOT NULL COMMENT '创建人',
    `update_user`     VARCHAR(32) NOT NULL COMMENT '更新人',
    PRIMARY KEY (id)
) COMMENT = '商机报价单审批'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_quotation_id ON opportunity_quotation_approval (quotation_id ASC);

CREATE TABLE opportunity_quotation_snapshot
(
    `id`              VARCHAR(32) NOT NULL COMMENT 'id',
    `quotation_id`    VARCHAR(32) NOT NULL COMMENT '报价单id',
    `quotation_prop`  TEXT COMMENT '表单属性快照',
    `quotation_value` TEXT COMMENT '表单值快照',
    PRIMARY KEY (id)
) COMMENT = '商机报价单快照'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_quotation_id ON opportunity_quotation_snapshot (quotation_id ASC);


CREATE TABLE contract
(
    `id`              VARCHAR(32)  NOT NULL COMMENT 'id',
    `name`            VARCHAR(255) NOT NULL COMMENT '合同名称',
    `customer_id`     VARCHAR(32)  NOT NULL COMMENT '客户id',
    `owner`           VARCHAR(32)  NOT NULL COMMENT '合同负责人',
    `amount`          DECIMAL(20, 10) COMMENT '金额',
    `number`          VARCHAR(50)  NOT NULL COMMENT '编号',
    `approval_status`   VARCHAR(50) COMMENT '审核状态',
    `archived_status` VARCHAR(50)  NOT NULL COMMENT '归档状态',
    `status`          VARCHAR(50)  NOT NULL COMMENT '合同状态',
    `organization_id` VARCHAR(32)  NOT NULL COMMENT '组织id',
    `void_reason`     VARCHAR(255) COMMENT '作废原因',
    `create_time`     BIGINT       NOT NULL COMMENT '创建时间',
    `update_time`     BIGINT       NOT NULL COMMENT '更新时间',
    `create_user`     VARCHAR(32)  NOT NULL COMMENT '创建人',
    `update_user`     VARCHAR(32)  NOT NULL COMMENT '更新人',
    PRIMARY KEY (id)
) COMMENT = '合同'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_name ON contract (name ASC);
CREATE INDEX idx_customer_id ON contract (customer_id ASC);
CREATE INDEX idx_owner ON contract (owner ASC);
CREATE INDEX idx_number ON contract (number ASC);
CREATE INDEX idx_organization_id ON contract (organization_id ASC);
CREATE INDEX idx_approval_status ON contract (approval_status);
CREATE INDEX idx_archived_status ON contract (archived_status);
CREATE INDEX idx_status ON contract (status);


CREATE TABLE contract_field
(
    `id`          VARCHAR(32)  NOT NULL COMMENT 'id',
    `resource_id` VARCHAR(32)  NOT NULL COMMENT '合同id',
    `field_id`    VARCHAR(32)  NOT NULL COMMENT '自定义属性id',
    `field_value` VARCHAR(255) NOT NULL COMMENT '自定义属性值',
    `ref_sub_id`  VARCHAR(32) COMMENT '引用子表格ID;关联的子表格字段ID',
    `row_id`      VARCHAR(32) COMMENT '子表格行实例ID;行实例数据ID',
    PRIMARY KEY (id)
) COMMENT = '合同自定义属性'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_resource_id ON contract_field (resource_id ASC);
CREATE INDEX idx_ref_sub_id ON contract_field (ref_sub_id ASC);


CREATE TABLE contract_field_blob
(
    `id`          VARCHAR(32) NOT NULL COMMENT 'id',
    `resource_id` VARCHAR(32) NOT NULL COMMENT '合同id',
    `field_id`    VARCHAR(32) NOT NULL COMMENT '自定义属性id',
    `field_value` TEXT        NOT NULL COMMENT '自定义属性值',
    `ref_sub_id`  VARCHAR(32) COMMENT '引用子表格ID;关联的子表格字段ID',
    `row_id`      VARCHAR(32) COMMENT '子表格行实例ID;行实例数据ID',
    PRIMARY KEY (id)
) COMMENT = '合同自定义属性大文本'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_resource_id ON contract_field_blob (resource_id ASC);
CREATE INDEX idx_ref_sub_id ON contract_field_blob (ref_sub_id ASC);


CREATE TABLE contract_snapshot
(
    `id`             VARCHAR(32) NOT NULL COMMENT 'id',
    `contract_id`    VARCHAR(32) NOT NULL COMMENT '合同id',
    `contract_prop`  TEXT(255) COMMENT '表单属性快照',
    `contract_value` TEXT(255) COMMENT '表单值快照',
    PRIMARY KEY (id)
) COMMENT = '合同快照'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;


CREATE INDEX idx_contract_id ON contract_snapshot (contract_id ASC);

-- 回款计划
CREATE TABLE contract_payment_plan
(
    `id`              VARCHAR(32) NOT NULL COMMENT 'id',
    `contract_id`     VARCHAR(32) NOT NULL COMMENT '合同ID',
    `owner`           VARCHAR(32) NOT NULL COMMENT '负责人',
    `plan_status`     VARCHAR(32) NOT NULL COMMENT '计划状态',
    `plan_amount`     DECIMAL(20, 10) COMMENT '计划回款金额',
    `plan_end_time`   BIGINT COMMENT '计划回款时间',
    `organization_id` VARCHAR(32) NOT NULL COMMENT '组织id',
    `create_time`     BIGINT      NOT NULL COMMENT '创建时间',
    `update_time`     BIGINT      NOT NULL COMMENT '更新时间',
    `create_user`     VARCHAR(32) NOT NULL COMMENT '创建人',
    `update_user`     VARCHAR(32) NOT NULL COMMENT '更新人',
    PRIMARY KEY (id)
) COMMENT = '合同回款计划'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_contract_id ON contract_payment_plan (contract_id ASC);
CREATE INDEX idx_create_time ON contract_payment_plan (create_time ASC);
CREATE INDEX idx_owner ON contract_payment_plan (owner ASC);

CREATE TABLE contract_payment_plan_field
(
    `id`          VARCHAR(32)  NOT NULL COMMENT 'id',
    `resource_id` VARCHAR(32)  NOT NULL COMMENT '回款计划id',
    `field_id`    VARCHAR(32)  NOT NULL COMMENT '自定义属性id',
    `field_value` VARCHAR(255) NOT NULL COMMENT '自定义属性值',
    PRIMARY KEY (id)
) COMMENT = '回款计划自定义属性'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_resource_id ON contract_payment_plan_field (resource_id ASC);

CREATE TABLE contract_payment_plan_field_blob
(
    `id`          VARCHAR(32) NOT NULL COMMENT 'id',
    `resource_id` VARCHAR(32) NOT NULL COMMENT '回款计划id',
    `field_id`    VARCHAR(32) NOT NULL COMMENT '自定义属性id',
    `field_value` TEXT        NOT NULL COMMENT '自定义属性值',
    PRIMARY KEY (id)
) COMMENT = '回款计划自定义属性大文本'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_resource_id ON contract_payment_plan_field_blob (resource_id ASC);

-- 产品价格表
CREATE TABLE product_price
(
    `id`              VARCHAR(32)  NOT NULL COMMENT 'ID',
    `name`            VARCHAR(255) NOT NULL COMMENT '价格表名称',
    `status`          VARCHAR(32)  NOT NULL COMMENT '状态',
    `pos`             BIGINT       NOT NULL COMMENT '自定义排序',
    `organization_id` VARCHAR(32)  NOT NULL COMMENT '组织ID',
    `create_time`     BIGINT       NOT NULL COMMENT '创建时间',
    `update_time`     BIGINT       NOT NULL COMMENT '更新时间',
    `create_user`     VARCHAR(32)  NOT NULL COMMENT '创建人',
    `update_user`     VARCHAR(32)  NOT NULL COMMENT '更新人',
    PRIMARY KEY (id)
) COMMENT = '价格'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_org_id ON product_price (organization_id ASC);
CREATE INDEX idx_status ON product_price (status);

CREATE TABLE product_price_field
(
    `id`          VARCHAR(32)  NOT NULL COMMENT 'ID',
    `resource_id` VARCHAR(32)  NOT NULL COMMENT '价格表ID',
    `field_id`    VARCHAR(32)  NOT NULL COMMENT '自定义属性ID',
    `field_value` VARCHAR(255) NOT NULL COMMENT '自定义属性值',
    `ref_sub_id`  VARCHAR(32) COMMENT '引用子表格ID;关联的子表格字段ID',
    `row_id`      VARCHAR(32) COMMENT '子表格行实例ID;行实例数据ID',
    PRIMARY KEY (id)
) COMMENT = '价格自定义属性'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_resource_id ON product_price_field (resource_id ASC);
CREATE INDEX idx_ref_sub_id ON product_price_field (ref_sub_id ASC);

CREATE TABLE product_price_field_blob
(
    `id`          VARCHAR(32) NOT NULL COMMENT 'id',
    `resource_id` VARCHAR(32) NOT NULL COMMENT '价格id',
    `field_id`    VARCHAR(32) NOT NULL COMMENT '自定义属性id',
    `field_value` TEXT        NOT NULL COMMENT '自定义属性值',
    `ref_sub_id`  VARCHAR(32) COMMENT '引用子表格ID;关联的子表格字段ID',
    `row_id`      VARCHAR(32) COMMENT '子表格行实例ID;行实例数据ID',
    PRIMARY KEY (id)
) COMMENT = '价格表自定义属性大文本'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_resource_id ON product_price_field_blob (resource_id ASC);
CREATE INDEX idx_ref_sub_id ON product_price_field_blob (ref_sub_id ASC);

-- modify record, plan content field length (3000)
ALTER TABLE follow_up_record MODIFY COLUMN content VARCHAR(3000) NOT NULL COMMENT '跟进内容';
ALTER TABLE follow_up_plan MODIFY COLUMN content VARCHAR(3000) NOT NULL COMMENT '跟进内容';

-- set innodb lock wait timeout to default
SET SESSION innodb_lock_wait_timeout = DEFAULT;