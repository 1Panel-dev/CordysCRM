<template>
  <BaseFlowNode
    :name="nodeData.name ?? ''"
    :description="nodeData.description"
    :show-content="nodeData.showContent ?? true"
    :selected="Boolean(nodeData.selected)"
    node-type="start"
    :icon="{
      type: 'iconicon_play_circle_stroke',
      backgroundColor: 'var(--primary-8)',
    }"
  />
</template>

<script setup lang="ts">
  import { computed } from 'vue';

  import BaseFlowNode from './baseFlowNode.vue';

  import type { Node } from '@antv/x6';

  defineOptions({
    name: 'StartNode',
  });

  const props = defineProps<{
    node?: Node;
  }>();

  const nodeData = computed(
    () =>
      (props.node?.getData?.() ?? {}) as {
        name?: string;
        description?: string;
        showContent?: boolean;
        selected?: boolean;
      }
  );
</script>
