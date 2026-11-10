SET SESSION innodb_lock_wait_timeout = 7200;

ALTER TABLE approval_instance
    ADD COLUMN resubmit_node_invalid TINYINT(1) NOT NULL DEFAULT 0 COMMENT '本轮提审是否因重审起始节点失效而回退整单重审';

SET SESSION innodb_lock_wait_timeout = DEFAULT;
