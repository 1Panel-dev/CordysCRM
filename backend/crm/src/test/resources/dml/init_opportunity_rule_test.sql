INSERT INTO opportunity_rule (id, name, organization_id, owner_id, scope_id, enable, auto, operator,
                              `condition`, create_time, create_user, update_time, update_user) VALUES
('opportunity_rule_id', 'test_rule', '100001', 'admin', 'admin', 1, 1, 'and', '{\"column\":\"storageTime\",\"operator\":\"DYNAMICS\",\"value\":\"6,month\",\"scope\":[\"Created\"]}',
 CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 'admin');
INSERT INTO sys_organization_user (id, organization_id, department_id, user_id, enable, create_time, create_user, update_time, update_user) VALUES
('sys_org_user_id', '100001', 'default_dpt_id', 'admin', 1, CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 'admin');
INSERT INTO sys_department (id, name, organization_id, parent_id, pos, create_time, create_user, update_time, update_user, resource) VALUES
('default_dpt_id', 'default_dpt', '100001', 'root', 1, CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 'admin', '');