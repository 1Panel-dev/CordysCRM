delete from sys_role where id = 'recycle_role_id';
delete from sys_user_role where id = 'recycle_user_role_id';
delete from sys_department where id = 'recycle_department_id';
delete from sys_organization_user where id = 'recycle_organization_user_id';

delete from clue_pool where id = 'job_pool_id';
delete from clue_pool_recycle_rule where id = 'job_recycle_rule_id';