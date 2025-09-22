-- set innodb lock wait timeout

SET SESSION innodb_lock_wait_timeout = 7200;

-- 初始导航栏
insert into sys_navigation value (UUID_SHORT(), '100001', 'search', true, 1,
                              'admin', UNIX_TIMESTAMP() * 1000, 'admin', UNIX_TIMESTAMP() * 1000);

insert into sys_navigation value (UUID_SHORT(), '100001', 'agent', true, 2,
                              'admin', UNIX_TIMESTAMP() * 1000, 'admin', UNIX_TIMESTAMP() * 1000);

insert into sys_navigation value (UUID_SHORT(), '100001', 'notify', true, 3,
                              'admin', UNIX_TIMESTAMP() * 1000, 'admin', UNIX_TIMESTAMP() * 1000);

insert into sys_navigation value (UUID_SHORT(), '100001', 'about', true, 4,
                              'admin', UNIX_TIMESTAMP() * 1000, 'admin', UNIX_TIMESTAMP() * 1000);

insert into sys_navigation value (UUID_SHORT(), '100001', 'language', true, 5,
                              'admin', UNIX_TIMESTAMP() * 1000, 'admin', UNIX_TIMESTAMP() * 1000);

insert into sys_navigation value (UUID_SHORT(), '100001', 'help', true, 6,
                              'admin', UNIX_TIMESTAMP() * 1000, 'admin', UNIX_TIMESTAMP() * 1000);


SET SESSION innodb_lock_wait_timeout = DEFAULT;