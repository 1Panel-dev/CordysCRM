SET SESSION innodb_lock_wait_timeout = 7200;

ALTER TABLE approval_flow
    CHANGE COLUMN require_comment require_approve_comment TINYINT(1) NOT NULL DEFAULT 0 COMMENT '同意时是否必须填写审批意见',
    ADD COLUMN require_reject_comment TINYINT(1) NOT NULL DEFAULT 0 COMMENT '驳回时是否必须填写审批意见' AFTER require_approve_comment;

SET SESSION innodb_lock_wait_timeout = DEFAULT;
