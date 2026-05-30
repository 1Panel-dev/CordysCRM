-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

UPDATE sys_operation_log SET module = 'OPPORTUNITY_INDEX' WHERE module = 'OPPORTUNITY';


SET SESSION innodb_lock_wait_timeout = DEFAULT;