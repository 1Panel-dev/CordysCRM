-- set innodb lock wait timeout

SET SESSION innodb_lock_wait_timeout = 7200;

-- 初始化仪表板菜单
insert into sys_module value (UUID_SHORT(), '100001', 'dashboard', false, 7,
                              'admin', UNIX_TIMESTAMP() * 1000, 'admin', UNIX_TIMESTAMP() * 1000);

SET SESSION innodb_lock_wait_timeout = DEFAULT;