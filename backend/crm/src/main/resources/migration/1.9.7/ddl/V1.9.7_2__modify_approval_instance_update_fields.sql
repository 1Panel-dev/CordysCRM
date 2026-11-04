SET SESSION innodb_lock_wait_timeout = 7200;

ALTER TABLE approval_instance MODIFY COLUMN update_fields text  NULL COMMENT '编辑时修改的字段列表';

SET SESSION innodb_lock_wait_timeout = DEFAULT;