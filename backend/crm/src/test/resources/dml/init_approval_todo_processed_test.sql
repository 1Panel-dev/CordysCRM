DELETE FROM approval_task WHERE id IN ('todo_processed_task_001', 'todo_pending_task_001');
DELETE FROM approval_instance WHERE id IN ('todo_processed_inst_001', 'todo_pending_inst_001');

INSERT INTO approval_instance (`id`, `flow_version_id`, `type`, `resource_id`, `submitter_id`, `current_node_id`, `approval_status`, `submit_time`, `approval_time`,`create_time`, `update_time`, `create_user`, `update_user`)
VALUES
    ('todo_processed_inst_001', 'approval_flow_test_001', 'contract', 'todo_processed_resource_001', 'admin', 'node_done', 'APPROVED', 1736240043609, 1736241043609, 1736240043609, 1736241043609, 'admin', 'admin'),
    ('todo_pending_inst_001', 'approval_flow_test_001', 'contract', 'todo_pending_resource_001', 'admin', 'node_wait','APPROVED',1736242043609, 1736241043609,1736242043609, 1736242043609, 'admin', 'admin');

INSERT INTO approval_task (`id`, `node_id`, `instance_id`, `approver_id`, `status`, `type`, `action`, `create_time`, `update_time`, `create_user`, `update_user`)
VALUES
    ('todo_processed_task_001', 'node_done', 'todo_processed_inst_001', 'admin', 'APPROVED', 'approve', 'APPROVED', 1736241043609, 1736241043609, 'admin', 'admin'),
    ('todo_pending_task_001', 'node_wait', 'todo_pending_inst_001', 'admin', 'PENDING', 'approve', 'PENDING', 1736242043609, 1736242043609, 'admin', 'admin');
