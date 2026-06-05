-- 加入代办顶部导航栏
update sys_navigation
set pos = pos + 1
where organization_id = '100001'
  and pos >= 2;
insert into sys_navigation value (UUID_SHORT(), '100001', 'task', true, 2,
                                  'admin', UNIX_TIMESTAMP() * 1000, 'admin', UNIX_TIMESTAMP() * 1000);


INSERT INTO sys_module (id, organization_id, module_key, enable, pos, create_user, create_time, update_user, update_time)
VALUES (UUID_SHORT(), '100001', 'customForm', 1, 9, 'admin', UNIX_TIMESTAMP() * 1000, 'admin', UNIX_TIMESTAMP() * 1000);
