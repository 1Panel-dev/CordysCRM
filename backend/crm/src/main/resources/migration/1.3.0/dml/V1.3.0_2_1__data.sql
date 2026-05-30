-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

ALTER TABLE agent
    ADD COLUMN type varchar(50) NOT NULL COMMENT '类型';

ALTER TABLE agent
    ADD COLUMN workspace_id varchar(50) COMMENT '工作空间id';

ALTER TABLE agent
    ADD COLUMN application_id varchar(50) COMMENT '智能体应用id';

SET SESSION innodb_lock_wait_timeout = DEFAULT;