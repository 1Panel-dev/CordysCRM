<template>
  <!-- TODO lmy edit-permission -->
  <BaseFlowNode
    :name="nodeData.name ?? ''"
    :description="nodeData.description"
    :show-content="nodeData.showContent ?? true"
    :selected="Boolean(nodeData.selected)"
    node-type="action"
    :icon="iconConfig"
    :title-editable="Boolean(!nodeData.isPanMode)"
    :edit-permission="['']"
    deletable
    @delete="handleDelete"
    @title-edit="handleTitleEdit"
  />
</template>

<script setup lang="ts">
  import { computed, toRef } from 'vue';

  import BaseFlowNode from './baseFlowNode.vue';

  import useX6NodeData from '../../composables/useX6NodeData';
  import { renameFlowByGraphData } from '../../graph/renameRegistry';
  import type { Node } from '@antv/x6';

  defineOptions({
    name: 'ActionNode',
  });

  interface ActionIconConfig {
    type: string;
    backgroundColor: string;
  }

  const ACTION_TYPE_ICON_MAP: Record<string, ActionIconConfig> = {
    approval: {
      type: 'iconicon_contract',
      backgroundColor: 'var(--warning-yellow)',
    },
  };

  const props = defineProps<{
    node?: Node;
  }>();

  const emit = defineEmits<{
    (event: 'delete'): void;
  }>();

  const { nodeData } = useX6NodeData<{
    kind: 'action';
    nodeId?: string;
    name?: string;
    description?: string;
    actionType?: string;
    showContent?: boolean;
    selected?: boolean;
    isPanMode?: boolean;
  }>(toRef(props, 'node'));

  const iconConfig = computed<ActionIconConfig>(() => {
    const { actionType } = nodeData.value;
    return ACTION_TYPE_ICON_MAP[actionType ?? 'approval'];
  });

  function handleDelete() {
    emit('delete');
  }

  function handleTitleEdit(value: string, done?: () => void) {
    const data = nodeData.value;
    renameFlowByGraphData(data, value);
    props.node?.setData?.({
      ...data,
      name: value,
    });
    done?.();
  }
</script>
