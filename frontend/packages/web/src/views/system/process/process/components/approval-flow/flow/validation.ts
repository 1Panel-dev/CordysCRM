import { watch } from 'vue';

import { ApprovalTypeEnum, ApproverTypeEnum, EmptyApproverActionEnum } from '@lib/shared/enums/process';
import type { ApprovalActionNode, ApprovalConditionBranch, BasicFormParams } from '@lib/shared/models/system/process';

import type { FlowNode, FlowSchema } from '@/components/business/crm-flow/types';

import { hasConfiguredCondition } from './index';
import type { Ref } from 'vue';

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

function findStartNode(nodes: FlowNode[]) {
  return nodes.find((node) => node.type === 'start');
}

function isEmptyValue(value: unknown) {
  return value === null || value === undefined || (typeof value === 'string' && !value.trim());
}

// 只挑“开始节点校验相关”的字段做监听，避免无关字段变化时也触发清错。
function createBasicConfigValidationSnapshot(basicConfig: BasicFormParams) {
  return {
    name: basicConfig.name,
    createExecute: basicConfig.createExecute,
    updateExecute: basicConfig.updateExecute,
  };
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

// 根据本次校验结果回写红框：开始/审批节点写 node.invalid，if 分支写 branch.invalid。
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

// 开始节点校验
function validateBasicConfig(flowSchema: FlowSchema, basicConfig: BasicFormParams, result: FlowValidationResult) {
  const startNode = findStartNode(flowSchema.nodes);
  if (startNode && (isEmptyValue(basicConfig.name) || (!basicConfig.createExecute && !basicConfig.updateExecute))) {
    result.invalidNodeIds.push(startNode.id);
  }
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
    (isManualApproval &&
      (isEmptyValue(node.approverType) ||
        (isMemberOrRole(node.approverType) && !hasSelectedItems(approverList)) ||
        ([EmptyApproverActionEnum.ASSIGN_SPECIFIC, EmptyApproverActionEnum.ASSIGN_ADMIN].includes(
          node.emptyApproverAction
        ) &&
          isEmptyValue(node.fallbackApprover)) ||
        (isMemberOrRole(node.ccType) && !hasSelectedItems(ccList))));

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
function validateFlow(flowSchema: FlowSchema, basicConfig: BasicFormParams): FlowValidationResult {
  const result: FlowValidationResult = {
    invalidNodeIds: [],
    invalidBranchIds: [],
  };

  validateBasicConfig(flowSchema, basicConfig, result);
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

export default function useFlowValidation(params: { flowSchema: Ref<FlowSchema>; basicConfig: Ref<BasicFormParams> }) {
  // 给保存按钮调用：一次完成“清旧状态 -> 全量校验 -> 回写红框”。
  function validateFlowNodes() {
    invalidClearLocked = true;
    clearFlowInvalidMarks(params.flowSchema.value);
    const result = validateFlow(params.flowSchema.value, params.basicConfig.value);
    applyFlowInvalidMarks(params.flowSchema.value, result);

    if (result.invalidNodeIds.length || result.invalidBranchIds.length) {
      return false;
    }

    invalidClearLocked = false;

    return true;
  }

  // 开始节点基础配置一旦被修改，就先把开始节点红框去掉，体验上比一直红着更自然。
  watch(
    () => createBasicConfigValidationSnapshot(params.basicConfig.value),
    () => {
      clearInvalidState(findStartNode(params.flowSchema.value.nodes));
    },
    {
      deep: true,
    }
  );

  return {
    validateFlowNodes,
  };
}
