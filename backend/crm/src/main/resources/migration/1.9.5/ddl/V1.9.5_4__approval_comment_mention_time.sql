SET SESSION innodb_lock_wait_timeout = 7200;

ALTER TABLE approval_comment_mention
    ADD COLUMN create_time BIGINT NULL COMMENT '被@时间';

-- 历史@关系使用评论创建时间，已失去评论的关系以0兜底。
UPDATE approval_comment_mention m
    LEFT JOIN approval_comment c ON c.id = m.comment_id
SET m.create_time = COALESCE(c.create_time, 0);

ALTER TABLE approval_comment_mention
    MODIFY COLUMN create_time BIGINT NOT NULL COMMENT '被@时间';

CREATE INDEX idx_user_time_id ON approval_comment_mention (user_id, create_time, id);

SET SESSION innodb_lock_wait_timeout = DEFAULT;
