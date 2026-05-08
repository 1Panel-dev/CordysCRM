DELETE FROM approval_task WHERE id IN ('todo_cc_task_001', 'todo_cc_task_002', 'todo_cc_task_003');
DELETE FROM approval_instance WHERE id IN ('todo_cc_inst_001', 'todo_cc_inst_002', 'todo_cc_inst_003');

INSERT INTO approval_instance (`id`, `flow_id`, `type`, `resource_id`, `submitter_id`, `current_node_id`, `approval_status`, `submit_time`, `approval_time`, `result`, `create_time`, `update_time`, `create_user`, `update_user`)
VALUES
    ('todo_cc_inst_001', 'approval_flow_test_001', 'contract', 'todo_cc_resource_001', 'admin', 'node_cc_001', 'APPROVING', 1736243043609, NULL, 'R1', 1736243043609, 1736243043609, 'admin', 'admin'),
    ('todo_cc_inst_002', 'approval_flow_test_001', 'quotation', 'todo_cc_resource_002', 'admin', 'node_cc_002', 'APPROVED', 1736244043609, 1736245043609, 'R2', 1736244043609, 1736245043609, 'admin', 'admin'),
    ('todo_cc_inst_003', 'approval_flow_test_001', 'order', 'todo_cc_resource_003', 'other_user', 'node_cc_003', 'APPROVING', 1736246043609, NULL, 'R3', 1736246043609, 1736246043609, 'admin', 'admin');

INSERT INTO approval_task (`id`, `node_id`, `instance_id`, `approver_id`, `status`, `type`, `action`, `create_time`, `update_time`, `create_user`, `update_user`)
VALUES
    ('todo_cc_task_001', 'node_cc_001', 'todo_cc_inst_001', 'admin', 'PENDING', 'cc', 'READ', 1736243043609, 1736243043609, 'admin', 'admin'),
    ('todo_cc_task_002', 'node_cc_002', 'todo_cc_inst_002', 'admin', 'APPROVED', 'approve', 'APPROVED', 1736245043609, 1736245043609, 'admin', 'admin'),
    ('todo_cc_task_003', 'node_cc_003', 'todo_cc_inst_003', 'admin', 'PENDING', 'CC', 'READ', 1736246043609, 1736246043609, 'admin', 'admin');
