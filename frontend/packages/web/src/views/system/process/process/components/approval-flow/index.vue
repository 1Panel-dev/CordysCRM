<template>
  <div class="h-full w-full">
    <CrmFlow
      ref="crmFlowRef"
      v-model:model="flowSchema"
      :readonly="props.readonly"
      :right-content-visible="isRightContentVisible"
      @add-condition-branch="handleAddConditionBranch"
      @branch-click="handleBranchClick"
    >
      <template #insertNodeContent="{ anchorNodeId, anchorBranch }">
        <div v-if="!props.readonly" class="base-box-shadow min-w-[366px] rounded-[6px] bg-[var(--text-n10)] p-[16px]">
          <div v-for="group in approvalFlowAddNodeGroups" :key="group.key" class="mb-[16px] last:mb-0">
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
          v-model:basicConfig="basicConfig"
          :need-detail="props.needDetail"
          :readonly="props.readonly"
        />
        <approvalActionNodeForm
          v-if="selection.type === 'node' && isApprovalActionNode(selection.node)"
          v-model:node="selection.node"
          :form-type="basicConfig.formType"
          :option-map="props.optionMap"
          :readonly="props.readonly"
          @switch-more-setting="emit('switchMoreSetting')"
        />
      </template>
    </CrmFlow>
    <setConditionDrawer
      v-model:show="setConditionDrawerVisible"
      :branch="activeConditionBranch"
      :form-type="basicConfig.formType"
      :option-map="props.optionMap"
      :readonly="props.readonly"
      @confirm="handleConditionConfirm"
    />
  </div>
</template>

