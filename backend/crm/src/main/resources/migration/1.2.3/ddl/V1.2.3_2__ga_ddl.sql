-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

alter table sys_message_task
    add ding_talk_enable bit default b'0' null comment '钉钉启用' after we_com_enable;

alter table sys_message_task
    add lark_enable bit default b'0' null comment '飞书启用' after ding_talk_enable;

-- customer_id 字段允许为空
ALTER TABLE customer_contact MODIFY COLUMN customer_id varchar(32) NULL COMMENT '客户id';

CREATE TABLE opportunity_stage_config
(
    `id`              VARCHAR(32) NOT NULL COMMENT 'id',
    `name`            VARCHAR(16) NOT NULL COMMENT '值',
    `type`            VARCHAR(50) NOT NULL COMMENT '类型',
    `rate`            VARCHAR(10) NOT NULL COMMENT '赢率',
    `afoot_roll_back` BIT(1) DEFAULT 0 COMMENT '进行中回退设置',
    `end_roll_back`   BIT(1) DEFAULT 0 COMMENT '完结回退设置',
    `pos`             BIGINT      NOT NULL COMMENT '顺序',
    `organization_id` VARCHAR(32) NOT NULL COMMENT '组织id',
    `create_time`     BIGINT      NOT NULL COMMENT '创建时间',
    `update_time`     BIGINT      NOT NULL COMMENT '更新时间',
    `create_user`     VARCHAR(32) NOT NULL COMMENT '创建人',
    `update_user`     VARCHAR(32) NOT NULL COMMENT '更新人',
    PRIMARY KEY (id)
) COMMENT = '商机阶段配置'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;


-- set innodb lock wait timeout to default
SET SESSION innodb_lock_wait_timeout = DEFAULT;



