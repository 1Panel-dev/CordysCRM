-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

-- Add pool pick rule
ALTER TABLE sys_user_view_condition ADD children_value text DEFAULT NULL COMMENT '包含新增子部门集合';


-- set innodb lock wait timeout to default
SET SESSION innodb_lock_wait_timeout = DEFAULT;



