<template>
  <BaseFlowNode
    :name="nodeData.name ?? ''"
    :description="nodeData.description"
    :show-content="nodeData.showContent ?? true"
    :selected="Boolean(nodeData.selected)"
    node-type="action"
    :icon="iconConfig"
    deletable
    @delete="handleDelete"
  />
</template>

<script setup lang="ts">
  import { computed, toRef } from 'vue';

  import BaseFlowNode from './baseFlowNode.vue';

  import useX6NodeData from '../../composables/useX6NodeData';
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
    name?: string;
    description?: string;
    actionType?: string;
    showContent?: boolean;
    selected?: boolean;
  }>(toRef(props, 'node'));

  const iconConfig = computed<ActionIconConfig>(() => {
    const { actionType } = nodeData.value;
    return ACTION_TYPE_ICON_MAP[actionType ?? 'approval'];
  });

  function handleDelete() {
    emit('delete');
  }
</script>
