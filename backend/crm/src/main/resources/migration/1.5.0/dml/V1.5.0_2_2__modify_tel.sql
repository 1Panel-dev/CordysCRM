-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

alter table opportunity_quotation
    add until_time bigint not null comment '有效期至';

-- modify form_key length to 50
ALTER TABLE sys_module_form MODIFY COLUMN form_key VARCHAR (50);

CREATE INDEX idx_until_time
    ON opportunity_quotation (until_time);


CREATE INDEX idx_plan_end_time
    ON contract_payment_plan (plan_end_time);


INSERT INTO business_title_config (id, field, required, organization_id)
VALUES (UUID_SHORT(), 'name', true, '100001'),
       (UUID_SHORT(), 'identification_number', true, '100001'),
       (UUID_SHORT(), 'opening_bank', false, '100001'),
       (UUID_SHORT(), 'bank_account', false, '100001'),
       (UUID_SHORT(), 'registration_address', false, '100001'),
       (UUID_SHORT(), 'phone_number', false, '100001'),
       (UUID_SHORT(), 'registered_capital', false, '100001'),
       (UUID_SHORT(), 'company_size', false, '100001'),
       (UUID_SHORT(), 'registration_number', false, '100001');

alter table contract
    add start_time bigint not null comment '合同开始时间';
alter table contract
    add end_time bigint not null comment '合同结束时间';


SET SESSION innodb_lock_wait_timeout = DEFAULT;