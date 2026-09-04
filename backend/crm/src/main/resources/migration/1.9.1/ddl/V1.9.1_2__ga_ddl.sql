-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

-- API URL 长度放宽到 255字符
ALTER TABLE agent_model MODIFY COLUMN `api_url` VARCHAR(255) COMMENT 'API请求地址';

-- 历史消息支持状态
ALTER TABLE agent_message
    ADD COLUMN `status` VARCHAR(20) NULL COMMENT '消息状态：done 已完成，stopped 已停止' AFTER `helpful`;

-- 商机、回款计划、回款记录支持子表格
ALTER TABLE opportunity_field ADD COLUMN ref_sub_id VARCHAR(32) NULL COMMENT '引用子表格ID';
ALTER TABLE opportunity_field ADD COLUMN row_id VARCHAR(32) NULL COMMENT '子表格行实例ID';
ALTER TABLE opportunity_field ADD COLUMN biz_id VARCHAR(32) NULL COMMENT '唯一业务行ID';

CREATE INDEX idx_opportunity_field_cell ON opportunity_field (resource_id, row_id, field_id);

ALTER TABLE opportunity_field_blob ADD COLUMN ref_sub_id VARCHAR(32) NULL COMMENT '引用子表格ID';
ALTER TABLE opportunity_field_blob ADD COLUMN row_id VARCHAR(32) NULL COMMENT '子表格行实例ID';
ALTER TABLE opportunity_field_blob ADD COLUMN biz_id VARCHAR(32) NULL COMMENT '唯一业务行ID';

CREATE INDEX idx_opportunity_field_blob_cell ON opportunity_field_blob (resource_id, row_id, field_id);

ALTER TABLE contract_payment_plan_field ADD COLUMN ref_sub_id VARCHAR(32) NULL COMMENT '引用子表格ID';
ALTER TABLE contract_payment_plan_field ADD COLUMN row_id VARCHAR(32) NULL COMMENT '子表格行实例ID';
ALTER TABLE contract_payment_plan_field ADD COLUMN biz_id VARCHAR(32) NULL COMMENT '唯一业务行ID';

CREATE INDEX idx_contract_payment_plan_field_cell ON contract_payment_plan_field (resource_id, row_id, field_id);

ALTER TABLE contract_payment_plan_field_blob ADD COLUMN ref_sub_id VARCHAR(32) NULL COMMENT '引用子表格ID';
ALTER TABLE contract_payment_plan_field_blob ADD COLUMN row_id VARCHAR(32) NULL COMMENT '子表格行实例ID';
ALTER TABLE contract_payment_plan_field_blob ADD COLUMN biz_id VARCHAR(32) NULL COMMENT '唯一业务行ID';

CREATE INDEX idx_contract_payment_plan_field_blob_cell ON contract_payment_plan_field_blob (resource_id, row_id, field_id);

ALTER TABLE contract_payment_record_field ADD COLUMN ref_sub_id VARCHAR(32) NULL COMMENT '引用子表格ID';
ALTER TABLE contract_payment_record_field ADD COLUMN row_id VARCHAR(32) NULL COMMENT '子表格行实例ID';
ALTER TABLE contract_payment_record_field ADD COLUMN biz_id VARCHAR(32) NULL COMMENT '唯一业务行ID';

CREATE INDEX idx_contract_payment_record_field_cell ON contract_payment_record_field (resource_id, row_id, field_id);

ALTER TABLE contract_payment_record_field_blob ADD COLUMN ref_sub_id VARCHAR(32) NULL COMMENT '引用子表格ID';
ALTER TABLE contract_payment_record_field_blob ADD COLUMN row_id VARCHAR(32) NULL COMMENT '子表格行实例ID';
ALTER TABLE contract_payment_record_field_blob ADD COLUMN biz_id VARCHAR(32) NULL COMMENT '唯一业务行ID';

CREATE INDEX idx_contract_payment_record_field_blob_cell ON contract_payment_record_field_blob (resource_id, row_id, field_id);

-- set innodb lock wait timeout to default
SET SESSION innodb_lock_wait_timeout = DEFAULT;
