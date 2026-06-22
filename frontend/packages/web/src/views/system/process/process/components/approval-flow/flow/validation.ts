import { ApprovalTypeEnum, ApproverTypeEnum, EmptyApproverActionEnum } from '@lib/shared/enums/process';
import type { ApprovalActionNode, ApprovalConditionBranch } from '@lib/shared/models/system/process';

import type { FlowNode, FlowSchema } from '@/components/business/crm-flow/types';

import { hasConfiguredCondition } from './conditionDescription';

interface FlowValidationResult {
  invalidNodeIds: string[];
  invalidBranchIds: string[];
}

// 流程图不是单链表，条件组下面还会递归挂子节点，所以统一用一个 DFS 遍历入口
function walkFlowNodes(
  nodes: FlowNode[],
  visitor: (payload: { node?: FlowNode; branch?: ApprovalConditionBranch }) => void
) {
  nodes.forEach((node) => {
    visitor({ node });

    if (node.type !== 'condition-group') {
      return;
    }

    (node.branches as ApprovalConditionBranch[]).forEach((branch) => {
      visitor({ branch });
      walkFlowNodes(branch.children, visitor);
    });
  });
}

function isEmptyValue(value: unknown) {
  return value === null || value === undefined || (typeof value === 'string' && !value.trim());
}

// 清除红框
export function clearInvalidState(target?: { invalid?: boolean } | null) {
  if (!target) {
    return;
  }

  target.invalid = false;
}

// 保存前先全量清空 invalid，再按照本次校验结果重新打标，避免旧红框残留。
function clearFlowInvalidMarks(flowSchema: FlowSchema) {
  walkFlowNodes(flowSchema.nodes, ({ node, branch }) => {
    clearInvalidState(node);
    clearInvalidState(branch);
  });
}

// 根据本次校验结果回写红框：审批节点写 node.invalid，if 分支写 branch.invalid。
function applyFlowInvalidMarks(flowSchema: FlowSchema, result: FlowValidationResult) {
  const invalidNodeIds = new Set(result.invalidNodeIds);
  const invalidBranchIds = new Set(result.invalidBranchIds);

  walkFlowNodes(flowSchema.nodes, ({ node, branch }) => {
    if (node && invalidNodeIds.has(node.id)) {
      node.invalid = true;
    }

    if (branch && invalidBranchIds.has(branch.id)) {
      branch.invalid = true;
    }
  });
}

function isMemberOrRole(type?: ApproverTypeEnum | null) {
  return !!type && [ApproverTypeEnum.SPECIFIED_MEMBER, ApproverTypeEnum.ROLE].includes(type);
}

function hasSelectedItems(list?: unknown[]) {
  return Array.isArray(list) && list.some((item) => !isEmptyValue(item));
}

// 审批节点校验
function validateApprovalActionNode(node: ApprovalActionNode, result: FlowValidationResult) {
  const isManualApproval = node.approvalType === ApprovalTypeEnum.MANUAL;
  const approverList = node.approverList ?? [];
  const ccList = node.ccList ?? [];
  const isInvalid =
    isEmptyValue(node.name) ||
    (isMemberOrRole(node.ccType) && !hasSelectedItems(ccList)) ||
    (isManualApproval &&
      (isEmptyValue(node.approverType) ||
        (isMemberOrRole(node.approverType) && !hasSelectedItems(approverList)) ||
        ([EmptyApproverActionEnum.ASSIGN_SPECIFIC, EmptyApproverActionEnum.ASSIGN_ADMIN].includes(
          node.emptyApproverAction
        ) &&
          isEmptyValue(node.fallbackApprover))));

  if (isInvalid) {
    result.invalidNodeIds.push(node.id);
  }
}

// if分支校验
function validateConditionBranch(branch: ApprovalConditionBranch, result: FlowValidationResult) {
  if (branch.isElse) {
    return;
  }

  if (!hasConfiguredCondition(branch.conditionConfig)) {
    result.invalidBranchIds.push(branch.id);
  }
}

// 保存前的总入口：只做纯数据校验，收集需要标红的节点和分支
function validateFlow(flowSchema: FlowSchema): FlowValidationResult {
  const result: FlowValidationResult = {
    invalidNodeIds: [],
    invalidBranchIds: [],
  };

  walkFlowNodes(flowSchema.nodes, ({ node, branch }) => {
    if (node?.type === 'action' && node.actionType === 'approval') {
      validateApprovalActionNode(node as ApprovalActionNode, result);
    }

    if (branch) {
      validateConditionBranch(branch, result);
    }
  });

  return result;
}

// 保存校验后，当前已选中的审批节点右侧表单可能触发回填更新
// 这里先锁住“自动清红框”，只在用户真的开始编辑当前节点时再解锁。
let invalidClearLocked = false;

export function canClearInvalidState() {
  return !invalidClearLocked;
}

export function unlockInvalidClearState() {
  invalidClearLocked = false;
}

// 给保存按钮调用：一次完成“清旧状态 -> 全量校验 -> 回写红框”。
export function validateFlowNodes(flowSchema: FlowSchema) {
  invalidClearLocked = true;
  clearFlowInvalidMarks(flowSchema);
  const result = validateFlow(flowSchema);
  applyFlowInvalidMarks(flowSchema, result);

  const valid = !result.invalidNodeIds.length && !result.invalidBranchIds.length;
  if (valid) {
    invalidClearLocked = false;
  }

  return valid;
}
