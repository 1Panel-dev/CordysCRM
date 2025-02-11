-- 初始化用户
INSERT INTO `sys_user`(`id`, `name`, `phone`, `email`, `password`, `gender`, `language`, `create_time`, `update_time`, `create_user`, `update_user`)
VALUES
    ('1', '用户1', '1234567', '1@cordys-crm.io', '4706261da78b85ec9553be61a15f924c', 1, 'zh_CN', 1716175907000, 1729752373360, '717345437786112', 'admin'),
    ('2', '用户2', '2234567', '2@cordys-crm.io', '4706261da78b85ec9553be61a15f924c', 1, 'zh_CN', 1716175907000, 1729752373360, '717345437786112', 'admin'),
    ('3', '用户3', '3234567', '3@cordys-crm.io', '4706261da78b85ec9553be61a15f924c', 0, 'zh_CN', 1716175907000, 1729752373360, '717345437786112', 'admin'),
    ('4', '用户4', '4234567', '4@cordys-crm.io', '4706261da78b85ec9553be61a15f924c', 0, 'zh_CN', 1716175907000, 1729752373360, '717345437786112', 'admin'),
    ('5', '用户5', '5234567', '5@cordys-crm.io', '4706261da78b85ec9553be61a15f924c', 1, 'zh_CN', 1716175907000, 1729752373360, '717345437786112', 'admin');




INSERT INTO `sys_organization_user`(`id`, `organization_id`, `department_id`, `resource_user_id`, `user_id`, `enable`, `employee_id`, `position`, `employee_type`, `supervisor_id`, `work_city`, `create_user`, `update_user`, `create_time`, `update_time`)
VALUES
    ('u_1', '100001', '9', '', '1', true, '', '', '', '', '', 'admin', 'admin', 1716175907000, 1716175907000),
    ('u_2', '100001', '9', '', '2', true, '', '', '', '', '', 'admin', 'admin', 1716175907000, 1716175907000),
    ('u_3', '100001', '9', '', '3', true, '', '', '', '', '', 'admin', 'admin', 1716175907000, 1716175907000),
    ('u_4', '100001', '9', '', '4', true, '', '', '', '', '', 'admin', 'admin', 1716175907000, 1716175907000),
    ('u_5', '100001', '9', '', '5', true, '', '', '', '', '', 'admin', 'admin', 1716175907000, 1716175907000);


INSERT INTO `sys_role`(`id`, `name`, `internal`, `data_scope`, `create_time`, `update_time`, `create_user`, `update_user`, `description`, `organization_id`)
VALUES ('r_1', '测试角色', 0, '1', 1716175907000, 1716175907000, 'admin', 'admin', NULL, '100001');

INSERT INTO `sys_user_role`(`id`, `role_id`, `user_id`, `create_time`, `update_time`, `create_user`, `update_user`)
VALUES ('sur_1', 'r_1', '1', 1716175907000, 1716175907000, 'admin', 'admin');


INSERT INTO `sys_department`(`id`, `name`, `organization_id`, `parent_id`, `pos`, `create_time`, `update_time`, `create_user`, `update_user`, `resource`, `resource_id`)
VALUES
    ('9', '部门9', '100001', '4', 6, 1736240043609, 1736240043609, 'admin', 'admin', 'INTERNAL', NULL);