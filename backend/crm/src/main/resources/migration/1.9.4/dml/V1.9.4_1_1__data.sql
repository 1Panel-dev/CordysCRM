-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

-- 商机审批结果通知
INSERT INTO sys_message_task (id, event, task_type, email_enable, sys_enable, organization_id, template, create_user, create_time, update_user, update_time)
    VALUE (UUID_SHORT(), 'BUSINESS_APPROVAL', 'OPPORTUNITY', false, true,'100001', null, 'admin', UNIX_TIMESTAMP() * 1000 + 2, 'admin', UNIX_TIMESTAMP() * 1000 + 2 );

-- 线索审批结果通知
INSERT INTO sys_message_task (id, event, task_type, email_enable, sys_enable, organization_id, template, create_user, create_time, update_user, update_time)
    VALUE (UUID_SHORT(), 'CLUE_APPROVAL', 'CLUE', false, true,'100001', null, 'admin', UNIX_TIMESTAMP() * 1000 + 3, 'admin', UNIX_TIMESTAMP() * 1000 + 3 );

-- 客户审批结果通知
INSERT INTO sys_message_task (id, event, task_type, email_enable, sys_enable, organization_id, template, create_user, create_time, update_user, update_time)
    VALUE (UUID_SHORT(), 'CUSTOMER_APPROVAL', 'CUSTOMER', false, true,'100001', null, 'admin', UNIX_TIMESTAMP() * 1000 + 4, 'admin', UNIX_TIMESTAMP() * 1000 + 4 );



-- 回填历史审批实例的资源名称, 取值口径与原待办列表的 join 一致;
-- 业务资源已被删除的实例保持为空, 列表按 '资源不存在' 展示。
UPDATE approval_instance ai
    INNER JOIN clue cl ON ai.type = 'clue' AND ai.resource_id = cl.id
    SET ai.resource_name = cl.name
WHERE ai.resource_name IS NULL;

UPDATE approval_instance ai
    INNER JOIN customer cu ON ai.type = 'customer' AND ai.resource_id = cu.id
    SET ai.resource_name = cu.name
WHERE ai.resource_name IS NULL;

UPDATE approval_instance ai
    INNER JOIN opportunity op ON ai.type = 'opportunity' AND ai.resource_id = op.id
    SET ai.resource_name = op.name
WHERE ai.resource_name IS NULL;

UPDATE approval_instance ai
    INNER JOIN opportunity_quotation oq ON ai.type IN ('quotation', 'quote') AND ai.resource_id = oq.id
    SET ai.resource_name = oq.name
WHERE ai.resource_name IS NULL;

UPDATE approval_instance ai
    INNER JOIN contract c ON ai.type = 'contract' AND ai.resource_id = c.id
    SET ai.resource_name = c.name
WHERE ai.resource_name IS NULL;

UPDATE approval_instance ai
    INNER JOIN sales_order so ON ai.type = 'order' AND ai.resource_id = so.id
    SET ai.resource_name = so.name
WHERE ai.resource_name IS NULL;

UPDATE approval_instance ai
    INNER JOIN contract_invoice ci ON ai.type = 'invoice' AND ai.resource_id = ci.id
    SET ai.resource_name = ci.name
WHERE ai.resource_name IS NULL;

-- 自定义表单: 实例 type 存的是 customFormId
UPDATE approval_instance ai
    INNER JOIN custom_form_data cfd ON ai.type = cfd.custom_form_id AND ai.resource_id = cfd.id
    SET ai.resource_name = cfd.name
WHERE ai.resource_name IS NULL;

-- set innodb lock wait timeout to default
SET SESSION innodb_lock_wait_timeout = DEFAULT;
