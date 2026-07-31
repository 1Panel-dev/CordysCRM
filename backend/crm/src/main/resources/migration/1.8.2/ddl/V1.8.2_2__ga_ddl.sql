-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

-- modify field table, add unique id
ALTER TABLE contract_invoice_field ADD COLUMN ref_sub_id VARCHAR(32) NULL COMMENT '引用子表格ID';
ALTER TABLE contract_invoice_field ADD COLUMN row_id VARCHAR(32) NULL COMMENT '子表格行实例ID';
ALTER TABLE contract_invoice_field ADD COLUMN biz_id VARCHAR(32) NULL COMMENT '唯一业务行ID';

ALTER TABLE contract_invoice_field_blob ADD COLUMN ref_sub_id VARCHAR(32) NULL COMMENT '引用子表格ID';
ALTER TABLE contract_invoice_field_blob ADD COLUMN row_id VARCHAR(32) NULL COMMENT '子表格行实例ID';
ALTER TABLE contract_invoice_field_blob ADD COLUMN biz_id VARCHAR(32) NULL COMMENT '唯一业务行ID';

ALTER TABLE custom_form_data_field ADD COLUMN ref_sub_id VARCHAR(32) NULL COMMENT '引用子表格ID';
ALTER TABLE custom_form_data_field ADD COLUMN row_id VARCHAR(32) NULL COMMENT '子表格行实例ID';
ALTER TABLE custom_form_data_field ADD COLUMN biz_id VARCHAR(32) NULL COMMENT '唯一业务行ID';

ALTER TABLE custom_form_data_field_blob ADD COLUMN ref_sub_id VARCHAR(32) NULL COMMENT '引用子表格ID';
ALTER TABLE custom_form_data_field_blob ADD COLUMN row_id VARCHAR(32) NULL COMMENT '子表格行实例ID';
ALTER TABLE custom_form_data_field_blob ADD COLUMN biz_id VARCHAR(32) NULL COMMENT '唯一业务行ID';

-- add unique index
CREATE UNIQUE INDEX uk_contract_invoice_field_cell ON contract_field (resource_id, row_id, field_id);
CREATE UNIQUE INDEX uk_contract_invoice_field_blob_cell ON contract_field_blob (resource_id, row_id, field_id);

CREATE UNIQUE INDEX uk_custom_form_data_field_cell ON contract_field (resource_id, row_id, field_id);
CREATE UNIQUE INDEX uk_custom_form_data_field_blob_cell ON contract_field_blob (resource_id, row_id, field_id);

-- set innodb lock wait timeout to default
SET SESSION innodb_lock_wait_timeout = DEFAULT;
