-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

-- approval_flow 增加 delete_execute 字段
ALTER TABLE approval_flow ADD COLUMN delete_execute tinyint(1) DEFAULT 0 COMMENT '删除时执行';

-- approval_node 增加 execute_time 字段
ALTER TABLE approval_node ADD COLUMN execute_time varchar(30) DEFAULT NULL COMMENT '执行时机：CREATE/UPDATE/DELETE';

-- approval_instance 增加 execute_time 字段
ALTER TABLE approval_instance ADD COLUMN execute_time varchar(30) DEFAULT NULL COMMENT '执行时机：CREATE/UPDATE/DELETE';

-- set innodb lock wait timeout to default
SET SESSION innodb_lock_wait_timeout = DEFAULT;
