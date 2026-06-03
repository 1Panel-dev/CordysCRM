-- 加入代办顶部导航栏
update sys_navigation
set pos = pos + 1
where organization_id = '100001'
  and pos >= 2;
insert into sys_navigation value (UUID_SHORT(), '100001', 'task', true, 2,
                                  'admin', UNIX_TIMESTAMP() * 1000, 'admin', UNIX_TIMESTAMP() * 1000);
