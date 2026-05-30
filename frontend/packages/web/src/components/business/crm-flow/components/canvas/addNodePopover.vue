<template>
  <div
    v-if="show && position"
    class="add-node-popover"
    :style="{ left: `${position.x}px`, top: `${position.y}px` }"
    @mouseenter="emit('mouseenter')"
    @mouseleave="emit('mouseleave')"
  >
    <slot name="content"> </slot>
  </div>
</template>

<script setup lang="ts">
  defineOptions({
    name: 'AddNodePopover',
  });

  defineProps<{
    show: boolean;
    position: { x: number; y: number } | null;
  }>();

  const emit = defineEmits<{
    (event: 'mouseenter'): void;
    (event: 'mouseleave'): void;
  }>();
</script>

<style scoped lang="less">
  .add-node-popover {
    position: absolute;
    z-index: 1000;
    transform: translate(-50%, 8px);
  }
</style>
