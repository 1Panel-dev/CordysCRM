-- 审批人引用用户 ID，需要容纳新增用户的 32 位 UUIDv7。
ALTER TABLE approval_task
    MODIFY COLUMN approver_id VARCHAR(32) NOT NULL COMMENT '审批人ID';
