<template>
  <div class="h-full w-full">
    <CrmFlow v-model:model="flowSchema">
      <template #insertNodeContent="{ anchorNodeId, anchorBranch }">
        <div class="base-box-shadow min-w-[366px] rounded-[6px] bg-[var(--text-n10)] p-[16px]">
          <div v-for="group in addNodeGroups" :key="group.key" class="mb-[16px] last:mb-0">
            <div class="mb-[8px] font-semibold">{{ group.title }}</div>
            <div class="flex gap-[8px]">
              <div
                v-for="item in group.options"
                :key="item.label"
                class="inline-flex h-[38px] items-center gap-[8px] rounded-[var(--border-radius-small)] border border-transparent bg-[var(--text-n9)] px-[12px] transition-all hover:border-[var(--primary-1)]"
                @click="insertFromPopover(item.type, anchorNodeId, anchorBranch)"
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
        <div> {{ selection }} </div>
      </template>
    </CrmFlow>
  </div>
</template>

<script setup lang="ts">
  import { ref } from 'vue';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import { insertNodeAfterNode, insertNodeToConditionBranch } from '@/components/business/crm-flow/dsl/actions';
  import {
    createActionNode,
    createConditionGroupNode,
    createEndNode,
    createFlowId,
    createStartNode,
  } from '@/components/business/crm-flow/dsl/factory';
  import CrmFlow from '@/components/business/crm-flow/index.vue';
  import type { FlowSchema } from '@/components/business/crm-flow/types';

  defineOptions({
    name: 'ApprovalFlowView',
  });

  function createDefaultFlow(): FlowSchema {
    return {
      id: createFlowId('flow'),
      name: '新建流程',
      nodes: [
        createStartNode({ description: '报价' }),
        createActionNode({ name: '审批动作', description: '选择审批人' }),
        createEndNode(),
      ],
    };
  }

  const flowSchema = ref<FlowSchema>(createDefaultFlow());

  interface AddNodeOption {
    label: string;
    type: 'action' | 'condition-group';
    icon: string;
    iconBgClass: string;
  }

  const addNodeGroups: Array<{ key: string; title: string; options: AddNodeOption[] }> = [
    {
      key: 'approval',
      title: '审批人',
      options: [
        {
          label: '人工审批',
          type: 'action',
          icon: 'iconicon_contract',
          iconBgClass: 'bg-[var(--warning-yellow)]',
        },
        {
          label: '自动通过',
          type: 'action',
          icon: 'iconicon_contract',
          iconBgClass: 'bg-[var(--warning-yellow)]',
        },
        {
          label: '自动拒绝',
          type: 'action',
          icon: 'iconicon_contract',
          iconBgClass: 'bg-[var(--warning-yellow)]',
        },
      ],
    },
    {
      key: 'condition',
      title: '触发条件',
      options: [
        {
          label: '条件规则',
          type: 'condition-group',
          icon: 'iconicon_fork',
          iconBgClass: 'bg-[var(--info-blue)]',
        },
      ],
    },
  ];

  function createApprovalActionNode() {
    return createActionNode({
      name: '审批动作',
      description: '选择审批人',
      actionType: 'approval',
    });
  }

  function handleInsertNode(payload: { anchorNodeId: string; type: 'action' | 'condition-group' }) {
    const node = payload.type === 'action' ? createApprovalActionNode() : createConditionGroupNode();
    insertNodeAfterNode(flowSchema.value, payload.anchorNodeId, node);
  }

  function handleInsertBranchNode(payload: { groupId: string; branchId: string; type: 'action' | 'condition-group' }) {
    const node = payload.type === 'action' ? createApprovalActionNode() : createConditionGroupNode();
    insertNodeToConditionBranch(flowSchema.value, payload.groupId, payload.branchId, node);
  }

  function insertFromPopover(
    type: 'action' | 'condition-group',
    anchorNodeId: string | null,
    anchorBranch: { groupId: string; branchId: string } | null
  ) {
    if (anchorBranch) {
      handleInsertBranchNode({
        groupId: anchorBranch.groupId,
        branchId: anchorBranch.branchId,
        type,
      });
      return;
    }

    if (!anchorNodeId) {
      return;
    }

    handleInsertNode({
      anchorNodeId,
      type,
    });
  }
</script>
