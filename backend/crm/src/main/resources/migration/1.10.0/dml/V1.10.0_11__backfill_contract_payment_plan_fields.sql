INSERT INTO contract_payment_plan_field (id, resource_id, field_id, field_value)
SELECT UUID_SHORT(), plan.id, module_field.id,
       CASE module_field.internal_key
           WHEN 'contractPaymentPlanPlanAmount' THEN CAST(plan.plan_amount AS CHAR)
           WHEN 'contractPaymentPlanPlanEndTime' THEN CAST(plan.plan_end_time AS CHAR)
       END
FROM contract_payment_plan plan
JOIN sys_module_form module_form ON module_form.organization_id = plan.organization_id
    AND module_form.form_key = 'contractPaymentPlan'
JOIN sys_module_field module_field ON module_field.form_id = module_form.id
WHERE ((module_field.internal_key = 'contractPaymentPlanPlanAmount' AND plan.plan_amount IS NOT NULL)
    OR (module_field.internal_key = 'contractPaymentPlanPlanEndTime' AND plan.plan_end_time IS NOT NULL))
  AND NOT EXISTS (
      SELECT 1
      FROM contract_payment_plan_field existing
      WHERE existing.resource_id = plan.id
        AND existing.row_id IS NULL
        AND existing.field_id = module_field.id
  );
