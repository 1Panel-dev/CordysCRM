INSERT INTO contract_payment_plan_field (id, resource_id, field_id, field_value)
SELECT UUID_SHORT(), p.id, (
    SELECT f.id FROM sys_module_form form
    JOIN sys_module_field f ON f.form_id = form.id
    WHERE form.organization_id = p.organization_id AND form.form_key = 'contractPaymentPlan'
      AND f.internal_key = 'contractPaymentPlanPlanAmount'
), CAST(p.plan_amount AS CHAR)
FROM contract_payment_plan p WHERE p.plan_amount IS NOT NULL
UNION ALL
SELECT UUID_SHORT(), p.id, (
    SELECT f.id FROM sys_module_form form
    JOIN sys_module_field f ON f.form_id = form.id
    WHERE form.organization_id = p.organization_id AND form.form_key = 'contractPaymentPlan'
      AND f.internal_key = 'contractPaymentPlanPlanEndTime'
), CAST(p.plan_end_time AS CHAR)
FROM contract_payment_plan p WHERE p.plan_end_time IS NOT NULL;
