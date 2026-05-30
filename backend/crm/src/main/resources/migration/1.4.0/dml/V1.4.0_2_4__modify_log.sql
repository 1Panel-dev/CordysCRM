-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

UPDATE sys_operation_log SET module = 'PRODUCT_MANAGEMENT_PRO' WHERE module = 'PRODUCT_MANAGEMENT';

SET SESSION innodb_lock_wait_timeout = DEFAULT;