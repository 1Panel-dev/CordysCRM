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

-- set innodb lock wait timeout to default
SET SESSION innodb_lock_wait_timeout = DEFAULT;
