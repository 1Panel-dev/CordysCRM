INSERT INTO `sys_department`(`id`, `name`, `organization_id`, `parent_id`, `pos`, `create_time`, `update_time`, `create_user`, `update_user`, `resource`, `resource_id`)
VALUES
    ('1', '部门1', '100001', '100001', 1, 1736240043609, 1736240043609, 'admin', 'admin', 'INTERNAL', NULL),
    ('2', '部门2', '100001', '100001', 2, 1736240043609, 1736240043609, 'admin', 'admin', 'INTERNAL', NULL),
    ('3', '部门3', '100001', '1', 3, 1736240043609, 1736240043609, 'admin', 'admin', 'INTERNAL', NULL),
    ('4', '部门4', '100001', '1', 4, 1736240043609, 1736240043609, 'admin', 'admin', 'INTERNAL', NULL),
    ('5', '部门5', '100001', '3', 5, 1736240043609, 1736240043609, 'admin', 'admin', 'INTERNAL', NULL),
    ('6', '部门6', '100001', '5', 5, 1736240043609, 1736240043609, 'admin', 'admin', 'INTERNAL', NULL),
    ('7', '部门7', '100001', '4', 5, 1736240043609, 1736240043609, 'admin', 'admin', 'INTERNAL', NULL),
    ('8', '部门8', '100001', '4', 6, 1736240043609, 1736240043609, 'admin', 'admin', 'INTERNAL', NULL);




INSERT INTO `sys_organization_user`(`id`, `organization_id`, `department_id`, `resource_user_id`, `user_id`, `enable`, `employee_id`, `position`, `employee_type`, `supervisor_id`, `work_city`, `create_user`, `update_user`, `create_time`, `update_time`)
VALUES ('1', '100001', '8', '', 'admin', true, '', '', '', '', '', 'admin', 'admin', 1716175907000, 1716175907000);