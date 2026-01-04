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
    `create_time`           BIGINT       NOT NULL COMMENT '创建时间',
    `update_time`           BIGINT       NOT NULL COMMENT '更新时间',
    `create_user`           VARCHAR(32)  NOT NULL COMMENT '创建人',
    `update_user`           VARCHAR(32)  NOT NULL COMMENT '更新人',
    PRIMARY KEY (id)
) COMMENT = '工商抬头'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;


CREATE INDEX idx_business_name ON business_title (business_name ASC);

-- set innodb lock wait timeout to default
SET SESSION innodb_lock_wait_timeout = DEFAULT;