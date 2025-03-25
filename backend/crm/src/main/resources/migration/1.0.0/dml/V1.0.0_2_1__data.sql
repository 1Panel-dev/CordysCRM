-- set innodb lock wait timeout

SET SESSION innodb_lock_wait_timeout = 7200;

-- 初始化用户
INSERT INTO `sys_user` ( `id`, `name`, `email`, `password`, `gender`, `phone`, `language`, `last_organization_id`, `create_time`, `update_time`, `create_user`, `update_user` )
VALUES
    ( 'admin', 'Administrator', 'admin@cordys-crm.io', MD5( 'CordysCRM' ), 1, '4000520755', 'zh_CN', '100001', 1716175907000, 1729752373360, '717345437786112', 'admin' );

SET @internal_department_id = UUID_SHORT();-- 初始化用户基本信息

INSERT INTO `sys_organization_user` ( `id`, `organization_id`, `department_id`, `resource_user_id`, `user_id`, `enable`, `employee_id`, `position`, `employee_type`, `supervisor_id`, `work_city`, `create_user`, `update_user`, `create_time`, `update_time` )
VALUES
    ( UUID_SHORT(), '100001', @internal_department_id, '', 'admin', TRUE, '', '', '', '', '', 'admin', 'admin', 1716175907000, 1716175907000 );-- 初始化默认部门

INSERT INTO `sys_department` ( `id`, `name`, `organization_id`, `parent_id`, `pos`, `create_time`, `update_time`, `create_user`, `update_user`, `resource`, `resource_id` )
VALUES
    ( @internal_department_id, '公司名称', '100001', 'NONE', 100001, 1736240043609, 1736240043609, 'admin', 'admin', 'INTERNAL', NULL );-- 初始化默认组织

INSERT INTO `sys_organization` ( `id`, `name`, `create_time`, `update_time`, `create_user`, `update_user` )
VALUES
    ( '100001', 'default', 1736152274610, 1736152274610, 'admin', 'admin' );-- 初始化组织管理员

INSERT INTO sys_role ( id, NAME, internal, data_scope, create_time, update_time, create_user, update_user, description, organization_id )
VALUES
    ( 'org_admin', 'org_admin', 1, 'ALL', UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000, 'admin', 'admin', '', '100001' );-- 初始化销售经理

INSERT INTO sys_role ( id, NAME, internal, data_scope, create_time, update_time, create_user, update_user, description, organization_id )
VALUES
    ( 'sales_manager', 'sales_manager', 1, 'DEPT_AND_CHILD', UNIX_TIMESTAMP() * 1000 + 1, UNIX_TIMESTAMP() * 1000 + 1, 'admin', 'admin', '', '100001' );-- 初始化销售专员

INSERT INTO sys_role ( id, NAME, internal, data_scope, create_time, update_time, create_user, update_user, description, organization_id )
VALUES
    ( 'sales_staff', 'sales_staff', 1, 'SELF', UNIX_TIMESTAMP() * 1000 + 2, UNIX_TIMESTAMP() * 1000 + 2, 'admin', 'admin', '', '100001' );-- set innodb lock wait timeout to default

SET SESSION innodb_lock_wait_timeout = DEFAULT;