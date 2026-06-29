-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

-- 历史数据处理：根据 approval_flow 的 create_execute 和 update_execute，将原来一份节点数据拆成多份
-- 对 create_execute=true 的审批流，设置 execute_time = CREATE
UPDATE approval_node an
INNER JOIN approval_flow_version afv ON an.flow_version_id = afv.id
INNER JOIN approval_flow af ON afv.flow_id = af.id
SET an.execute_time = 'CREATE'
WHERE af.create_execute = 1;

-- 对仅 update_execute=true 的审批流，设置 execute_time = UPDATE
UPDATE approval_node an
INNER JOIN approval_flow_version afv ON an.flow_version_id = afv.id
INNER JOIN approval_flow af ON afv.flow_id = af.id
SET an.execute_time = 'UPDATE'
WHERE af.update_execute = 1 AND (af.create_execute = 0 OR af.create_execute IS NULL);

-- 对 create_execute=true AND update_execute=true 的审批流，拆分节点数据，复制一份作为 UPDATE（approval_node）
INSERT INTO approval_node (id, flow_version_id, number, name, node_type, sort, execute_time)
SELECT
    CAST(an.id AS UNSIGNED) + 1000,
    an.flow_version_id,
    an.number,
    an.name,
    an.node_type,
    an.sort,
    'UPDATE' AS execute_time
FROM approval_node an
INNER JOIN approval_flow_version afv ON an.flow_version_id = afv.id
INNER JOIN approval_flow af ON afv.flow_id = af.id
WHERE af.create_execute = 1 AND af.update_execute = 1
  AND an.execute_time = 'CREATE';

-- 复制 approval_node_approver（审批人节点配置）
INSERT INTO approval_node_approver (id, flow_version_id, approval_type, multi_approver_mode,
    empty_approver_action, fallback_approver, same_submitter_action, approver_type, approver_direction,
    approver_list, cc_type, cc_direction, cc_list, pass_post_config, reject_post_config, field_permissions)
SELECT
    CAST(ana.id AS UNSIGNED) + 1000,
    ana.flow_version_id,
    ana.approval_type,
    ana.multi_approver_mode,
    ana.empty_approver_action,
    ana.fallback_approver,
    ana.same_submitter_action,
    ana.approver_type,
    ana.approver_direction,
    ana.approver_list,
    ana.cc_type,
    ana.cc_direction,
    ana.cc_list,
    ana.pass_post_config,
    ana.reject_post_config,
    ana.field_permissions
FROM approval_node_approver ana
INNER JOIN approval_node an ON ana.id = an.id
INNER JOIN approval_flow_version afv ON an.flow_version_id = afv.id
INNER JOIN approval_flow af ON afv.flow_id = af.id
WHERE af.create_execute = 1 AND af.update_execute = 1
  AND an.execute_time = 'CREATE';

-- 复制 approval_node_condition（条件节点配置）
INSERT INTO approval_node_condition (id, flow_version_id, condition_config)
SELECT
    CAST(anc.id AS UNSIGNED) + 1000,
    anc.flow_version_id,
    anc.condition_config
FROM approval_node_condition anc
INNER JOIN approval_node an ON anc.id = an.id
INNER JOIN approval_flow_version afv ON an.flow_version_id = afv.id
INNER JOIN approval_flow af ON afv.flow_id = af.id
WHERE af.create_execute = 1 AND af.update_execute = 1
  AND an.execute_time = 'CREATE';

-- 复制 approval_node_link（节点连接关系）
-- from_node_id 和 to_node_id 都需要映射到新ID
INSERT INTO approval_node_link (id, flow_version_id, from_node_id, to_node_id, sort)
SELECT
    UUID_SHORT(),
    anl.flow_version_id,
    CAST(anl.from_node_id AS UNSIGNED) + 1000 AS from_node_id,
    CAST(anl.to_node_id AS UNSIGNED) + 1000 AS to_node_id,
    anl.sort
FROM approval_node_link anl
INNER JOIN approval_node from_node ON anl.from_node_id = from_node.id
INNER JOIN approval_flow_version afv ON from_node.flow_version_id = afv.id
INNER JOIN approval_flow af ON afv.flow_id = af.id
WHERE af.create_execute = 1 AND af.update_execute = 1
  AND af.deleted = 0
  AND from_node.execute_time = 'CREATE';

-- 处理已经审批通过的历史数据，设置通过标识位
update contract set approved = 1 where approval_status = 'APPROVED';
update opportunity_quotation set approved = 1 where approval_status = 'APPROVED';
update contract_invoice set approved = 1 where approval_status = 'APPROVED';
update sales_order set approved = 1 where approval_status = 'APPROVED';

-- 再执行一次审批中历史数据
delete from sys_parameter where param_key in ('handler.contract.approval.status', 'handler.contract.invoice.approval.status', 'handler.quotation.approval.status', 'handler.order.approval.status');
delete from approval_instance where approval_status = 'APPROVING';

-- set innodb lock wait timeout to default
SET SESSION innodb_lock_wait_timeout = DEFAULT;
