-- set innodb lock wait timeout

SET SESSION innodb_lock_wait_timeout = 7200;

insert into sys_module value (UUID_SHORT(), '100001', 'contract', false, 9,
                              'admin', UNIX_TIMESTAMP() * 1000, 'admin', UNIX_TIMESTAMP() * 1000);


SET SESSION innodb_lock_wait_timeout = DEFAULT;