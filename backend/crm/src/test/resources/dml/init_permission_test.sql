-- 初始化用于项目级别权限测试的用户
INSERT INTO `sys_user` (`id`, `name`, `email`, `password`,  `gender`, `phone`, `language`, `create_time`,
                        `update_time`, `create_user`, `update_user`)
VALUES ('permission_test', 'permission_test', 'permission_test@cordys-crm.io', MD5('CordysCRM'), 1,'1253465576', 'zh_CN',
        UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000, 'admin', 'admin');

-- 初始化一个用于权限测试的用户组，这里默认使用 PROJECT 作为ID，如果是组织和项目级别类似，便于根据权限的前缀找到对应测试的用户组
INSERT INTO `sys_role`(id, name, internal, data_scope, create_time, update_time, create_user, update_user,
                       description, organization_id)
VALUES('permission_test', 'permission_test', 0, 'ALL', UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000, 'admin', 'admin',
       'permission_test', '1');

-- 初始化用户和角色的关系
INSERT INTO `sys_user_role` (id, role_id, user_id, create_time, update_time, create_user, update_user)
VALUES('permission_test', 'permission_test', 'permission_test', UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000, 'admin', 'admin');

-- 初始化组织和用户的关系
INSERT INTO sys_organization_user
(`id`, `organization_id`, `department_id`, `user_id`, `enable`, `employee_id`, `position`, `employee_type`, `supervisor_id`, `work_city`, `create_user`, `update_user`, `create_time`, `update_time`)
VALUES ('permission_test', '100001', '100001', 'permission_test', true, '', '', '', '', '', 'admin', 'admin', UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000);



