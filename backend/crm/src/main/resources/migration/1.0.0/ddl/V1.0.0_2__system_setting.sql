-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

CREATE TABLE IF NOT EXISTS operation_log
(
    `id`              VARCHAR(50)  NOT NULL COMMENT 'ID',
    `organization_id` VARCHAR(50)  NOT NULL DEFAULT 'NONE' COMMENT '组织id',
    `create_time`     BIGINT       NOT NULL COMMENT '操作时间',
    `create_user`     VARCHAR(50) COMMENT '操作人',
    `source_id`       VARCHAR(50) COMMENT '资源id',
    `method`          VARCHAR(255) NOT NULL COMMENT '操作方法',
    `type`            VARCHAR(20)  NOT NULL COMMENT '操作类型/add/update/delete',
    `module`          VARCHAR(50) COMMENT '操作模块/api/case/scenario/ui',
    `content`         VARCHAR(500) COMMENT '操作详情',
    `path`            VARCHAR(255) COMMENT '操作路径',
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '操作日志';

CREATE INDEX idx_create_time ON operation_log (create_time desc);
CREATE INDEX idx_create_user ON operation_log (create_user);
CREATE INDEX idx_method ON operation_log (method);
CREATE INDEX idx_module ON operation_log (module);
CREATE INDEX idx_type ON operation_log (type);
CREATE INDEX idx_organization_id ON operation_log (organization_id);
CREATE INDEX idx_source_id ON operation_log (source_id);


CREATE TABLE IF NOT EXISTS operation_log_blob
(
    `id`             VARCHAR(50) NOT NULL COMMENT '主键,与operation_log表id一致',
    `original_value` LONGBLOB COMMENT '变更前内容',
    `modified_value` LONGBLOB COMMENT '变更后内容',
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '操作日志内容详情';


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

CREATE TABLE IF NOT EXISTS `schedule`
(
    `id`            varchar(50) COLLATE utf8mb4_general_ci  NOT NULL,
    `key`           varchar(50) COLLATE utf8mb4_general_ci           DEFAULT NULL COMMENT 'qrtz UUID',
    `type`          varchar(50) COLLATE utf8mb4_general_ci  NOT NULL COMMENT '执行类型 cron',
    `value`         varchar(255) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'cron 表达式',
    `job`           varchar(64) COLLATE utf8mb4_general_ci  NOT NULL COMMENT 'Schedule Job Class Name',
    `resource_type` varchar(50) COLLATE utf8mb4_general_ci  NOT NULL DEFAULT 'NONE' COMMENT '资源类型 API_IMPORT,API_SCENARIO,UI_SCENARIO,LOAD_TEST,TEST_PLAN,CLEAN_REPORT,BUG_SYNC',
    `enable`        bit(1)                                           DEFAULT NULL COMMENT '是否开启',
    `resource_id`   varchar(50) COLLATE utf8mb4_general_ci           DEFAULT NULL COMMENT '资源ID，api_scenario ui_scenario load_test',
    `create_user`   varchar(50) COLLATE utf8mb4_general_ci  NOT NULL COMMENT '创建人',
    `create_time`   bigint                                  NOT NULL COMMENT '创建时间',
    `update_time`   bigint                                  NOT NULL COMMENT '更新时间',
    `organization_id`    varchar(50) COLLATE utf8mb4_general_ci           DEFAULT NULL COMMENT '组织ID',
    `name`          varchar(255) COLLATE utf8mb4_general_ci          DEFAULT NULL COMMENT '名称',
    `config`        varchar(1000) COLLATE utf8mb4_general_ci         DEFAULT NULL COMMENT '配置',
    `num`           bigint                                  NOT NULL COMMENT '业务ID',
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

CREATE TABLE IF NOT EXISTS user
(
    `id`                   VARCHAR(50)  NOT NULL COMMENT '用户ID',
    `name`                 VARCHAR(255) NOT NULL COMMENT '用户名',
    `email`                VARCHAR(64)  NOT NULL COMMENT '用户邮箱',
    `password`             VARCHAR(256) COLLATE utf8mb4_bin COMMENT '用户密码',
    `enable`               BIT          NOT NULL DEFAULT 1 COMMENT '是否启用',
    `create_time`          BIGINT       NOT NULL COMMENT '创建时间',
    `update_time`          BIGINT       NOT NULL COMMENT '更新时间',
    `language`             VARCHAR(30) COMMENT '语言',
    `last_organization_id` VARCHAR(50) COMMENT '当前组织ID',
    `phone`                VARCHAR(50) COMMENT '手机号',
    `source`               VARCHAR(50)  NOT NULL COMMENT '来源：LOCAL OIDC CAS OAUTH2',
    `create_user`          VARCHAR(50)  NOT NULL COMMENT '创建人',
    `update_user`          VARCHAR(50)  NOT NULL COMMENT '修改人',
    `deleted`              BIT          NOT NULL DEFAULT 0 COMMENT '是否删除',
    `cft_token`            VARCHAR(255) COMMENT 'CFT Token',
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '用户';



CREATE INDEX idx_name ON user (`name`);
CREATE UNIQUE INDEX idx_email ON user (`email`);
CREATE INDEX idx_create_time ON user (`create_time` desc);
CREATE INDEX idx_update_time ON user (`update_time` desc);
CREATE INDEX idx_organization_id ON user (`last_organization_id`);
CREATE INDEX idx_create_user ON user (`create_user`);
CREATE INDEX idx_update_user ON user (`update_user`);
CREATE INDEX idx_deleted ON user (`deleted`);


CREATE TABLE IF NOT EXISTS user_extend
(
    `id`            VARCHAR(50) NOT NULL COMMENT '用户ID',
    `platform_info` BLOB COMMENT '其他平台对接信息',
    `avatar`        VARCHAR(255) COMMENT '头像',
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '用户扩展';

CREATE TABLE IF NOT EXISTS `user_key`
(
    `id`          varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'user_key ID',
    `create_user` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户ID',
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

-- set innodb lock wait timeout to default
SET SESSION innodb_lock_wait_timeout = DEFAULT;


