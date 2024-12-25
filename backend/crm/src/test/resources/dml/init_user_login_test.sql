-- 初始化一个系统管理员
insert into user(id, name, email, password, create_time, update_time, language, last_organization_id, phone, source, create_user, update_user, deleted)
VALUES ('test.login', 'test.login', 'test.login@163.com', MD5('test.login@163.com'), UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000, NULL, NUll, '', 'LOCAL', 'admin', 'admin', false);
