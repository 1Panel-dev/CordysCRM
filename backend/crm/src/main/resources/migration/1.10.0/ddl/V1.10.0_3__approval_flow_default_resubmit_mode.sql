SET SESSION innodb_lock_wait_timeout = 7200;

ALTER TABLE approval_flow
    ADD COLUMN default_resubmit_mode VARCHAR(30) NOT NULL DEFAULT 'SUBMITTER' COMMENT '默认重审路径：SUBMITTER/CURRENT_NODE/SPECIFIED_NODE';

SET SESSION innodb_lock_wait_timeout = DEFAULT;