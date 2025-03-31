INSERT INTO sys_role (id, name, internal, data_scope, create_time, update_time, create_user, update_user, organization_id) VALUES
    ('recycle_role_id', 'role_test', 0, 'all', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'admin', 'admin', '100001');
INSERT INTO sys_user_role (id, user_id, role_id, create_time, update_time, create_user, update_user) VALUES
    ('recycle_user_role_id', 'admin', 'recycle_role_id', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'admin', 'admin');
INSERT INTO sys_department (id, name, organization_id, parent_id, pos, create_time, update_time, create_user, update_user, resource) VALUES
    ('recycle_department_id', 'recycle_department_test', '100001', 'NONE', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'admin', 'admin', '');
INSERT INTO sys_organization_user (id, user_id, organization_id, department_id, create_time, update_time, create_user, update_user) VALUES
    ('recycle_organization_user_id', 'admin', '100001', 'recycle_department_id', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'admin', 'admin');

INSERT INTO clue_pool (id, scope_id, organization_id, name, owner_id, enable, auto, create_time, update_time, create_user, update_user) VALUES
    ('job_pool_id', '[\"role_id\", \"department_id\", \"admin\"]', '100001', 'test_pool', 'admin', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'admin', 'admin');
INSERT INTO clue_pool_recycle_rule (id, pool_id, operator, `condition`, create_time, update_time, create_user, update_user) VALUES
    ('job_recycle_rule_id', 'job_pool_id', 'and', '{\"column\":\"storageTime\",\"operator\":\"DYNAMICS\",\"value\":\"6,month\",\"scope\":[\"Created\"]}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'admin', 'admin');