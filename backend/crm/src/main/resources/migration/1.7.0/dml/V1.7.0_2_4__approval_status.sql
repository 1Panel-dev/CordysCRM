-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

-- handle order his approval status
update sales_order set approval_status = 'NONE';

SET SESSION innodb_lock_wait_timeout = DEFAULT;