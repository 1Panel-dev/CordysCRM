-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

CREATE TABLE opportunity_quotation
(
    `id`              VARCHAR(32)  NOT NULL COMMENT 'id',
    `name`            VARCHAR(255) NOT NULL COMMENT '名称',
    `opportunity_id`  VARCHAR(32)  NOT NULL COMMENT '商机id',
    `amount`          DECIMAL      NOT NULL COMMENT '累计金额',
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
    `id`              VARCHAR(32) NOT NULL COMMENT 'id',
    `resource_id`     VARCHAR(32) NOT NULL COMMENT '报价单id',
    `field_id`        VARCHAR(32) NOT NULL COMMENT '自定义属性id',
    `field_value`     TEXT        NOT NULL COMMENT '自定义属性值',
    `parent_ref_id`   VARCHAR(32) COMMENT '父引用ID;关联的子表格字段ID',
    `row_instance_id` VARCHAR(32) COMMENT '行实例ID;行实例数据ID',
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


-- set innodb lock wait timeout to default
SET SESSION innodb_lock_wait_timeout = DEFAULT;



