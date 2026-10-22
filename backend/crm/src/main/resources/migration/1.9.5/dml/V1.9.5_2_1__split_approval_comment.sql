SET SESSION innodb_lock_wait_timeout = 7200;

UPDATE approval_flow
SET require_reject_comment = require_approve_comment;

SET SESSION innodb_lock_wait_timeout = DEFAULT;