<script setup lang="ts">
  import { computed, nextTick, onMounted, ref, watch } from 'vue';

  import { ApprovalTypeEnum } from '@lib/shared/enums/process';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type {
    ApprovalActionNode,
    ApprovalConditionBranch,
    ApprovalNodeLinkResponse,
    ApprovalProcessNode,
  } from '@lib/shared/models/system/process';
  import { BasicFormParams } from '@lib/shared/models/system/process';

  import type { FilterForm } from '@/components/pure/crm-advance-filter/type';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import { findBranchLocation } from '@/components/business/crm-flow/dsl/queries';
  import type { BranchClickPayload } from '@/components/business/crm-flow/graph/types';
  import CrmFlow from '@/components/business/crm-flow/index.vue';
  import type { FlowNode, FlowSchema, NodeSelectionState } from '@/components/business/crm-flow/types';
  import approvalActionNodeForm from './approval-node/index.vue';
  import basicForm from './basicForm.vue';
  import setConditionDrawer from './setConditionDrawer.vue';

  import {
    approvalFlowAddNodeGroups,
    businessTypeOptions,
    defaultBasicForm,
    executionTimingList,
  } from '@/config/process';

  import { addApprovalConditionBranch, createDefaultFlow, insertFromAnchor, resolveConditionDescription } from './flow';
  import { deserializeProcessNodes, serializeFlowNodes } from './flow/transform';
  import useFlowValidation from './flow/validation';

  defineOptions({
    name: 'ApprovalFlowView',
  });

  const props = defineProps<{
    needDetail?: boolean;
    readonly?: boolean;
    optionMap?: Record<string, any[]>;
  }>();

  const emit = defineEmits<{
    (event: 'switchMoreSetting'): void;
    (event: 'change'): void;
  }>();
  const { t } = useI18n();

  // 基础表单
  const basicConfig = defineModel<BasicFormParams>('basicConfig', {
    default: () => ({
      ...defaultBasicForm,
    }),
  });

  // 右侧面板仅展示开始节点与审批节点配置
  function isRightContentVisible(selection: NodeSelectionState) {
    if (selection.type !== 'node') return false;
    return ['start', 'action'].includes(selection.node.type);
  }

  function isApprovalActionNode(node: FlowNode): node is ApprovalActionNode {
    return node.type === 'action' && node.actionType === 'approval';
  }

  function resolveOptionLabel(value: string, options: Array<{ value: string; label: string }>): string {
    return options.find((item) => item.value === value)?.label ?? '';
  }

  const startNodeDescription = computed(() => {
    const businessTypeLabel = resolveOptionLabel(basicConfig.value.formType, businessTypeOptions);
    const executionTimingLabel = executionTimingList
      .filter((item) => basicConfig.value[item.value])
      .map((item) => item.label)
      .filter(Boolean)
      .join('/');

    return executionTimingLabel ? `${businessTypeLabel}(${executionTimingLabel})` : businessTypeLabel;
  });

  // 流程图
  const flowSchema = ref<FlowSchema>(createDefaultFlow(startNodeDescription.value));

  // 初始化时自动选中开始节点
  const crmFlowRef = ref<InstanceType<typeof CrmFlow>>();
  function selectStartNodeOnInit() {
    const startNode = flowSchema.value.nodes.find((node) => node.type === 'start');
    if (!startNode) {
      return;
    }

    crmFlowRef.value?.selectNode(startNode.id);
    nextTick(() => {
      crmFlowRef.value?.refreshCanvas(true);
    });
  }

  function getProcessNodes(): ApprovalProcessNode[] {
    return serializeFlowNodes(flowSchema.value.nodes).nodes;
  }

  function getProcessLinks(): ApprovalNodeLinkResponse[] {
    return serializeFlowNodes(flowSchema.value.nodes).links;
  }

  function setProcessData(nodes: ApprovalProcessNode[], links: ApprovalNodeLinkResponse[]) {
    flowSchema.value = deserializeProcessNodes(nodes, links, startNodeDescription.value);
    nextTick(() => {
      selectStartNodeOnInit();
    });
  }

  function refreshCanvas(fitToContent = false) {
    crmFlowRef.value?.refreshCanvas(fitToContent);
  }

  onMounted(() => {
    nextTick(() => {
      selectStartNodeOnInit();
    });
  });

  function insertFromPopover(
    type: 'action' | 'condition-group',
    anchorNodeId: string | null,
    anchorBranch: { groupId: string; branchId: string } | null,
    actionApprovalType?: ApprovalTypeEnum
  ) {
    if (!anchorBranch && !anchorNodeId) {
      return;
    }

    insertFromAnchor({
      flowSchema: flowSchema.value,
      type,
      anchorNodeId,
      anchorBranch,
      actionApprovalType,
    });
  }

  // 新增if条件分支
  function handleAddConditionBranch(groupId: string) {
    if (props.readonly) {
      return;
    }
    addApprovalConditionBranch(flowSchema.value, groupId);
  }

  // 触发条件抽屉
  const setConditionDrawerVisible = ref(false);
  const activeConditionBranch = ref<ApprovalConditionBranch | null>(null);

  function openConditionDrawer(branch: ApprovalConditionBranch) {
    activeConditionBranch.value = branch;
    setConditionDrawerVisible.value = true;
  }

  function handleBranchClick(payload: BranchClickPayload) {
    const location = findBranchLocation<ApprovalConditionBranch>(flowSchema.value.nodes, payload.branchId);
    if (!location || location.group.id !== payload.groupId || location.branch.isElse) {
      return;
    }

    openConditionDrawer(location.branch);
  }

  function handleConditionConfirm(payload: { name: string; conditionConfig: FilterForm }) {
    if (props.readonly || !activeConditionBranch.value) {
      return;
    }

    activeConditionBranch.value.name = payload.name;
    activeConditionBranch.value.conditionConfig = payload.conditionConfig;
    activeConditionBranch.value.description = resolveConditionDescription(payload.conditionConfig);
    activeConditionBranch.value.invalid = false;
  }

  const { validateFlowNodes } = useFlowValidation({
    flowSchema,
    basicConfig,
  });

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

  function resetToDefaultFlow() {
    setConditionDrawerVisible.value = false;
    activeConditionBranch.value = null;
    flowSchema.value = createDefaultFlow(startNodeDescription.value);

    nextTick(() => {
      selectStartNodeOnInit();
    });
  }

  watch(
    () => basicConfig.value.formType,
    (formType, oldFormType) => {
      if (props.readonly || !oldFormType || formType === oldFormType) {
        return;
      }

      resetToDefaultFlow();
    }
  );

  watch(
    flowSchema,
    () => {
      if (!props.readonly) {
        emit('change');
      }
    },
    {
      deep: true,
    }
  );

  defineExpose({
    validateFlowNodes,
    getProcessNodes,
    getProcessLinks,
    setProcessData,
    refreshCanvas,
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
