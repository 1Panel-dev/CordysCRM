<template>
  <div class="h-full w-full">
    <CrmFlow
      ref="crmFlowRef"
      v-model:model="flowSchema"
      :right-content-visible="isRightContentVisible"
      @add-condition-branch="handleAddConditionBranch"
    >
      <template #insertNodeContent="{ anchorNodeId, anchorBranch }">
        <div class="base-box-shadow min-w-[366px] rounded-[6px] bg-[var(--text-n10)] p-[16px]">
          <div v-for="group in addNodeGroups" :key="group.key" class="mb-[16px] last:mb-0">
            <div class="mb-[8px] font-semibold">{{ group.title }}</div>
            <div class="flex gap-[8px]">
              <div
                v-for="item in group.options"
                :key="item.label"
                class="inline-flex h-[38px] cursor-pointer items-center gap-[8px] rounded-[var(--border-radius-small)] border border-transparent bg-[var(--text-n9)] px-[12px] transition-all hover:border-[var(--primary-1)]"
                @click="insertFromPopover(item.type, anchorNodeId, anchorBranch, item.actionApprovalType)"
              >
                <span
                  class="inline-flex size-[16px] items-center justify-center rounded-[var(--border-radius-small)] text-[var(--text-n10)]"
                  :class="item.iconBgClass"
                >
                  <CrmIcon :type="item.icon" :size="12" />
                </span>
                <span>{{ item.label }}</span>
              </div>
            </div>
          </div>
        </div>
      </template>
      <template #rightContent="{ selection }">
        <basicForm
          v-if="selection.type === 'node' && selection?.node.type === 'start'"
          ref="basicFormRef"
          v-model:basicConfig="basicConfig"
        />
        <approvalActionNodeForm
          v-if="selection.type === 'node' && selection?.node.type === 'action'"
          :node="selection.node"
        />
      </template>
    </CrmFlow>
  </div>
</template>

