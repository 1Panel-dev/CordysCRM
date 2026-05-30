-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

-- modify unique index of sub table
ALTER TABLE product_price_field DROP INDEX uk_price_field_cell,
    ADD UNIQUE INDEX uk_price_field_cell (resource_id, ref_sub_id, row_id, field_id);
ALTER TABLE product_price_field_blob DROP INDEX uk_price_field_blob_cell,
    ADD UNIQUE INDEX uk_price_field_blob_cell (resource_id, ref_sub_id, row_id, field_id);
ALTER TABLE contract_field DROP INDEX uk_contract_field_cell,
    ADD UNIQUE INDEX uk_contract_field_cell (resource_id, ref_sub_id, row_id, field_id);
ALTER TABLE contract_field_blob DROP INDEX uk_contract_field_blob_cell,
    ADD UNIQUE INDEX uk_contract_field_blob_cell (resource_id, ref_sub_id, row_id, field_id);
ALTER TABLE opportunity_quotation_field DROP INDEX uk_opp_quotation_field_cell,
    ADD UNIQUE INDEX uk_opp_quotation_field_cell (resource_id, ref_sub_id, row_id, field_id);
ALTER TABLE opportunity_quotation_field_blob DROP INDEX uk_opp_quotation_field_blob_cell,
    ADD UNIQUE INDEX uk_opp_quotation_field_blob_cell (resource_id, ref_sub_id, row_id, field_id);

-- set innodb lock wait timeout to default
SET SESSION innodb_lock_wait_timeout = DEFAULT;