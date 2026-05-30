DELETE FROM approval_record WHERE id IN ('approval_record_current', 'approval_record_other');
DELETE FROM approval_task WHERE id IN ('approval_task_current', 'approval_task_other');
DELETE FROM approval_instance WHERE id = 'approval_instance_test_001';
DELETE FROM sys_user WHERE id IN ('appr_user_curr', 'appr_user_othr');

INSERT INTO sys_user (`id`, `name`, `email`, `password`, `gender`, `phone`, `language`, `last_organization_id`, `create_time`, `update_time`, `create_user`, `update_user`)
VALUES
    ('appr_user_curr', 'Current Approver', 'current.approver@cordys-crm.io', MD5('CordysCRM'), 1, '13900000001', 'zh_CN', '100001', 1736240043609, 1736240043609, 'admin', 'admin'),
    ('appr_user_othr', 'Other Approver', 'other.approver@cordys-crm.io', MD5('CordysCRM'), 1, '13900000002', 'zh_CN', '100001', 1736240043609, 1736240043609, 'admin', 'admin');

INSERT INTO approval_instance (`id`, `flow_version_id`, `type`, `resource_id`, `submitter_id`, `current_node_id`, `approval_status`, `submit_time`, `approval_time`,`create_time`, `update_time`, `create_user`, `update_user`)
VALUES
    ('approval_instance_test_001', 'approval_flow_test_001', 'contract', 'approval_resource_test_001', 'admin', 'node_current', 'APPROVING', 1736240043609, NULL, 1736240043609, 1736240043609, 'admin', 'admin');

INSERT INTO approval_task (`id`, `node_id`, `node_round`, `instance_id`, `approver_id`, `status`, `type`, `action`, `create_time`, `update_time`, `create_user`, `update_user`)
VALUES
    ('approval_task_current', 'node_current', 1, 'approval_instance_test_001', 'appr_user_curr', 'APPROVED', 'NL', 'APPROVE', 1736240043609, 1736240043609, 'admin', 'admin'),
    ('approval_task_other', 'node_other', 1, 'approval_instance_test_001', 'appr_user_othr', 'APPROVED', 'NL', 'APPROVE',1736241043609, 1736241043609, 'admin', 'admin');

INSERT INTO approval_record (`id`, `instance_id`, `task_id`, `node_id`, `node_round`, `result`, `comment`, `create_time`, `update_time`, `create_user`, `update_user`)
VALUES
    ('approval_record_current', 'approval_instance_test_001', 'approval_task_current', 'node_current', 1, 'APPROVED', 'current-node-comment', 1736240043609, 1736240043609, 'admin', 'admin'),
    ('approval_record_other', 'approval_instance_test_001', 'approval_task_other', 'node_other', 1, 'UNAPPROVED', 'other-node-comment', 1736241043609, 1736241043609, 'admin', 'admin');
