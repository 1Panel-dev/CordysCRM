import { useI18n } from '@lib/shared/hooks/useI18n';

import type { ActionNode, ConditionBranch, ConditionGroupNode, EndNode, StartNode } from '../types';

const { t } = useI18n();
let flowIdSeed = 0;

export function createFlowId(prefix = 'node'): string {
  flowIdSeed += 1; // 防止id冲突
  return `${prefix}_${Date.now().toString(36)}_${flowIdSeed.toString(36)}`;
}

export function createStartNode(partial: Partial<StartNode> = {}): StartNode {
  return {
    id: partial.id ?? createFlowId('start'),
    type: 'start',
    name: partial.name ?? t('common.start'),
    description: partial.description,
  };
}

export function createActionNode<T extends ActionNode>(partial: Partial<T> = {}): T {
  return {
    id: partial.id ?? createFlowId('action'),
    type: 'action',
    name: partial.name ?? '',
    actionType: partial.actionType ?? 'approval',
    description: partial.description,
    ...partial,
  } as T;
}

export function createEndNode(partial: Partial<EndNode> = {}): EndNode {
  return {
    id: partial.id ?? createFlowId('end'),
    type: 'end',
    name: partial.name ?? t('common.end'),
  };
}

export function createConditionBranch<T extends ConditionBranch>(partial: Partial<T> = {}): T {
  return {
    id: partial.id ?? createFlowId('branch_if'),
    name: partial.name ?? t('crmFlow.triggerCondition'),
    isElse: false,
    description: partial.description ?? t('crmFlow.pleaseSet'),
    children: partial.children ?? [],
    ...partial,
  } as T;
}

export function createElseBranch<T extends ConditionBranch>(partial: Partial<T> = {}): T {
  return {
    id: partial.id ?? createFlowId('branch_else'),
    name: partial.name ?? t('crmFlow.else'),
    isElse: true,
    description: partial.description ?? t('crmFlow.elseDescription'),
    children: partial.children ?? [],
    ...partial,
  } as T;
}

export function createConditionGroupNode(partial: Partial<ConditionGroupNode> = {}): ConditionGroupNode {
  return {
    id: partial.id ?? createFlowId('condition_group'),
    type: 'condition-group',
    name: partial.name ?? '',
    branches: partial.branches ?? [createConditionBranch(), createElseBranch()],
  };
}
