-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;


UPDATE agent set type = 'SCRIPT';


SET SESSION innodb_lock_wait_timeout = DEFAULT;