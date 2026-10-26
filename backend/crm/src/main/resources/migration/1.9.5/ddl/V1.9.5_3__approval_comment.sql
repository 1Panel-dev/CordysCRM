-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

-- init approval comment
CREATE TABLE approval_comment
(
    `id`               VARCHAR(32) NOT NULL COMMENT 'ID',
    `resource_id`      VARCHAR(32) NOT NULL COMMENT '业务资源ID',
    `parent_id`        VARCHAR(32) DEFAULT NULL COMMENT '一级评论ID',
    `reply_to_user_id` VARCHAR(32) DEFAULT NULL COMMENT '被回复人ID',
    `content`          TEXT        NOT NULL COMMENT '评论内容',
    `organization_id`  VARCHAR(32) NOT NULL COMMENT '组织ID',
    `create_time`      BIGINT      NOT NULL COMMENT '创建时间',
    `update_time`      BIGINT      NOT NULL COMMENT '更新时间',
    `create_user`      VARCHAR(32) NOT NULL COMMENT '创建人',
    `update_user`      VARCHAR(32) NOT NULL COMMENT '更新人',
    PRIMARY KEY (`id`)
) COMMENT = '审批评论表'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_org_id ON approval_comment (organization_id ASC);
CREATE INDEX idx_resource_id ON approval_comment (resource_id ASC);
CREATE INDEX idx_parent_id ON approval_comment (parent_id ASC);

-- init approval comment mention
CREATE TABLE approval_comment_mention
(
    `id`         VARCHAR(32) NOT NULL COMMENT 'ID',
    `comment_id` VARCHAR(32) NOT NULL COMMENT '评论ID',
    `user_id`    VARCHAR(32) NOT NULL COMMENT '被@用户ID',
    `create_time` BIGINT     NOT NULL COMMENT '被@时间',
    PRIMARY KEY (`id`)
) COMMENT = '审批评论@用户关系'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

CREATE UNIQUE INDEX idx_comment_user ON approval_comment_mention (comment_id ASC, user_id ASC);
CREATE INDEX idx_user_time_id ON approval_comment_mention (user_id, create_time, id);

-- set innodb lock wait timeout to default
SET SESSION innodb_lock_wait_timeout = DEFAULT;
