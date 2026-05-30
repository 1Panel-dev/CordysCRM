-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

-- init biz_id by group (resource_id, row_id)
UPDATE product_price_field f JOIN (
    SELECT resource_id, row_id, UUID_SHORT() AS biz_id FROM product_price_field
    WHERE row_id IS NOT NULL AND biz_id IS NULL GROUP BY resource_id, row_id
) t ON f.resource_id = t.resource_id AND f.row_id = t.row_id
SET f.biz_id = t.biz_id
WHERE f.row_id IS NOT NULL AND f.biz_id IS NULL;

UPDATE product_price_field_blob f JOIN (
    SELECT resource_id, row_id, UUID_SHORT() AS biz_id FROM product_price_field_blob
    WHERE row_id IS NOT NULL AND biz_id IS NULL GROUP BY resource_id, row_id
) t ON f.resource_id = t.resource_id AND f.row_id = t.row_id
SET f.biz_id = t.biz_id
WHERE f.row_id IS NOT NULL AND f.biz_id IS NULL;

UPDATE contract_field f JOIN (
    SELECT resource_id, row_id, UUID_SHORT() AS biz_id FROM contract_field
    WHERE row_id IS NOT NULL AND biz_id IS NULL GROUP BY resource_id, row_id
) t ON f.resource_id = t.resource_id AND f.row_id = t.row_id
SET f.biz_id = t.biz_id
WHERE f.row_id IS NOT NULL AND f.biz_id IS NULL;

UPDATE contract_field_blob f JOIN (
    SELECT resource_id, row_id, UUID_SHORT() AS biz_id FROM contract_field_blob
    WHERE row_id IS NOT NULL AND biz_id IS NULL GROUP BY resource_id, row_id
) t ON f.resource_id = t.resource_id AND f.row_id = t.row_id
SET f.biz_id = t.biz_id
WHERE f.row_id IS NOT NULL AND f.biz_id IS NULL;

UPDATE opportunity_quotation_field f JOIN (
    SELECT resource_id, row_id, UUID_SHORT() AS biz_id FROM opportunity_quotation_field
    WHERE row_id IS NOT NULL AND biz_id IS NULL GROUP BY resource_id, row_id
) t ON f.resource_id = t.resource_id AND f.row_id = t.row_id
SET f.biz_id = t.biz_id
WHERE f.row_id IS NOT NULL AND f.biz_id IS NULL;

UPDATE opportunity_quotation_field_blob f JOIN (
    SELECT resource_id, row_id, UUID_SHORT() AS biz_id FROM opportunity_quotation_field_blob
    WHERE row_id IS NOT NULL AND biz_id IS NULL GROUP BY resource_id, row_id
) t ON f.resource_id = t.resource_id AND f.row_id = t.row_id
SET f.biz_id = t.biz_id
WHERE f.row_id IS NOT NULL AND f.biz_id IS NULL;

SET SESSION innodb_lock_wait_timeout = DEFAULT;