<script setup lang="ts">
  import { computed, nextTick, onMounted, ref, watch } from 'vue';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { BasicFormParams } from '@lib/shared/models/system/process';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import {
    addConditionBranch,
    insertNodeAfterNode,
    insertNodeToConditionBranch,
  } from '@/components/business/crm-flow/dsl/actions';
  import {
    createActionNode,
    createConditionBranch,
    createConditionGroupNode,
    createElseBranch,
    createEndNode,
    createStartNode,
  } from '@/components/business/crm-flow/dsl/factory';
  import { findBranchLocation, findNodeLocation } from '@/components/business/crm-flow/dsl/queries';
  import CrmFlow from '@/components/business/crm-flow/index.vue';
  import type { ConditionBranch, FlowNode, FlowSchema, NodeSelectionState } from '@/components/business/crm-flow/types';
  import approvalActionNodeForm from './approvalActionNodeForm.vue';
  import basicForm from './basicForm.vue';

  import {
    type ApprovalType,
    businessTypeOptions,
    defaultBasicForm,
    executionTimingList,
    resolveApprovalActionNodeDefaults,
  } from '@/config/process';

  defineOptions({
    name: 'ApprovalFlowView',
  });

  const { t } = useI18n();

  function createApprovalActionNode(approvalType: ApprovalType = 'manual') {
    const defaults = resolveApprovalActionNodeDefaults(approvalType);
    return createActionNode({
      name: defaults.name,
      description: defaults.description,
      actionType: 'approval',
      config: {
        approvalType,
      },
    });
  }

  const basicConfig = defineModel<BasicFormParams>('basicConfig', {
    default: () => ({
      ...defaultBasicForm,
    }),
  });
  const basicFormRef = ref<InstanceType<typeof basicForm> | null>(null);

  function validate(cb?: () => void) {
    basicFormRef.value?.validate(cb);
  }

  function isRightContentVisible(selection: NodeSelectionState) {
    if (selection.type !== 'node') return false;
    return ['start', 'action'].includes(selection.node.type);
  }

  function getStartNodeDescription() {
    const businessTypeLabel =
      businessTypeOptions.find((item) => item.value === basicConfig.value.formType)?.label ?? '';
    const executionTimingLabel = basicConfig.value.executeTiming
      .map((value: string) => executionTimingList.find((item) => item.value === value)?.label ?? '')
      .filter(Boolean)
      .join('/');

    return executionTimingLabel ? `${businessTypeLabel}(${executionTimingLabel})` : businessTypeLabel;
  }

  function createDefaultFlow(): FlowSchema {
    return {
      nodes: [createStartNode({ description: getStartNodeDescription() }), createApprovalActionNode(), createEndNode()],
    };
  }

  const flowSchema = ref<FlowSchema>(createDefaultFlow());

  // 初始化时自动选中开始节点
  const crmFlowRef = ref<InstanceType<typeof CrmFlow>>();
  function selectStartNodeOnInit() {
    const startNode = flowSchema.value.nodes.find((node) => node.type === 'start');
    if (!startNode) {
      return;
    }

    crmFlowRef.value?.selectNode(startNode.id);
  }

  onMounted(() => {
    nextTick(() => {
      selectStartNodeOnInit();
    });
  });

  interface AddNodeOption {
    label: string;
    type: 'action' | 'condition-group';
    icon: string;
    iconBgClass: string;
    actionApprovalType?: ApprovalType;
  }

  const addNodeGroups = computed<Array<{ key: string; title: string; options: AddNodeOption[] }>>(() => [
    {
      key: 'approval',
      title: t('process.process.flow.approver'),
      options: [
        {
          label: t('process.process.flow.manualApproval'),
          type: 'action',
          actionApprovalType: 'manual',
          icon: 'iconicon_contract',
          iconBgClass: 'bg-[var(--warning-yellow)]',
        },
        {
          label: t('process.process.flow.autoApprove'),
          type: 'action',
          actionApprovalType: 'auto-approve',
          icon: 'iconicon_contract',
          iconBgClass: 'bg-[var(--warning-yellow)]',
        },
        {
          label: t('process.process.flow.autoReject'),
          type: 'action',
          actionApprovalType: 'auto-reject',
          icon: 'iconicon_contract',
          iconBgClass: 'bg-[var(--warning-yellow)]',
        },
      ],
    },
    {
      key: 'condition',
      title: t('crmFlow.triggerCondition'),
      options: [
        {
          label: t('process.process.flow.conditionRule'),
          type: 'condition-group',
          icon: 'iconicon_fork',
          iconBgClass: 'bg-[var(--info-blue)]',
        },
      ],
    },
  ]);

  // 新建条件分支时，分支末尾默认挂一个审批节点
  function createApprovalConditionBranch(partial: Partial<ConditionBranch> = {}): ConditionBranch {
    return createConditionBranch({
      ...partial,
      children: partial.children ?? [createApprovalActionNode()],
    });
  }

  // 新建条件组时，if 和 else 两个默认分支都要带审批节点
  function createApprovalConditionGroupNode() {
    return createConditionGroupNode({
      branches: [
        createApprovalConditionBranch(),
        createElseBranch({
          children: [createApprovalActionNode()],
        }),
      ],
    });
  }

  function bindElseBranchFallbackNode(
    groupNode: ReturnType<typeof createApprovalConditionGroupNode>,
    fallbackNode?: FlowNode
  ) {
    if (fallbackNode?.type !== 'action') {
      return;
    }

    const elseBranch = groupNode.branches.find((branch) => branch.isElse);
    if (!elseBranch) {
      return;
    }

    // 条件组插入到“审批节点”前时，把原审批节点迁移到 else 分支，避免主链出现重复审批
    elseBranch.children = [fallbackNode];
  }

  // 主链插入条件组
  function insertApprovalConditionGroupAfterNode(anchorNodeId: string) {
    const location = findNodeLocation(flowSchema.value.nodes, anchorNodeId);
    if (!location) {
      return;
    }

    // 当前锚点后面的节点，是“加条件组”时要兜底迁移的候选节点。
    const nextNode = location.container[location.index + 1];
    const conditionGroupNode = createApprovalConditionGroupNode();
    bindElseBranchFallbackNode(conditionGroupNode, nextNode);

    if (nextNode?.type === 'action') {
      // 用条件组替换主链中的原审批节点。
      location.container.splice(location.index + 1, 1, conditionGroupNode);
      return;
    }

    insertNodeAfterNode(flowSchema.value, anchorNodeId, conditionGroupNode);
  }

  // 分支内插入条件组
  function insertApprovalConditionGroupToBranch(groupId: string, branchId: string) {
    const location = findBranchLocation(flowSchema.value.nodes, branchId);
    if (!location || location.group.id !== groupId) {
      return;
    }

    // 分支内新增节点默认插在头部，所以这里检查头节点是否需要迁移到 else
    const firstNode = location.branch.children[0];
    const conditionGroupNode = createApprovalConditionGroupNode();
    bindElseBranchFallbackNode(conditionGroupNode, firstNode);

    if (firstNode?.type === 'action') {
      // 分支头部为审批节点时，替换为条件组，并把审批迁移到 else
      location.branch.children.splice(0, 1, conditionGroupNode);
      return;
    }

    location.branch.children.unshift(conditionGroupNode);
  }

  function handleInsertFromAnchor(payload: {
    type: 'action' | 'condition-group';
    anchorNodeId: string | null;
    anchorBranch: { groupId: string; branchId: string } | null;
    actionApprovalType?: ApprovalType;
  }) {
    if (payload.type === 'condition-group') {
      if (payload.anchorBranch) {
        // 分支内插入条件组时，处理分支头审批节点迁移
        insertApprovalConditionGroupToBranch(payload.anchorBranch.groupId, payload.anchorBranch.branchId);
        return;
      }

      if (!payload.anchorNodeId) {
        return;
      }

      // 主链插入条件组时，处理分支头审批节点迁移
      insertApprovalConditionGroupAfterNode(payload.anchorNodeId);
      return;
    }

    const actionNode = createApprovalActionNode(payload.actionApprovalType ?? 'manual');
    if (payload.anchorBranch) {
      insertNodeToConditionBranch(
        flowSchema.value,
        payload.anchorBranch.groupId,
        payload.anchorBranch.branchId,
        actionNode
      );
      return;
    }

    if (!payload.anchorNodeId) {
      return;
    }
    insertNodeAfterNode(flowSchema.value, payload.anchorNodeId, actionNode);
  }

  function insertFromPopover(
    type: 'action' | 'condition-group',
    anchorNodeId: string | null,
    anchorBranch: { groupId: string; branchId: string } | null,
    actionApprovalType?: ApprovalType
  ) {
    if (!anchorBranch && !anchorNodeId) {
      return;
    }

    handleInsertFromAnchor({ type, anchorNodeId, anchorBranch, actionApprovalType });
  }

  // 新增if条件分支
  function handleAddConditionBranch(groupId: string) {
    addConditionBranch(flowSchema.value, groupId, createApprovalConditionBranch());
  }

  const startNodeDescription = computed(() => getStartNodeDescription());

  // 更新开始节点描述
  watch(
    startNodeDescription,
    (description) => {
      const firstNode = flowSchema.value.nodes[0];
      const startNode =
        firstNode?.type === 'start' ? firstNode : flowSchema.value.nodes.find((node) => node.type === 'start');
      if (!startNode) {
        return;
      }
      startNode.description = description;
    },
    {
      immediate: true,
    }
  );

  defineExpose({
    validate,
  });
</script>

<style scoped lang="less">
  :deep(.process-setting-form) {
    .n-form-item-label {
      font-weight: 600;
      color: var(--text-n1);
    }
  }
</style>
