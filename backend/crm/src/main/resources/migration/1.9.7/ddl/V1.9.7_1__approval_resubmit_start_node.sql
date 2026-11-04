SET SESSION innodb_lock_wait_timeout = 7200;

ALTER TABLE approval_instance
    ADD COLUMN resubmit_start_node_number VARCHAR(50) NULL COMMENT '重审起始审批节点编号，驳回后为下一次提审起点';

SET SESSION innodb_lock_wait_timeout = DEFAULT;