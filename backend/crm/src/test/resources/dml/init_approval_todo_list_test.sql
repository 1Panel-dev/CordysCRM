DELETE FROM approval_task WHERE id IN (
    'todo_list_task_contract',
    'todo_list_task_quote',
    'todo_list_task_old_node'
);
DELETE FROM approval_instance WHERE id IN (
    'todo_list_inst_contract',
    'todo_list_inst_quote'
);

INSERT INTO approval_instance (`id`, `flow_id`, `type`, `resource_id`, `submitter_id`, `current_node_id`, `approval_status`, `submit_time`, `approval_time`, `result`, `create_time`, `update_time`, `create_user`, `update_user`)
VALUES
    ('todo_list_inst_contract', 'approval_flow_test_001', 'contract', 'todo_list_resource_contract', 'admin', 'node_contract_current', 'APPROVING', 1736240043609, NULL, NULL, 1736240043609, 1736240043609, 'admin', 'admin'),
    ('todo_list_inst_quote', 'approval_flow_test_001', 'quotation', 'todo_list_resource_quote', 'admin', 'node_quote_current', 'APPROVING', 1736241043609, NULL, NULL, 1736241043609, 1736241043609, 'admin', 'admin');

INSERT INTO approval_task (`id`, `node_id`, `instance_id`, `approver_id`, `status`, `type`, `action`, `create_time`, `update_time`, `create_user`, `update_user`)
VALUES
    ('todo_list_task_contract', 'node_contract_current', 'todo_list_inst_contract', 'admin', 'PENDING', 'approve', 'PENDING', 1736240043609, 1736240043609, 'admin', 'admin'),
    ('todo_list_task_quote', 'node_quote_current', 'todo_list_inst_quote', 'admin', 'PENDING', 'approve', 'PENDING', 1736241043609, 1736241043609, 'admin', 'admin'),
    ('todo_list_task_old_node', 'node_contract_old', 'todo_list_inst_contract', 'admin', 'PENDING', 'approve', 'PENDING', 1736242043609, 1736242043609, 'admin', 'admin');
