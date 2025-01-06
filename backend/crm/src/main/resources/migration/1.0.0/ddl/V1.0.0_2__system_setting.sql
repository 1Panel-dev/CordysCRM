-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

CREATE TABLE sys_user
(
    `id`            VARCHAR(32)  NOT NULL COMMENT 'id',
    `name`          VARCHAR(255) NOT NULL COMMENT '名称',
    `phone`         VARCHAR(11)  NOT NULL COMMENT '手机号',
    `email`         VARCHAR(255) NOT NULL COMMENT '邮箱',
    `password`      VARCHAR(255) NOT NULL COMMENT '密码',
    `enable`        BIT(1)       NOT NULL DEFAULT 1 COMMENT '是否启用',
    `employee_id`   VARCHAR(255)          DEFAULT '' COMMENT '工号',
    `gender`        VARCHAR(255)          DEFAULT '' COMMENT '性别',
    `position`      VARCHAR(255)          DEFAULT '' COMMENT '职位',
    `employee_type` VARCHAR(255)          DEFAULT '' COMMENT '员工类型',
    `supervisor_id` VARCHAR(32)           DEFAULT '' COMMENT '直属上级',
    `work_city`     VARCHAR(255)          DEFAULT '' COMMENT '工作城市',
    `create_time`   BIGINT       NOT NULL COMMENT '创建时间',
    `update_time`   BIGINT       NOT NULL COMMENT '更新时间',
    `create_user`   VARCHAR(32)  NOT NULL COMMENT '创建人',
    `update_user`   VARCHAR(32)  NOT NULL COMMENT '更新人',
    PRIMARY KEY (id)
) COMMENT = '用户(员工)'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_email ON sys_user (email DESC);
CREATE INDEX idx_create_time ON sys_user (create_time DESC);
CREATE INDEX idx_update_time ON sys_user (update_time ASC);
CREATE INDEX idx_create_user ON sys_user (create_user ASC);
CREATE INDEX idx_update_user ON sys_user (update_user ASC);

CREATE TABLE sys_user_extend
(
    `id`            VARCHAR(32) NOT NULL COMMENT '用户id',
    `avatar`        VARCHAR(255) COMMENT '头像内容',
    `platform_info` LONGBLOB COMMENT '对接平台信息',
    PRIMARY KEY (id)
) COMMENT = '用户扩展内容'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS worker_node
(
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT 'auto increment id',
    host_name   VARCHAR(64) NOT NULL COMMENT 'host name',
    port        VARCHAR(64) NOT NULL COMMENT 'port',
    type        INT         NOT NULL COMMENT 'node type: ACTUAL or CONTAINER',
    launch_date BIGINT      NOT NULL COMMENT 'launch date',
    modified    BIGINT      NOT NULL COMMENT 'modified time',
    created     BIGINT      NOT NULL COMMENT 'created time',
    PRIMARY KEY (ID)
)
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci COMMENT = 'DB WorkerID Assigner for UID Generator';

CREATE TABLE sys_operation_log
(
    `id`              VARCHAR(32)  NOT NULL COMMENT '主键',
    `organization_id` VARCHAR(32)  NOT NULL DEFAULT 'NONE' COMMENT '组织id',
    `type`            VARCHAR(32)  NOT NULL COMMENT '操作类型/add/update/delete',
    `module`          VARCHAR(32)  NOT NULL COMMENT '操作模块',
    `details`         VARCHAR(500) COMMENT '操作详情',
    `source_id`       VARCHAR(32)  NOT NULL COMMENT '资源id',
    `create_time`     BIGINT       NOT NULL COMMENT '操作时间',
    `create_user`     VARCHAR(32)  NOT NULL COMMENT '操作人',
    `path`            VARCHAR(255) COMMENT '操作路径',
    `method`          VARCHAR(255) NOT NULL COMMENT '操作方法',
    `login_address`   VARCHAR(255)  COMMENT '登录地',
    `platform`        VARCHAR(32)  COMMENT '平台',
    PRIMARY KEY (id)
) COMMENT = '操作日志'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_organization_id ON sys_operation_log (organization_id ASC);
CREATE INDEX idx_source_id ON sys_operation_log (source_id ASC);
CREATE INDEX idx_type ON sys_operation_log (type ASC);

