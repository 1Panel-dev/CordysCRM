-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;


UPDATE agent set type = 'SCRIPT';

UPDATE sys_module set enable = b'1' where module_key = 'dashboard';


SET SESSION innodb_lock_wait_timeout = DEFAULT;