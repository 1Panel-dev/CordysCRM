-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

CREATE TABLE sys_user
(
    `id`                   VARCHAR(32)  NOT NULL COMMENT 'id',
    `name`                 VARCHAR(255) NOT NULL COMMENT '名称',
    `phone`                VARCHAR(11) COMMENT '手机号',
    `email`                VARCHAR(255) COMMENT '邮箱',
    `password`             VARCHAR(255) NOT NULL COMMENT '密码',
    `language`             VARCHAR(32) COMMENT '语言',
    `last_organization_id` VARCHAR(50) COMMENT '当前组织ID',
    `gender`               BIT(1)       NOT NULL DEFAULT 0 COMMENT '性别(0-男/1-女)',
    `create_time`          BIGINT       NOT NULL COMMENT '创建时间',
    `update_time`          BIGINT       NOT NULL COMMENT '更新时间',
    `create_user`          VARCHAR(32)  NOT NULL COMMENT '创建人',
    `update_user`          VARCHAR(32)  NOT NULL COMMENT '更新人',
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

CREATE TABLE sys_operation_log(
  `id` VARCHAR(32) NOT NULL   COMMENT '主键' ,
  `organization_id` VARCHAR(32) NOT NULL  DEFAULT 'NONE' COMMENT '组织id' ,
  `type` VARCHAR(32) NOT NULL   COMMENT '操作类型/add/update/delete' ,
  `module` VARCHAR(32) NOT NULL   COMMENT '操作模块' ,
  `resource_id` VARCHAR(32) NOT NULL   COMMENT '资源id' ,
  `resource_name` VARCHAR(255)    COMMENT '资源名称' ,
  `detail` VARCHAR(500)    COMMENT '操作详情' ,
  `create_time` BIGINT NOT NULL   COMMENT '操作时间' ,
  `create_user` VARCHAR(32) NOT NULL   COMMENT '操作人' ,
  `path` VARCHAR(255)    COMMENT '操作路径' ,
  `method` VARCHAR(255) NOT NULL   COMMENT '操作方法' ,
  PRIMARY KEY (id)
)  COMMENT = '操作日志'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;

CREATE TABLE sys_login_log
(
    `id`            VARCHAR(32) NOT NULL COMMENT '主键',
    `create_time`   BIGINT      NOT NULL COMMENT '操作时间',
    `operator`      VARCHAR(32) NOT NULL COMMENT '操作人',
    `login_address` VARCHAR(255) COMMENT '登录地',
    `platform`      VARCHAR(32) COMMENT '平台',
    PRIMARY KEY (id)
) COMMENT = '登入日志'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_operator ON sys_login_log (operator ASC);
CREATE INDEX idx_create_time ON sys_login_log (create_time ASC);

CREATE INDEX idx_organization_id ON sys_operation_log (organization_id ASC);
CREATE INDEX idx_resource_id ON sys_operation_log (resource_id ASC);
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


CREATE TABLE IF NOT EXISTS sys_notification
(
    `id`              VARCHAR(32)  NOT NULL COMMENT 'id',
    `type`            VARCHAR(64)  NOT NULL COMMENT '通知类型',
    `receiver`        VARCHAR(32)  NOT NULL COMMENT '接收人',
    `subject`         VARCHAR(255) NOT NULL COMMENT '标题',
    `status`          VARCHAR(64)  NOT NULL COMMENT '状态',
    `operator`        VARCHAR(32)  NOT NULL COMMENT '操作人',
    `operation`       VARCHAR(50)  NOT NULL COMMENT '操作',
    `organization_id` VARCHAR(32)  NOT NULL COMMENT '组织id',
    `resource_id`     VARCHAR(32)  NOT NULL COMMENT '资源ID',
    `resource_type`   VARCHAR(64)  NOT NULL COMMENT '资源类型',
    `resource_name`   VARCHAR(255) NOT NULL COMMENT '资源名称',
    `content`         BLOB         NOT NULL COMMENT '通知内容',
    `create_time`     BIGINT       NOT NULL COMMENT '创建时间',
    `update_time`     BIGINT       NOT NULL COMMENT '更新时间',
    `create_user`     VARCHAR(32)  NOT NULL COMMENT '创建人',
    `update_user`     VARCHAR(32)  NOT NULL COMMENT '更新人',
    PRIMARY KEY (id)
) COMMENT = '消息通知'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;



CREATE INDEX idx_receiver ON sys_notification (receiver ASC);
CREATE INDEX idx_create_time ON sys_notification (create_time DESC);
CREATE INDEX idx_type ON sys_notification (type ASC);
CREATE INDEX idx_subject ON sys_notification (subject ASC);
CREATE INDEX idx_resource_type ON sys_notification (resource_type ASC);
CREATE INDEX idx_operator ON sys_notification (operator ASC);
CREATE INDEX idx_organization_id ON sys_notification (organization_id ASC);
CREATE INDEX idx_resource_id ON sys_notification (resource_id ASC);


CREATE TABLE sys_message_task
(
    `id`              VARCHAR(50)  NOT NULL COMMENT '',
    `event`           VARCHAR(255) NOT NULL COMMENT '通知事件类型',
    `task_type`       VARCHAR(64)  NOT NULL COMMENT '任务类型',
    `email_enable`    BIT          NOT NULL DEFAULT 0 COMMENT '邮件启用',
    `sys_enable`      BIT          NOT NULL DEFAULT 0 COMMENT '系统启用',
    `organization_id` VARCHAR(32)  NOT NULL COMMENT '组织id',
    `template`        BLOB COMMENT '消息模版',
    `create_user`     VARCHAR(50)  NOT NULL COMMENT '创建人',
    `create_time`     BIGINT       NOT NULL DEFAULT 0 COMMENT '创建时间',
    `update_user`     VARCHAR(50)  NOT NULL COMMENT '修改人',
    `update_time`     BIGINT       NOT NULL COMMENT '更新时间',
    PRIMARY KEY (id)
) COMMENT = '消息通知任务'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;


CREATE INDEX idx_create_time ON sys_message_task (create_time DESC);
CREATE INDEX idx_task_type ON sys_message_task (task_type ASC);
CREATE INDEX idx_event ON sys_message_task (event ASC);
CREATE INDEX idx_organization_id ON sys_message_task (organization_id ASC);
CREATE INDEX idx_email_enable ON sys_message_task (email_enable ASC);
CREATE INDEX idx_sys_enable ON sys_message_task (sys_enable ASC);

CREATE TABLE sys_announcement
(
    `id`              VARCHAR(32)  NOT NULL COMMENT 'id',
    `subject`         VARCHAR(255) NOT NULL COMMENT '公告标题',
    `content`         BLOB         NOT NULL COMMENT '公告内容',
    `start_time`      BIGINT       NOT NULL COMMENT '开始时间',
    `end_time`        BIGINT       NOT NULL COMMENT '结束时间',
    `url`             VARCHAR(255) COMMENT '链接',
    `rename_url`      VARCHAR(255) COMMENT '重命名链接',
    `receiver`        BLOB         NOT NULL COMMENT '接收人id(销售ids/角色ids/部门ids)',
    `organization_id` VARCHAR(32)  NOT NULL COMMENT '组织id',
    `notice`          BIT          NOT NULL DEFAULT 0 COMMENT '转为通知',
    `create_time`     BIGINT       NOT NULL COMMENT '创建时间',
    `update_time`     BIGINT       NOT NULL COMMENT '更新时间',
    `create_user`     VARCHAR(32)  NOT NULL COMMENT '创建人',
    `update_user`     VARCHAR(32)  NOT NULL COMMENT '更新人',
    `receive_type`    BLOB         NOT NULL COMMENT '接收类型组合',
    PRIMARY KEY (id)
) COMMENT = '公告'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_organizationId ON sys_announcement (organization_id ASC);
CREATE INDEX idx_create_time ON sys_announcement (create_time DESC);

CREATE TABLE sys_organization
(
    `id`          VARCHAR(32)  NOT NULL COMMENT 'id',
    `name`        VARCHAR(255) NOT NULL COMMENT '名称',
    `create_time` BIGINT       NOT NULL COMMENT '创建时间',
    `update_time` BIGINT       NOT NULL COMMENT '更新时间',
    `create_user` VARCHAR(32)  NOT NULL COMMENT '创建人',
    `update_user` VARCHAR(32)  NOT NULL COMMENT '更新人',
    PRIMARY KEY (id)
) COMMENT = '组织架构'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

CREATE TABLE sys_organization_user
(
    `id`               VARCHAR(32) NOT NULL COMMENT 'id',
    `organization_id`  VARCHAR(32) NOT NULL COMMENT '组织id',
    `department_id`    VARCHAR(32) COMMENT '部门id',
    `resource_user_id` VARCHAR(64) COMMENT '三方唯一id',
    `user_id`          VARCHAR(32) NOT NULL COMMENT '用户id',
    `enable`           BIT(1)      NOT NULL DEFAULT 1 COMMENT '是否启用',
    `employee_id`      VARCHAR(255)         DEFAULT '' COMMENT '工号',
    `position`         VARCHAR(255)         DEFAULT '' COMMENT '职位',
    `employee_type`    VARCHAR(255)         DEFAULT '' COMMENT '员工类型',
    `supervisor_id`    VARCHAR(32)          DEFAULT '' COMMENT '直属上级',
    `work_city`        VARCHAR(255)         DEFAULT '' COMMENT '工作城市',
    `create_time`      BIGINT      NOT NULL COMMENT '创建时间',
    `update_time`      BIGINT      NOT NULL COMMENT '更新时间',
    `create_user`      VARCHAR(32) NOT NULL COMMENT '创建人',
    `update_user`      VARCHAR(32) NOT NULL COMMENT '更新人',
    PRIMARY KEY (id)
) COMMENT = '组织成员'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_organization_id ON sys_organization_user (organization_id ASC);
CREATE INDEX idx_department_id ON sys_organization_user (department_id ASC);
CREATE INDEX idx_user_id ON sys_organization_user (user_id ASC);

CREATE TABLE sys_role
(
    `id`              VARCHAR(32)  NOT NULL COMMENT 'id',
    `name`            VARCHAR(255) NOT NULL COMMENT '角色名称',
    `internal`        BIT          NOT NULL DEFAULT 0 COMMENT '是否是内置角色',
    `data_scope`      VARCHAR(30)  NOT NULL COMMENT '数据范围（全部数据权限/指定部门权限/本部门数据权限/本部门及以下数据权限/仅本人数据）',
    `create_time`     BIGINT       NOT NULL COMMENT '创建时间',
    `update_time`     BIGINT       NOT NULL COMMENT '更新时间',
    `create_user`     VARCHAR(32)  NOT NULL COMMENT '创建人',
    `update_user`     VARCHAR(32)  NOT NULL COMMENT '更新人',
    `description`     VARCHAR(1000) COMMENT '描述',
    `organization_id` VARCHAR(32)  NOT NULL COMMENT '组织id',
    PRIMARY KEY (id)
) COMMENT = '角色'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

CREATE TABLE sys_role_permission(
    `id` VARCHAR(32) NOT NULL   COMMENT 'id' ,
    `role_id` VARCHAR(32) NOT NULL   COMMENT '角色id' ,
    `permission_id` VARCHAR(255) NOT NULL   COMMENT '权限id' ,
    PRIMARY KEY (id)
)  COMMENT = '角色权限'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_role_id ON sys_role_permission (role_id ASC);

CREATE TABLE sys_role_scope_dept
(
    `id`            VARCHAR(32) NOT NULL COMMENT 'ID',
    `role_id`       VARCHAR(32) NOT NULL COMMENT '角色ID',
    `department_id` VARCHAR(32) NOT NULL COMMENT '部门ID',
    PRIMARY KEY (id)
) COMMENT = '角色与部门的关联表，角色 data_scope 为指定部门时使用'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

CREATE TABLE sys_user_role
(
    `id`          VARCHAR(32) NOT NULL COMMENT 'id',
    `role_id`     VARCHAR(32) NOT NULL COMMENT '角色id',
    `user_id`     VARCHAR(32) NOT NULL COMMENT '用户id',
    `create_time` BIGINT      NOT NULL COMMENT '创建时间',
    `update_time` BIGINT      NOT NULL COMMENT '更新时间',
    `create_user` VARCHAR(32) NOT NULL COMMENT '创建人',
    `update_user` VARCHAR(32) NOT NULL COMMENT '更新人',
    PRIMARY KEY (id)
) COMMENT = '用户关联角色'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_role_id ON sys_user_role (role_id ASC);
CREATE INDEX idx_user_id ON sys_user_role (user_id ASC);

CREATE TABLE sys_organization_config_detail
(
    `id`          VARCHAR(32)  NOT NULL COMMENT 'id',
    `config_id`   VARCHAR(32)  NOT NULL COMMENT '配置id',
    `type`        VARCHAR(64)  NOT NULL COMMENT '配置内容类型',
    `enable`      BIT          NOT NULL DEFAULT 0 COMMENT '是否启用',
    `name`        VARCHAR(255) NOT NULL DEFAULT 'NONE' COMMENT '配置名称',
    `content`     BLOB         NOT NULL COMMENT '配置内容',
    `description` VARCHAR(1000) COMMENT '描述',
    `create_time` BIGINT       NOT NULL COMMENT '创建时间',
    `update_time` BIGINT       NOT NULL COMMENT '更新时间',
    `create_user` VARCHAR(32)  NOT NULL COMMENT '创建人',
    `update_user` VARCHAR(32)  NOT NULL COMMENT '更新人',
    PRIMARY KEY (id)
) COMMENT = '企业设置详情表'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_config_id ON sys_organization_config_detail (config_id ASC);
CREATE INDEX idx_type ON sys_organization_config_detail (type ASC);
CREATE INDEX idx_name ON sys_organization_config_detail (name ASC);

CREATE TABLE sys_organization_config
(
    `id`              VARCHAR(32) NOT NULL COMMENT 'id',
    `type`            VARCHAR(64) NOT NULL COMMENT '配置类型',
    `organization_id` VARCHAR(32) NOT NULL COMMENT '企业id',
    `sync`            BIT(1)      NOT NULL DEFAULT 0 COMMENT '是否同步（只对同步企业生效）',
    `sync_resource`   VARCHAR(255) COMMENT '同步来源',
    `enable`          BIT(1)      NOT NULL DEFAULT 0 COMMENT '是否开启',
    `create_time`     BIGINT      NOT NULL COMMENT '创建时间',
    `update_time`     BIGINT      NOT NULL COMMENT '更新时间',
    `create_user`     VARCHAR(32) NOT NULL COMMENT '创建人',
    `update_user`     VARCHAR(32) NOT NULL COMMENT '更新人',

    PRIMARY KEY (id)
) COMMENT = '企业设置'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_organizationId ON sys_organization_config (organization_id ASC);
CREATE INDEX idx_type ON sys_organization_config (type ASC);


CREATE TABLE sys_department
(
    `id`              VARCHAR(32)  NOT NULL COMMENT 'id',
    `name`            VARCHAR(255) NOT NULL COMMENT '名称',
    `organization_id` VARCHAR(32)  NOT NULL COMMENT '组织id',
    `parent_id`       VARCHAR(32)  NOT NULL COMMENT '父级',
    `pos`             BIGINT       NOT NULL COMMENT '排序',
    `create_time`     BIGINT       NOT NULL COMMENT '创建时间',
    `update_time`     BIGINT       NOT NULL COMMENT '更新时间',
    `create_user`     VARCHAR(32)  NOT NULL COMMENT '创建人',
    `update_user`     VARCHAR(32)  NOT NULL COMMENT '更新人',
    `resource`        VARCHAR(255) NOT NULL COMMENT '来源',
    `resource_id`     VARCHAR(255) COMMENT '来源id',
    PRIMARY KEY (id)
) COMMENT = '部门表'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_resource ON sys_department (resource ASC);
CREATE INDEX idx_organization_id ON sys_department (organization_id ASC);

CREATE TABLE sys_department_commander
(
    `id`            VARCHAR(32) NOT NULL COMMENT 'id',
    `user_id`       VARCHAR(32) NOT NULL COMMENT '用户id',
    `department_id` VARCHAR(32) NOT NULL COMMENT '部门id',
    `create_time`   BIGINT      NOT NULL COMMENT '创建时间',
    `update_time`   BIGINT      NOT NULL COMMENT '更新时间',
    `create_user`   VARCHAR(32) NOT NULL COMMENT '创建人',
    `update_user`   VARCHAR(32) NOT NULL COMMENT '更新人',
    PRIMARY KEY (id)
) COMMENT = '部门责任人表'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_user_id ON sys_department_commander (user_id ASC);
CREATE INDEX idx_department_id ON sys_department_commander (department_id ASC);

## module setting
CREATE TABLE sys_module
(
    `id`              VARCHAR(32) NOT NULL COMMENT 'id',
    `organization_id` VARCHAR(32) NOT NULL COMMENT '组织id',
    `module_key`      VARCHAR(20) NOT NULL COMMENT '模块KEY',
    `enable`          BIT(1)      NOT NULL DEFAULT 1 COMMENT '启用/禁用',
    `pos`             BIGINT      NOT NULL COMMENT '自定义排序',
    `create_user`     VARCHAR(32) NOT NULL COMMENT '创建人',
    `create_time`     BIGINT      NOT NULL COMMENT '创建时间',
    `update_user`     VARCHAR(32) NOT NULL COMMENT '修改人',
    `update_time`     BIGINT      NOT NULL COMMENT '修改时间',
    PRIMARY KEY (id)
) COMMENT = '模块设置'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_organization_id ON sys_module (organization_id ASC);

CREATE TABLE sys_module_form
(
    `id`              VARCHAR(32) NOT NULL COMMENT 'id',
    `form_key`        VARCHAR(20) NOT NULL COMMENT '表单Key',
    `organization_id` VARCHAR(32) NOT NULL COMMENT '组织id',
    `create_time`     BIGINT      NOT NULL COMMENT '创建时间',
    `update_time`     BIGINT      NOT NULL COMMENT '更新时间',
    `create_user`     VARCHAR(32) NOT NULL COMMENT '创建人',
    `update_user`     VARCHAR(32) NOT NULL COMMENT '更新人',
    PRIMARY KEY (id)
) COMMENT = '模块表单配置'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_organization_id ON sys_module_form (organization_id ASC);

CREATE TABLE sys_module_form_blob
(
    `id`   VARCHAR(32) NOT NULL COMMENT 'id',
    `prop` BLOB COMMENT '属性',
    PRIMARY KEY (id)
) COMMENT = '模块表单属性配置'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

CREATE TABLE sys_module_field
(
    `id`           VARCHAR(32) NOT NULL COMMENT 'ID',
    `form_id`      VARCHAR(32) NOT NULL COMMENT '所属表单ID',
    `internal_key` VARCHAR(255) COMMENT '字段内置Key',
    `name`         VARCHAR(255) NOT NULL   COMMENT '名称' ,
    `type`         VARCHAR(20) NOT NULL COMMENT '类型',
    `pos`          BIGINT      NOT NULL COMMENT '排序',
    `create_user`  VARCHAR(32) NOT NULL COMMENT '创建人',
    `create_time`  BIGINT      NOT NULL COMMENT '创建时间',
    `update_user`  VARCHAR(32) NOT NULL COMMENT '更新人',
    `update_time`  BIGINT      NOT NULL COMMENT '更新时间',
    PRIMARY KEY (id)
) COMMENT = '模块字段配置'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_form_id_internal_key ON sys_module_field (form_id, internal_key);

CREATE TABLE sys_module_field_blob
(
    `id`   VARCHAR(32) NOT NULL COMMENT 'id',
    `prop` TEXT    COMMENT '属性' ,
    PRIMARY KEY (id)
) COMMENT = '模块字段属性配置'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

CREATE TABLE sys_parameter
(
    `param_key`   VARCHAR(100) NOT NULL COMMENT 'key',
    `param_value` VARCHAR(255) COMMENT 'value',
    `type`        VARCHAR(100) NOT NULL DEFAULT 'text' COMMENT 'type',
    PRIMARY KEY (param_key)
) COMMENT = '系统参数'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

CREATE TABLE product(
                        `id` VARCHAR(32) NOT NULL   COMMENT 'id' ,
                        `name` VARCHAR(255) NOT NULL   COMMENT '名称' ,
                        `price` DECIMAL NOT NULL   COMMENT '价格' ,
                        `status` VARCHAR(32) NOT NULL   COMMENT '状态' ,
                        `organization_id` VARCHAR(32) NOT NULL   COMMENT '组织机构id' ,
                        `create_time` BIGINT NOT NULL   COMMENT '创建时间' ,
                        `update_time` BIGINT NOT NULL   COMMENT '更新时间' ,
                        `create_user` VARCHAR(32) NOT NULL   COMMENT '创建人' ,
                        `update_user` VARCHAR(32) NOT NULL   COMMENT '更新人' ,
                        PRIMARY KEY (id)
)  COMMENT = '产品'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_organization_id ON product(organization_id ASC);
CREATE INDEX idx_name ON product(name ASC);


CREATE TABLE product_field
(
    `id`          VARCHAR(32)  NOT NULL COMMENT 'id',
    `resource_id` VARCHAR(32)  NOT NULL COMMENT '产品id',
    `field_id`    VARCHAR(32)  NOT NULL COMMENT '自定义属性id',
    `field_value` VARCHAR(255) NOT NULL COMMENT '自定义属性值',
    PRIMARY KEY (id)
) COMMENT = '产品自定义属性'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_resource_id_field_id_field_value ON product_field (resource_id, field_id, field_value);

CREATE TABLE product_field_blob
(
    `id`          VARCHAR(32) NOT NULL COMMENT 'id',
    `resource_id` VARCHAR(32) NOT NULL COMMENT '产品id',
    `field_id`    VARCHAR(32) NOT NULL COMMENT '自定义属性id',
    `field_value` BLOB        NOT NULL COMMENT '自定义属性值',
    PRIMARY KEY (id)
) COMMENT = '客户自定义属性大文本'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_resource_id ON product_field_blob (resource_id);

CREATE TABLE sys_attachment(
    `id` VARCHAR(32) NOT NULL   COMMENT 'ID' ,
    `name` VARCHAR(255) NOT NULL   COMMENT '名称' ,
    `type` VARCHAR(50)    COMMENT '类型' ,
    `size` BIGINT(255)    COMMENT '大小' ,
    `storage` VARCHAR(50) NOT NULL   COMMENT '存储方式' ,
    `resource_id` VARCHAR(32)    COMMENT '资源ID' ,
    `organization_id` VARCHAR(32) NOT NULL   COMMENT '组织ID' ,
    `create_time` BIGINT NOT NULL   COMMENT '创建时间' ,
    `update_time` BIGINT NOT NULL   COMMENT '更新时间' ,
    `create_user` VARCHAR(32) NOT NULL   COMMENT '创建人' ,
    `update_user` VARCHAR(32) NOT NULL   COMMENT '更新人' ,
    PRIMARY KEY (id)
)  COMMENT = '系统附件'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;


CREATE INDEX idx_storage ON sys_attachment(storage ASC);
CREATE INDEX idx_org_id ON sys_attachment(organization_id ASC);
CREATE INDEX idx_resource_id ON sys_attachment(resource_id ASC);

-- set innodb lock wait timeout to default
SET SESSION innodb_lock_wait_timeout = DEFAULT;


