DELETE FROM approval_record WHERE id IN ('approval_record_current', 'approval_record_other');
DELETE FROM approval_task WHERE id IN ('approval_task_current', 'approval_task_other');
DELETE FROM approval_instance WHERE id = 'approval_instance_test_001';
DELETE FROM approval_flow_version WHERE id = 'approval_resource_flow_test_001';
DELETE FROM sys_user WHERE id IN ('appr_user_curr', 'appr_user_othr');
