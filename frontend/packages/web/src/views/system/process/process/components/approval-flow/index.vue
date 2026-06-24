<template>
  <div class="h-full w-full">
    <CrmTab
      v-if="flowTimingTabs.length"
      v-model:active-tab="activeFlowTiming"
      class="flow-timing-tabs absolute left-[16px] top-[16px] z-[10] w-fit"
      type="segment"
      no-content
      :tab-list="flowTimingTabs"
      @change="handleFlowTimingChange"
    />
    <CrmFlow
      v-if="flowTimingTabs.length"
      ref="crmFlowRef"
      v-model:model="flowSchema"
      :canvas-flow="canvasFlowSchema"
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
      :sort="activeConditionBranchSort"
      :priority-options="conditionPriorityOptions"
      :readonly="props.readonly"
      @confirm="handleConditionConfirm"
    />
  </div>
</template>

<script setup lang="ts">
  import { computed, nextTick, ref, watch } from 'vue';

  import { ApprovalTypeEnum } from '@lib/shared/enums/process';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type {
    ApprovalActionNode,
    ApprovalConditionBranch,
    ApprovalFlowNodeConfig,
  } from '@lib/shared/models/system/process';
  import { BasicFormParams } from '@lib/shared/models/system/process';

  import type { FilterForm } from '@/components/pure/crm-advance-filter/type';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmTab from '@/components/pure/crm-tab/index.vue';
  import { findBranchLocation } from '@/components/business/crm-flow/dsl/queries';
  import type { BranchClickPayload } from '@/components/business/crm-flow/graph/types';
  import CrmFlow from '@/components/business/crm-flow/index.vue';
  import type { FlowNode, FlowSchema, NodeSelectionState } from '@/components/business/crm-flow/types';
  import approvalActionNodeForm from './approval-node/index.vue';
  import setConditionDrawer from './setConditionDrawer.vue';

  import { approvalFlowAddNodeGroups, businessTypeOptions, defaultBasicForm } from '@/config/process';

  import {
    addApprovalConditionBranch,
    createDefaultFlow,
    insertFromAnchor,
    resolveApprovalActionNodeDescriptionItems,
  } from './flow';
  import { resolveConditionDescription } from './flow/conditionDescription';
  import { deserializeProcessNodes, serializeFlowNodes } from './flow/transform';
  import useConditionFilterConfig from './flow/useConditionFilterConfig';
  import { validateFlowNodes as validateFlowSchemaNodes } from './flow/validation';

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

  type ApprovalFlowTiming = 'create' | 'update' | 'delete';

  const flowTimingConfig: Array<{
    value: ApprovalFlowTiming;
    executeKey: 'createExecute' | 'updateExecute' | 'deleteExecute';
    configKey: 'createNodeConfig' | 'updateNodeConfig' | 'deleteNodeConfig';
    label: string;
  }> = [
    {
      value: 'create',
      executeKey: 'createExecute',
      configKey: 'createNodeConfig',
      label: t('common.create'),
    },
    {
      value: 'update',
      executeKey: 'updateExecute',
      configKey: 'updateNodeConfig',
      label: t('common.edit'),
    },
    {
      value: 'delete',
      executeKey: 'deleteExecute',
      configKey: 'deleteNodeConfig',
      label: t('common.delete'),
    },
  ];

  const activeFlowTiming = ref<ApprovalFlowTiming>('create');
  const flowTimingTabs = computed(() =>
    flowTimingConfig
      .filter((item) => basicConfig.value[item.executeKey])
      .map((item) => ({
        name: item.value,
        tab: item.label,
      }))
  );

  function isRightContentVisible(selection: NodeSelectionState) {
    if (selection.type !== 'node') return false;
    return selection.node.type === 'action';
  }

  function isApprovalActionNode(node: FlowNode): node is ApprovalActionNode {
    return node.type === 'action' && node.actionType === 'approval';
  }

  function resolveOptionLabel(value: string, options: Array<{ value: string; label: string }>): string {
    return options.find((item) => item.value === value)?.label ?? '';
  }

  function resolveStartNodeDescription(timing: ApprovalFlowTiming) {
    const businessTypeLabel = resolveOptionLabel(basicConfig.value.formType, businessTypeOptions);
    const executionTimingLabel = flowTimingConfig.find((item) => item.value === timing)?.label ?? '';

    return executionTimingLabel ? `${businessTypeLabel}(${executionTimingLabel})` : businessTypeLabel;
  }

  // 流程图
  const flowSchemas = ref<Record<ApprovalFlowTiming, FlowSchema>>({
    create: createDefaultFlow(resolveStartNodeDescription('create')),
    update: createDefaultFlow(resolveStartNodeDescription('update')),
    delete: createDefaultFlow(resolveStartNodeDescription('delete')),
  });
  const flowSchema = ref<FlowSchema>(flowSchemas.value.create);

  // descriptionItems 只是审批人卡片的展示字段，不能直接写回正在编辑的 flowSchema。
  // 这里派生一份画布专用数据，避免右侧表单更新审批人时触发流程图深层重绘导致画布消失。
  function createCanvasFlowNodes(nodes: FlowNode[]): FlowNode[] {
    return nodes.map((node) => {
      if (node.type === 'condition-group') {
        return {
          ...node,
          branches: node.branches.map((branch) => ({
            ...branch,
            children: createCanvasFlowNodes(branch.children),
          })),
        };
      }

      if (isApprovalActionNode(node)) {
        return {
          ...node,
          descriptionItems: resolveApprovalActionNodeDescriptionItems(node),
        };
      }

      return { ...node };
    });
  }

  const canvasFlowSchema = computed<FlowSchema>(() => ({
    nodes: createCanvasFlowNodes(flowSchema.value.nodes),
  }));

  const { descriptionContext, loadFilterConfig: loadConditionFilterConfig } = useConditionFilterConfig({
    formType: () => basicConfig.value.formType,
    optionMap: () => props.optionMap,
  });

  // 递归找所有 if 条件分支，然后重新生成卡片描述
  function updateConditionDescription(nodes: FlowNode[]) {
    nodes.forEach((node) => {
      if (node.type !== 'condition-group') {
        return;
      }

      node.branches.forEach((branch) => {
        if (!branch.isElse) {
          (branch as ApprovalConditionBranch).description = resolveConditionDescription(
            (branch as ApprovalConditionBranch).conditionConfig,
            descriptionContext.value
          );
        }
        updateConditionDescription(branch.children);
      });
    });
  }

  function refreshConditionDescriptions() {
    flowTimingConfig.forEach((timing) => {
      updateConditionDescription(flowSchemas.value[timing.value].nodes);
    });
  }

  function getTimingProcessData() {
    return flowTimingConfig.reduce((data, config) => {
      const flowData = serializeFlowNodes(flowSchemas.value[config.value].nodes);
      data[config.configKey] = flowData;
      return data;
    }, {} as Record<string, ApprovalFlowNodeConfig>);
  }

  const crmFlowRef = ref<InstanceType<typeof CrmFlow>>();
  function refreshCanvas(fitToContent = false) {
    crmFlowRef.value?.refreshCanvas(fitToContent);
  }

  function setProcessData(data: {
    createNodeConfig?: ApprovalFlowNodeConfig;
    updateNodeConfig?: ApprovalFlowNodeConfig;
    deleteNodeConfig?: ApprovalFlowNodeConfig;
  }) {
    flowTimingConfig.forEach((config) => {
      const nodeConfig = data[config.configKey];
      const nodes = nodeConfig?.nodes ?? [];
      const links = nodeConfig?.links ?? [];
      flowSchemas.value[config.value] = nodes.length
        ? deserializeProcessNodes(nodes, links, resolveStartNodeDescription(config.value))
        : createDefaultFlow(resolveStartNodeDescription(config.value));
    });
    refreshConditionDescriptions();
    flowSchema.value = flowSchemas.value[activeFlowTiming.value];
    nextTick(() => {
      refreshCanvas(true);
    });
  }

  // 触发条件抽屉
  const setConditionDrawerVisible = ref(false);
  const activeConditionBranch = ref<ApprovalConditionBranch | null>(null);

  const activeIfConditionBranches = computed(() => {
    if (!activeConditionBranch.value) {
      return [];
    }

    return (
      findBranchLocation<ApprovalConditionBranch>(
        flowSchema.value.nodes,
        activeConditionBranch.value.id
      )?.group.branches.filter((branch) => !branch.isElse) ?? []
    );
  });

  const activeConditionBranchSort = computed(() => {
    if (!activeConditionBranch.value) {
      return 1;
    }

    const index = activeIfConditionBranches.value.findIndex((branch) => branch.id === activeConditionBranch.value?.id);
    return index >= 0 ? index + 1 : 1;
  });

  function handleFlowTimingChange(value: string | number) {
    setConditionDrawerVisible.value = false;
    activeConditionBranch.value = null;
    activeFlowTiming.value = value as ApprovalFlowTiming;
    flowSchema.value = flowSchemas.value[activeFlowTiming.value];
  }

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

  const conditionPriorityOptions = computed(() => {
    if (!activeConditionBranch.value) {
      return [];
    }

    const ifBranchCount = activeIfConditionBranches.value.length;
    return Array.from({ length: ifBranchCount }, (_, index) => ({
      label: `P${index + 1}`,
      value: index + 1,
    }));
  });

  function openConditionDrawer(branch: ApprovalConditionBranch) {
    activeConditionBranch.value = branch;
    setConditionDrawerVisible.value = true;
  }

  watch(setConditionDrawerVisible, (visible) => {
    if (visible) {
      return;
    }
    nextTick(() => {
      refreshCanvas();
    });
  });

  function handleBranchClick(payload: BranchClickPayload) {
    const location = findBranchLocation<ApprovalConditionBranch>(flowSchema.value.nodes, payload.branchId);
    if (!location || location.group.id !== payload.groupId || location.branch.isElse) {
      return;
    }

    openConditionDrawer(location.branch);
  }

  function updateConditionBranchSort(branch: ApprovalConditionBranch, targetSort: number) {
    const location = findBranchLocation<ApprovalConditionBranch>(flowSchema.value.nodes, branch.id);
    if (!location) {
      return;
    }

    const ifBranches = location.group.branches.filter((item) => !item.isElse);
    const currentIndex = ifBranches.findIndex((item) => item.id === branch.id);
    const targetIndex = targetSort - 1;
    if (currentIndex < 0 || targetIndex < 0 || targetIndex >= ifBranches.length || currentIndex === targetIndex) {
      return;
    }

    [ifBranches[currentIndex], ifBranches[targetIndex]] = [ifBranches[targetIndex], ifBranches[currentIndex]];
    const elseBranches = location.group.branches.filter((item) => item.isElse);
    location.group.branches = [...ifBranches, ...elseBranches];
  }

  function handleConditionConfirm(payload: { name: string; sort: number; conditionConfig: FilterForm }) {
    if (props.readonly || !activeConditionBranch.value) {
      return;
    }

    activeConditionBranch.value.name = payload.name;
    activeConditionBranch.value.conditionConfig = payload.conditionConfig;
    activeConditionBranch.value.description = resolveConditionDescription(
      payload.conditionConfig,
      descriptionContext.value
    );
    activeConditionBranch.value.invalid = false;
    updateConditionBranchSort(activeConditionBranch.value, payload.sort);
  }

  function validateFlowNodes() {
    const enabledTimings = flowTimingConfig.filter((item) => basicConfig.value[item.executeKey]);
    return enabledTimings.every((timing) => {
      const valid = validateFlowSchemaNodes(flowSchemas.value[timing.value]);
      if (!valid) {
        handleFlowTimingChange(timing.value);
      }
      return valid;
    });
  }

  function updateStartNodeDescriptions() {
    flowTimingConfig.forEach((timing) => {
      const firstNode = flowSchemas.value[timing.value].nodes[0];
      const startNode =
        firstNode?.type === 'start'
          ? firstNode
          : flowSchemas.value[timing.value].nodes.find((node) => node.type === 'start');
      if (startNode) {
        startNode.description = resolveStartNodeDescription(timing.value);
      }
    });
  }

  watch(
    flowTimingTabs,
    (tabs) => {
      if (!tabs.length || tabs.some((item) => item.name === activeFlowTiming.value)) {
        return;
      }

      handleFlowTimingChange(tabs[0].name as ApprovalFlowTiming);
    },
    {
      immediate: true,
    }
  );

  function resetToDefaultFlow() {
    setConditionDrawerVisible.value = false;
    activeConditionBranch.value = null;
    flowTimingConfig.forEach((timing) => {
      flowSchemas.value[timing.value] = createDefaultFlow(resolveStartNodeDescription(timing.value));
    });
    flowSchema.value = flowSchemas.value[activeFlowTiming.value];
  }

  watch(
    () => props.optionMap,
    () => {
      refreshConditionDescriptions();
    },
    {
      deep: true,
    }
  );

  watch(
    () => basicConfig.value.formType,
    async (formType, oldFormType) => {
      updateStartNodeDescriptions();

      if (!props.readonly && oldFormType && formType !== oldFormType) {
        resetToDefaultFlow();
      }

      await loadConditionFilterConfig();
      refreshConditionDescriptions();
    },
    {
      immediate: true,
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
    getTimingProcessData,
    setProcessData,
    refreshCanvas,
  });
</script>

<style lang="less" scoped>
  .flow-timing-tabs {
    :deep(.n-tabs-tab.n-tabs-tab--active) {
      color: var(--primary-8) !important;
    }
  }
</style>
