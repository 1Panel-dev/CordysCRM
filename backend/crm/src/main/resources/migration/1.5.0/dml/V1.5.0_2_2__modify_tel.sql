-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

alter table opportunity_quotation
    add until_time bigint not null comment '有效期至';

-- modify form_key length to 50
ALTER TABLE sys_module_form MODIFY COLUMN form_key VARCHAR(50);

CREATE INDEX idx_until_time
    ON opportunity_quotation (until_time);


CREATE INDEX idx_plan_end_time
    ON contract_payment_plan (plan_end_time);


INSERT INTO business_title_config (id, field, required)
VALUES
    (UUID_SHORT(), 'business_name', true),
    (UUID_SHORT(), 'identification_number', true),
    (UUID_SHORT(), 'opening_bank', false),
    (UUID_SHORT(), 'bank_account', false),
    (UUID_SHORT(), 'registration_address', false),
    (UUID_SHORT(), 'phone_number', false),
    (UUID_SHORT(), 'registered_capital', false),
    (UUID_SHORT(), 'customer_size', false),
    (UUID_SHORT(), 'registration_number', false);



SET SESSION innodb_lock_wait_timeout = DEFAULT;