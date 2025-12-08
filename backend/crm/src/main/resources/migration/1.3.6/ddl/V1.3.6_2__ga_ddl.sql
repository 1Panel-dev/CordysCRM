-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

alter table sys_organization_user
    ADD COLUMN onboarding_date BIGINT COMMENT '入职时间';




-- set innodb lock wait timeout to default
SET SESSION innodb_lock_wait_timeout = DEFAULT;



