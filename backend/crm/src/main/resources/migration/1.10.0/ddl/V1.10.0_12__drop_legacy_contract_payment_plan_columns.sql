-- 确认每个非空旧值都有对应的自定义字段记录，再移除旧列。
CREATE TEMPORARY TABLE IF NOT EXISTS contract_payment_plan_backfill_check (
    missing_count BIGINT NOT NULL,
    CONSTRAINT ck_contract_payment_plan_fields_backfilled CHECK (missing_count = 0)
);

INSERT INTO contract_payment_plan_backfill_check (missing_count)
SELECT COUNT(*)
FROM contract_payment_plan plan
CROSS JOIN (
    SELECT 'contractPaymentPlanPlanAmount' AS internal_key
    UNION ALL
    SELECT 'contractPaymentPlanPlanEndTime'
) expected_field
WHERE ((expected_field.internal_key = 'contractPaymentPlanPlanAmount' AND plan.plan_amount IS NOT NULL)
    OR (expected_field.internal_key = 'contractPaymentPlanPlanEndTime' AND plan.plan_end_time IS NOT NULL))
  AND NOT EXISTS (
      SELECT 1
      FROM sys_module_form module_form
      JOIN sys_module_field module_field ON module_field.form_id = module_form.id
         AND module_field.internal_key = expected_field.internal_key
      JOIN contract_payment_plan_field stored_field ON stored_field.field_id = module_field.id
         AND stored_field.resource_id = plan.id
         AND stored_field.row_id IS NULL
      WHERE module_form.organization_id = plan.organization_id
        AND module_form.form_key = 'contractPaymentPlan'
  );

DROP TEMPORARY TABLE contract_payment_plan_backfill_check;

ALTER TABLE contract_payment_plan
    DROP COLUMN plan_amount,
    DROP COLUMN plan_end_time;
