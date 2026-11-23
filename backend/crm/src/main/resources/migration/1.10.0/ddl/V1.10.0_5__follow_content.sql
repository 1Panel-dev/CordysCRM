SET SESSION innodb_lock_wait_timeout = 7200;

ALTER TABLE follow_up_plan
    MODIFY content VARCHAR(3000) NULL COMMENT '跟进内容';

ALTER TABLE follow_up_record
    MODIFY content VARCHAR(3000) NULL COMMENT '跟进内容';

SET SESSION innodb_lock_wait_timeout = DEFAULT;