CREATE TABLE sys_operation_log_blob
(
    `id`             VARCHAR(32) NOT NULL COMMENT '主键',
    `original_value` LONGBLOB COMMENT '变更前内容',
    `modified_value` LONGBLOB COMMENT '变更后内容',
    PRIMARY KEY (id)
) COMMENT = '操作日志内容详情'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `schedule`
(
    `id`              varchar(32) COLLATE utf8mb4_general_ci  NOT NULL,
    `key`             varchar(50) COLLATE utf8mb4_general_ci           DEFAULT NULL COMMENT 'qrtz UUID',
    `type`            varchar(50) COLLATE utf8mb4_general_ci  NOT NULL COMMENT '执行类型 cron',
    `value`           varchar(255) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'cron 表达式',
    `job`             varchar(64) COLLATE utf8mb4_general_ci  NOT NULL COMMENT 'Schedule Job Class Name',
    `resource_type`   varchar(50) COLLATE utf8mb4_general_ci  NOT NULL DEFAULT 'NONE' COMMENT '资源类型 API_IMPORT,API_SCENARIO,UI_SCENARIO,LOAD_TEST,TEST_PLAN,CLEAN_REPORT,BUG_SYNC',
    `enable`          bit(1)                                           DEFAULT NULL COMMENT '是否开启',
    `resource_id`     varchar(32) COLLATE utf8mb4_general_ci           DEFAULT NULL COMMENT '资源ID，api_scenario ui_scenario load_test',
    `create_user`     varchar(32) COLLATE utf8mb4_general_ci  NOT NULL COMMENT '创建人',
    `create_time`     bigint                                  NOT NULL COMMENT '创建时间',
    `update_time`     bigint                                  NOT NULL COMMENT '更新时间',
    `organization_id` varchar(32) COLLATE utf8mb4_general_ci           DEFAULT NULL COMMENT '组织ID',
    `name`            varchar(255) COLLATE utf8mb4_general_ci          DEFAULT NULL COMMENT '名称',
    `config`          varchar(1000) COLLATE utf8mb4_general_ci         DEFAULT NULL COMMENT '配置',
    `num`             bigint                                  NOT NULL COMMENT '业务ID',
    PRIMARY KEY (`id`),
    KEY `idx_resource_id` (`resource_id`),
    KEY `idx_create_user` (`create_user`),
    KEY `idx_create_time` (`create_time` DESC),
    KEY `idx_update_time` (`update_time` DESC),
    KEY `idx_organization_id` (`organization_id`),
    KEY `idx_enable` (`enable`),
    KEY `idx_name` (`name`),
    KEY `idx_type` (`type`),
    KEY `idx_resource_type` (`resource_type`),
    KEY `idx_num` (`num`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='定时任务';


CREATE TABLE IF NOT EXISTS `user_key`
(
    `id`          varchar(32) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'user_key ID',
    `create_user` varchar(32) COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户ID',
    `access_key`  varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'access_key',
    `secret_key`  varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'secret key',
    `create_time` bigint                                 NOT NULL COMMENT '创建时间',
    `enable`      bit(1)                                 NOT NULL DEFAULT b'1' COMMENT '状态',
    `forever`     bit(1)                                 NOT NULL DEFAULT b'1' COMMENT '是否永久有效',
    `expire_time` bigint                                          DEFAULT NULL COMMENT '到期时间',
    `description` varchar(1000) COLLATE utf8mb4_general_ci        DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `idx_ak` (`access_key`),
    KEY `idx_create_user` (`create_user`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='用户api key';


CREATE TABLE IF NOT EXISTS sys_notification(
                             `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
                             `type` VARCHAR(64) NOT NULL   COMMENT '通知类型' ,
                             `receiver` VARCHAR(50) NOT NULL   COMMENT '接收人' ,
                             `subject` VARCHAR(255) NOT NULL   COMMENT '标题' ,
                             `status` VARCHAR(64) NOT NULL   COMMENT '状态' ,
                             `operator` VARCHAR(50) NOT NULL   COMMENT '操作人' ,
                             `operation` VARCHAR(50) NOT NULL   COMMENT '操作' ,
                             `organization_id` VARCHAR(50) NOT NULL   COMMENT '组织id' ,
                             `resource_type` VARCHAR(64) NOT NULL   COMMENT '资源类型' ,
                             `resource_name` VARCHAR(255) NOT NULL   COMMENT '资源名称' ,
                             `content` BLOB NOT NULL   COMMENT '通知内容' ,
                             `create_time` BIGINT NOT NULL   COMMENT '创建时间' ,
                             `update_time` BIGINT NOT NULL   COMMENT '更新时间' ,
                             `create_user` VARCHAR(32) NOT NULL   COMMENT '创建人' ,
                             `update_user` VARCHAR(32) NOT NULL   COMMENT '更新人' ,
                             PRIMARY KEY (id)
)  COMMENT = '消息通知'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;



CREATE INDEX idx_receiver ON sys_notification(receiver ASC);
CREATE INDEX idx_create_time ON sys_notification(create_time DESC);
CREATE INDEX idx_type ON sys_notification(type ASC);
CREATE INDEX idx_subject ON sys_notification(subject ASC);
CREATE INDEX idx_resource_type ON sys_notification(resource_type ASC);
CREATE INDEX idx_operator ON sys_notification(operator ASC);
CREATE INDEX idx_organization_id ON sys_notification(organization_id ASC);

CREATE TABLE IF NOT EXISTS sys_message_task(
                             `id` VARCHAR(32) NOT NULL   COMMENT 'id' ,
                             `event` VARCHAR(255) NOT NULL   COMMENT '通知事件类型' ,
                             `receivers` VARCHAR(1000) NOT NULL   COMMENT '接收人id集合' ,
                             `project_robot_id` VARCHAR(50)   DEFAULT 'NONE' COMMENT '机器人id' ,
                             `task_type` VARCHAR(64) NOT NULL   COMMENT '任务类型' ,
                             `enable` BIT NOT NULL  DEFAULT 0 COMMENT '是否启用' ,
                             `create_user` VARCHAR(50) NOT NULL   COMMENT '创建人' ,
                             `create_time` BIGINT NOT NULL  DEFAULT 0 COMMENT '创建时间' ,
                             `update_user` VARCHAR(50) NOT NULL   COMMENT '修改人' ,
                             `update_time` BIGINT NOT NULL   COMMENT '更新时间' ,
                             `use_default_template` BIT NOT NULL  DEFAULT 1 COMMENT '是否使用默认模版' ,
                             `use_default_subject` BIT NOT NULL  DEFAULT 1 COMMENT '是否使用默认标题（仅邮件）' ,
                             `subject` VARCHAR(1000)    COMMENT '邮件标题' ,
                             PRIMARY KEY (id)
)  COMMENT = '消息通知任务'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_create_time ON sys_message_task(create_time DESC);
CREATE INDEX idx_task_type ON sys_message_task(task_type ASC);
CREATE INDEX idx_event ON sys_message_task(event ASC);
CREATE INDEX idx_enable ON sys_message_task(enable ASC);
CREATE INDEX idx_use_default_subject ON sys_message_task(use_default_subject ASC);
CREATE INDEX idx_use_default_template ON sys_message_task(use_default_template ASC);

CREATE TABLE IF NOT EXISTS `sys_message_task_blob`
(
    `id`       VARCHAR(32) NOT NULL COMMENT 'id',
    `template` BLOB COMMENT '消息模版',
    PRIMARY KEY (id)
) COMMENT = '消息通知任务大字段'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS sys_announcement(
                             `id` VARCHAR(32) NOT NULL   COMMENT 'id' ,
                             `create_time` BIGINT NOT NULL   COMMENT '创建时间' ,
                             `update_time` BIGINT NOT NULL   COMMENT '更新时间' ,
                             `create_user` VARCHAR(32) NOT NULL   COMMENT '创建人' ,
                             `update_user` VARCHAR(32) NOT NULL   COMMENT '更新人' ,
                             `subject` VARCHAR(255) NOT NULL   COMMENT '公告标题' ,
                             `content` VARCHAR(1000) NOT NULL   COMMENT '公告内容' ,
                             `start_time` BIGINT NOT NULL   COMMENT '开始时间' ,
                             `end_time` BIGINT NOT NULL   COMMENT '结束时间' ,
                             `url` VARCHAR(255)    COMMENT '链接' ,
                             `receiver` VARCHAR(1000) NOT NULL   COMMENT '接收人id(销售ids/角色ids/部门ids)' ,
                             `organization_id` VARCHAR(32) NOT NULL   COMMENT '组织id' ,
                             PRIMARY KEY (id)
)  COMMENT = '公告'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_organizationId ON sys_announcement(organization_id ASC);
CREATE INDEX idx_create_time ON sys_announcement(create_time DESC);

CREATE TABLE sys_module(
                           `id` VARCHAR(32) NOT NULL   COMMENT 'id' ,
                           `organization_id` VARCHAR(32) NOT NULL   COMMENT '组织id' ,
                           `name` VARCHAR(255) NOT NULL   COMMENT '模块名称(国际化KEY)' ,
                           `enable` BIT(1) NOT NULL  DEFAULT 1 COMMENT '启用/禁用' ,
                           `pos` BIGINT(255) NOT NULL   COMMENT '自定义排序' ,
                           `create_user` VARCHAR(32) NOT NULL   COMMENT '创建人' ,
                           `create_time` BIGINT(255) NOT NULL   COMMENT '创建时间' ,
                           `update_user` VARCHAR(32) NOT NULL   COMMENT '修改人' ,
                           `update_time` BIGINT(255) NOT NULL   COMMENT '修改时间' ,
                           PRIMARY KEY (id)
)  COMMENT = '模块设置'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_organization_id ON sys_module(organization_id ASC);


-- set innodb lock wait timeout to default
SET SESSION innodb_lock_wait_timeout = DEFAULT;


