-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

-- modify field table, add unique id
ALTER TABLE product_price_field ADD COLUMN biz_id VARCHAR(32) NULL COMMENT '唯一业务行ID' AFTER row_id;
ALTER TABLE product_price_field_blob ADD COLUMN biz_id VARCHAR(32) NULL COMMENT '唯一业务行ID' AFTER row_id;
ALTER TABLE contract_field ADD COLUMN biz_id VARCHAR(32) NULL COMMENT '唯一业务行ID' AFTER row_id;
ALTER TABLE contract_field_blob ADD COLUMN biz_id VARCHAR(32) NULL COMMENT '唯一业务行ID' AFTER row_id;
ALTER TABLE opportunity_quotation_field ADD COLUMN biz_id VARCHAR(32) NULL COMMENT '唯一业务行ID' AFTER row_id;
ALTER TABLE opportunity_quotation_field_blob ADD COLUMN biz_id VARCHAR(32) NULL COMMENT '唯一业务行ID' AFTER row_id;

-- add unique index
CREATE UNIQUE INDEX uk_price_field_cell ON product_price_field (resource_id, row_id, field_id);
CREATE UNIQUE INDEX uk_price_field_blob_cell ON product_price_field_blob (resource_id, row_id, field_id);
CREATE UNIQUE INDEX uk_contract_field_cell ON contract_field (resource_id, row_id, field_id);
CREATE UNIQUE INDEX uk_contract_field_blob_cell ON contract_field_blob (resource_id, row_id, field_id);
CREATE UNIQUE INDEX uk_opp_quotation_field_cell ON opportunity_quotation_field (resource_id, row_id, field_id);
CREATE UNIQUE INDEX uk_opp_quotation_field_blob_cell ON opportunity_quotation_field_blob (resource_id, row_id, field_id);

-- set innodb lock wait timeout to default
SET SESSION innodb_lock_wait_timeout = DEFAULT;