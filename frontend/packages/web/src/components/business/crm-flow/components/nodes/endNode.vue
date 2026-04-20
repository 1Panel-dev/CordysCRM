<template>
  <BaseFlowNode
    :name="nodeData.name ?? ''"
    :selected="Boolean(nodeData.selected)"
    node-type="end"
    :icon="{
      type: 'iconicon_end',
      backgroundColor: 'var(--text-n6)',
    }"
  />
</template>

<script setup lang="ts">
  import { computed } from 'vue';

  import BaseFlowNode from './baseFlowNode.vue';

  import type { Node } from '@antv/x6';

  defineOptions({
    name: 'EndNode',
  });

  const props = defineProps<{
    node?: Node;
  }>();

  const nodeData = computed(() => (props.node?.getData?.() ?? {}) as { name?: string; selected?: boolean });
</script>
