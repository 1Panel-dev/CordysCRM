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
  import { toRef } from 'vue';

  import BaseFlowNode from './baseFlowNode.vue';

  import useX6NodeData from '../../composables/useX6NodeData';
  import type { Node } from '@antv/x6';

  defineOptions({
    name: 'EndNode',
  });

  const props = defineProps<{
    node?: Node;
  }>();

  const { nodeData } = useX6NodeData<{ name?: string; selected?: boolean }>(toRef(props, 'node'));
</script>
