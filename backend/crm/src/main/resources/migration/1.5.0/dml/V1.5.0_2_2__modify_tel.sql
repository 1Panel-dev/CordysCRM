-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

alter table opportunity_quotation
    add until_time bigint not null comment '有效期至';

CREATE INDEX idx_until_time
    ON opportunity_quotation (until_time);


CREATE INDEX idx_plan_end_time
    ON contract_payment_plan (plan_end_time);



SET SESSION innodb_lock_wait_timeout = DEFAULT;